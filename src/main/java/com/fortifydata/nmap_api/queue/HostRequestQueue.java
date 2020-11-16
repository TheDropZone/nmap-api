package com.fortifydata.nmap_api.queue;

import com.ProActiveQueue.ProActiveQueueClient.Connection.MessageClient;
import com.ProActiveQueue.ProActiveQueueClient.Connection.MessageProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fortifydata.nmap_api.model.HostRequest;
import com.fortifydata.nmap_api.model.NmapScanType;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

@Component
public class HostRequestQueue extends MessageClient<HostRequest> {

    private MessageClient<HostRequest> client;

    @Value("${nmap.batchSize}")
    private Integer QUEUE_SIZE;

    public HostRequestQueue(@Qualifier("host.request.queue") Queue queue, ActiveMQConnectionFactory factory) throws JMSException {
        super(queue,factory);
    }

    public void getMessages (Consumer<List<HostRequest>> onMessage) throws IOException, JMSException {
        super.getMessages(onMessage, QUEUE_SIZE);
    }

    public void getMessages (Consumer<List<HostRequest>> onMessage, boolean matchScanType) throws IOException, JMSException {
        if(matchScanType){
            super.getMessages(onMessage, QUEUE_SIZE, MessageProperties.builder().addProperty(NmapScanType.class.getSimpleName(), MessageProperties.Selector.MATCHES));
        }else{
            getMessages(onMessage);
        }

    }

    @Override
    public void sendAll(List<HostRequest> hosts) throws JsonProcessingException, JMSException {
        super.sendAll(hosts,(host) -> {
            if(host.getScanType() != null){
                Properties props = new Properties();
                props.setProperty(NmapScanType.class.getSimpleName(),host.getScanType().toString());
                return props;
            }else{
                return null;
            }
        });
    }

    @Override
    public void send(HostRequest host) throws JsonProcessingException, JMSException {
        if(host.getScanType() != null){
            Properties props = new Properties();
            props.setProperty(NmapScanType.class.getSimpleName(),host.getScanType().toString());
            super.send(host, props);
        }else{
            super.send(host, null);
        }
    }
}
