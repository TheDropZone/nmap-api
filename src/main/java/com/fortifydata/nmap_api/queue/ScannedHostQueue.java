package com.fortifydata.nmap_api.queue;

import com.ProActiveQueue.ProActiveQueueClient.Connection.MessageClient;
import com.fortifydata.nmap_api.model.ScannedHost;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Queue;

@Component
public class ScannedHostQueue extends MessageClient<ScannedHost> {

    public ScannedHostQueue(@Qualifier("host.scanned.queue") Queue queue, ActiveMQConnectionFactory factory) throws JMSException {
        super(queue,factory);
    }
}
