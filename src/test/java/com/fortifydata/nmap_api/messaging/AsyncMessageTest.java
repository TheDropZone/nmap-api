package com.fortifydata.nmap_api.messaging;

import com.ProActiveQueue.ProActiveQueueClient.Connection.MessageClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fortifydata.nmap_api.model.HostRequest;
import com.fortifydata.nmap_api.model.NmapScanType;
import com.fortifydata.nmap_api.queue.HostRequestQueue;
import com.fortifydata.nmap_api.reciever.HostRequestReciever;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.nmap4j.model.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootTest
@ActiveProfiles("local")
@RunWith(SpringRunner.class)
class AsyncMessageTest {

    private final Logger log = LoggerFactory.getLogger(AsyncMessageTest.class);

    @Autowired
    private HostRequestQueue hostRequestQueue;

    @Qualifier("activeMQConnectionFactory")
    @Autowired
    private ActiveMQConnectionFactory factory;

    public Topic hostRequestTopic(){
        return new ActiveMQTopic("host.request.topic");
    }

    @Autowired
    @Qualifier("host.request.queue")
    public Queue queue;


    private AtomicReference requestReference = new AtomicReference<HostRequest>(null);

    private HostRequest getRequest(){
        HostRequest request = new HostRequest();
        request.setScanType(NmapScanType.STATUS);
        request.setHost("MyHost.com");
        request.setScanId(1L);
        request.setCompanyId(1L);
        return request;
    }

    @Test
    void asyncQueue() throws JsonProcessingException, JMSException, InterruptedException {
        hostRequestQueue.onMessageReceived(hostRequest -> {
            log.info("Received host request on async listener");
            requestReference.set(hostRequest);
        });
        log.info("Registered Async message listener");

        log.info("Sending Request");
        hostRequestQueue.send(getRequest());
        log.info("Request Sent");

        Thread.sleep(1000);
        Assert.assertNotNull(requestReference.get());
        System.out.println(requestReference.get());
        hostRequestQueue.close();
    }

    @Test
    void asyncTransactionalQueue() throws JsonProcessingException, JMSException, InterruptedException {

        HostRequestQueue hostRequestQueue1 = new HostRequestQueue(queue,factory);
        hostRequestQueue1.onMessageReceived(hostRequest -> {
            log.info("Received host request on async listener. Throwing error");
            int i = 10/0;
            requestReference.set(hostRequest);
        });
        log.info("Registered Async message listener 1");

        HostRequestQueue hostRequestQueue2 = new HostRequestQueue(queue,factory);
        hostRequestQueue2.onMessageReceived(hostRequest -> {
            log.info("Received host request on 2nd async listener");
            requestReference.set(hostRequest);
        });
        log.info("Registered Async message listener 2");

        log.info("Sending Request");
        HostRequestQueue hostRequestQueue3 = new HostRequestQueue(queue,factory);
        hostRequestQueue3.send(getRequest());
        log.info("Request Sent");

        Thread.sleep(100);
        Assert.assertNotNull(requestReference.get());
        System.out.println(requestReference.get().toString());
        hostRequestQueue1.close();
        hostRequestQueue2.close();
    }

    @Test
    void asyncTopic() throws JMSException, InterruptedException, JsonProcessingException {
        AtomicInteger atomicCount = new AtomicInteger(0);

        int count = 10;
        List<Thread> joins = new ArrayList<>();
        for(var i = 0; i < count; i++){
            final int index = i;
            var thread = (new Thread(() -> {
                try {
                    var hostRequestTopic = new HostRequestTopic(hostRequestTopic(),factory);
                    hostRequestTopic.onMessageReceived((hostRequest -> {
                        System.out.println(hostRequest);
                        atomicCount.incrementAndGet();
                        try {
                            hostRequestTopic.close();
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    }));
                    System.out.println("Registered listener " + index);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }));
            thread.start();
            joins.add(thread);
        }
        for (Thread thread : joins) {
            thread.join();
        }

        var hostRequestTopic = new HostRequestTopic(hostRequestTopic(),factory);
        hostRequestTopic.send(getRequest());
        Thread.sleep(100);
        Assert.assertEquals(count,atomicCount.get());

    }


    public class HostRequestTopic extends MessageClient<HostRequest> {

        public HostRequestTopic(Destination destination, ActiveMQConnectionFactory factory) throws JMSException {
            super(destination, factory);
        }
    }
}