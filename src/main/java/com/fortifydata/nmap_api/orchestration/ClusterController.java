package com.fortifydata.nmap_api.orchestration;

import com.ProActiveQueue.ProActiveQueueClient.Configuration.AwsConfig;
import com.ProActiveQueue.ProActiveQueueClient.Orchestration.EcsControllerService;
import com.amazonaws.services.ecs.model.*;
import com.fortifydata.nmap_api.queue.HostRequestQueue;
import com.fortifydata.nmap_api.scanning.ScanStatus;
import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableAsync
@EnableScheduling
@ConditionalOnProperty("orchestration.controller")
public class ClusterController {

    private final Logger log = LoggerFactory.getLogger(ClusterController.class);

    private String hostRequestQueueName;
    private String clusterName;
    private String serviceName;
    private String taskName;
    private EcsControllerService controller;

    @Autowired
    private ApplicationContext appContext;

    @Value("${nmap.batchSize}")
    private Integer nmapBatchSize;

    private final Integer BATCHES_PER_TASK = 3;
    private final Integer MAX_TASKS = 50;
    private final Integer MIN_TASKS = 1;

    private AtomicBoolean processing = new AtomicBoolean(false);

    public ClusterController(@Value("${aws.ecs.clusterName}") String clusterName,
                             @Value("${aws.ecs.serviceName}") String serviceName,
                             @Value("${aws.ecs.taskName}") String taskName,
                             @Value("${host.request.queueName}") String hostRequestQueueName,
                             @Autowired AwsConfig awsConfig){
        this.clusterName = clusterName;
        this.serviceName = serviceName;
        this.taskName = taskName;
        this.hostRequestQueueName = hostRequestQueueName;
        controller = new EcsControllerService(clusterName,serviceName,taskName,hostRequestQueueName,awsConfig);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void controller(){
        //register error handler
        controller.onErrorHandler((exception) -> {
            int exitCode = SpringApplication.exit(appContext, () -> 0);
            System.exit(exitCode);
        });

        controller.startController(MIN_TASKS,MAX_TASKS,BATCHES_PER_TASK * nmapBatchSize);
    }
}
