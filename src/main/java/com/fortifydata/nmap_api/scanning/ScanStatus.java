package com.fortifydata.nmap_api.scanning;

import com.fortifydata.nmap_api.model.HostRequest;
import com.google.common.base.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableAsync
@EnableScheduling
@ConditionalOnProperty("scan.status.broadcast")
public class ScanStatus {

    private final Logger log = LoggerFactory.getLogger(ScanStatus.class);

    private STATUS status = STATUS.IDLE;

    private STATUS lastStatus = STATUS.IDLE;

    public List<HostRequest> currentRequests;

    @Async("asyncExecutor")
    @Scheduled(fixedRate = 1000 * 5, initialDelay = 0)
    public void every5seconds() throws IOException, SQLException, InterruptedException {
        if(!Objects.equal(status, lastStatus)){
            broadcastUpdate();
        }else if(currentRequests != null && !currentRequests.isEmpty()){
            broadcastUpdate();
        }
        this.lastStatus = this.status;
    }

    public void broadcastUpdate(){

        log.info("Currently: " + this.status +
                ((currentRequests != null && !currentRequests.isEmpty()) ?
                        (" | hosts: " + currentRequests.stream().map(request -> request.getHost()).collect(Collectors.joining(", ")))
                        : ""));
    }

    public void setStatus(STATUS status, List<HostRequest> requests){
        this.lastStatus = this.status;
        this.status = status;
        if(status.equals(STATUS.IDLE)){
            this.currentRequests = null;
        }
        this.currentRequests = requests;
        broadcastUpdate();
    }

    public void setStatus(STATUS status){
        this.lastStatus = this.status;
        this.status = status;
        if(status.equals(STATUS.IDLE)){
            this.currentRequests = null;
        }

        broadcastUpdate();
    }

    public void setCurrentRequests(List<HostRequest> requests){
        this.currentRequests = requests;
    }

    public static enum STATUS{
        IDLE,
        STARTED,
        SCANNING,
        COMPLETED,
        ERROR,
        SHUT_DOWN
    }
}
