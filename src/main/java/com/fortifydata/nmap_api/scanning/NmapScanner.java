package com.fortifydata.nmap_api.scanning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortifydata.nmap_api.model.HostRequest;
import com.fortifydata.nmap_api.model.NmapScanType;
import com.fortifydata.nmap_api.model.ScannedHost;
import com.fortifydata.nmap_api.queue.HostRequestQueue;
import com.fortifydata.nmap_api.queue.ScannedHostQueue;
import com.google.common.base.Objects;
import org.nmap4j.Nmap4j;
import org.nmap4j.core.nmap.NMapExecutionException;
import org.nmap4j.core.nmap.NMapInitializationException;
import org.nmap4j.data.NMapRun;
import org.nmap4j.data.nmaprun.hostnames.Hostname;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.nmap4j.data.nmaprun.Host;

import javax.jms.JMSException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty("scanner.controller")
public class NmapScanner {

    private final Logger log = LoggerFactory.getLogger(NmapScanner.class);

    @Autowired
    private ScannedHostQueue scannedHostQueue;

    @Autowired
    private HostRequestQueue hostRequestQueue;

    @Autowired
    private ScanStatus status;

    @Value("${nmap.location}")
    private String nmapLocation;

    public boolean processHosts(List<HostRequest> requests) throws JMSException {
        try {
            NmapScanType scanType = Optional.ofNullable(requests.get(0).getScanType()).orElse(NmapScanType.STATUS);
            Map<String, HostRequest> hostMap = new HashMap<>();

            Nmap4j nmap4j = new Nmap4j( nmapLocation ) ;
            switch(scanType){
                case STATUS: nmap4j.addFlags( "-p80,443 -T5 -n -vv -oX -" );
                    break;
                case STATUS_FULL: nmap4j.addFlags( "-T5 -n -vv -Pn -oX -" );
                    break;
            }
            for(HostRequest request : requests){
                nmap4j.includeHosts(request.getHost());
                hostMap.put(request.getHost(),request);
            }
            status.setStatus(ScanStatus.STATUS.SCANNING, requests);
            nmap4j.execute() ;
            if( !nmap4j.hasError() ) {
                NMapRun nmapRun = nmap4j.getResult() ;
                String output = nmap4j.getOutput() ;
                if( output == null ) {
                    throw new NMapExecutionException();
                }
                List<Host> hosts = nmapRun.getHosts();
                if(scanType.equals(NmapScanType.STATUS_FULL)){
                    hosts = hosts.stream().map(host -> {
                        boolean isRunning = !host.getPorts().getPorts().isEmpty();
                        host.getStatus().setState(isRunning ? "up" : "down");
                        return host;
                    }).collect(Collectors.toList());
                }

                publishResults(hosts, hostMap);
                status.setStatus(ScanStatus.STATUS.COMPLETED, requests);
            }
        } catch (NMapInitializationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            status.setStatus(ScanStatus.STATUS.ERROR, requests);
            return false;
        } catch (NMapExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            status.setStatus(ScanStatus.STATUS.ERROR, requests);
            return false;
        }catch(Exception e){
            e.printStackTrace();
            status.setStatus(ScanStatus.STATUS.ERROR, requests);
            return false;
        }
        return true;
    }

    public void publishResults(List<Host> scanned, Map<String, HostRequest> hostMap) throws JsonProcessingException, JMSException {
        log.info("Preparing to send scanned host data to the queue");
        List<HostRequest> rescan = new ArrayList<>();

        //For each NMap scanned host returned
        List<ScannedHost> scannedHosts = scanned.stream().map(host -> {
            ScannedHost scannedHost = new ScannedHost();

            //FIRST, match the nmap host to the HostRequest object
            List<String> hostAddress = new ArrayList<>();
            if(host.getHostnames() != null && !host.getHostnames().isEmpty()){
                Optional<Hostname> userHostname = host.getHostnames().stream()
                        .filter(hostname -> Objects.equal(hostname.getType(),"user"))
                        .findFirst();
                if(userHostname.isPresent()){
                    hostAddress.add(userHostname.get().getName());
                }else{
                    host.getHostnames().forEach(hostname -> {
                        hostAddress.add(hostname.getName());
                    });
                }
            }else{
                host.getAddresses().forEach(address -> {
                    hostAddress.add(address.getAddr());
                });
            }
            var hostRequest = hostAddress.stream()
                    .map(address -> hostMap.get(address))
                    .filter(adr -> adr != null)
                    .findFirst().get();

            //Now, check if the hostRequest was a simples status scan, and if so, if nmap returned down.
            //If the scan was a simple STATUS scan, and the nmap host is down, rescan that host with a full port status scan
            if(Objects.equal(hostRequest.getScanType(),NmapScanType.STATUS) && Objects.equal(host.getStatus().getState(),"down")){
                hostRequest.setScanType(NmapScanType.STATUS_FULL);
                rescan.add(hostRequest);
                return null;
            }else{
                //Otherwise, publish the results
                scannedHost.setCompanyId(hostRequest.getCompanyId());
                scannedHost.setHost(host);
                scannedHost.setScanId(hostRequest.getScanId());
                return scannedHost;
            }
        }).filter(scannedHost -> scannedHost != null).collect(Collectors.toList());
        scannedHostQueue.sendAll(scannedHosts);
        if(!rescan.isEmpty()){
            hostRequestQueue.sendAll(rescan);
        }
    }
}
