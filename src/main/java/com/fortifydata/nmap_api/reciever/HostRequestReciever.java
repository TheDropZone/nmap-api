package com.fortifydata.nmap_api.reciever;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fortifydata.nmap_api.model.HostRequest;
import com.fortifydata.nmap_api.model.NmapScanType;
import com.fortifydata.nmap_api.queue.HostRequestQueue;
import com.fortifydata.nmap_api.scanning.NmapScanner;
import com.fortifydata.nmap_api.scanning.ScanStatus;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty("scanner.controller")
public class HostRequestReciever {

    private final Logger log = LoggerFactory.getLogger(HostRequestReciever.class);

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private HostRequestQueue hostRequestQueue;

    @Autowired
    private NmapScanner nmapScanner;

    @Autowired
    private ScanStatus status;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    private List<HostRequest> requestQueue = new ArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public void listen(){

        boolean processed = false;
        boolean wasIdle = true;
        Long start = System.currentTimeMillis();

        try{
            //Stay waiting/Idle until the scanner processes requests, and continue to process requests in a loop
            //until the scanner goes idle, and then shut down
            //OR, if we are idle and don't process anything for 5 minutes, shut down as well
            while(!(processed && wasIdle) && !((System.currentTimeMillis() - start) > 1000 * 60 * 5)){
                boolean runProcessed = listenAndProcess();
                processed = (!processed && runProcessed) ? true : processed;
                wasIdle = !runProcessed;
            }
        }catch(Exception e){
            e.printStackTrace();
            log.error("Crash during listen loop", e);
            status.setStatus(ScanStatus.STATUS.ERROR);
        }
        status.setStatus(ScanStatus.STATUS.SHUT_DOWN, requestQueue);
        int exitCode = SpringApplication.exit(appContext, () -> 0);
        System.exit(exitCode);
    }

    public boolean listenAndProcess() throws IOException, JMSException {
        status.setStatus(ScanStatus.STATUS.IDLE);
        AtomicBoolean processedRequests = new AtomicBoolean(false);

        hostRequestQueue.getMessages((hostRequests) -> {
            requestQueue = hostRequests;
            if(hostRequests != null && !hostRequests.isEmpty()){
                processedRequests.set(true);
            }
            status.setStatus(ScanStatus.STATUS.STARTED, requestQueue);
            try {
                boolean success = nmapScanner.processHosts(requestQueue);
                if(!success){
                    throw new RuntimeException("Failed running scan");
                }
            } catch (JMSException e) {
                throw new RuntimeException("Failed running scan");
            }
        }, true);
        return processedRequests.get();
    }

}
