package com.fortifydata.nmap_api;

import com.ProActiveQueue.ProActiveQueueClient.Configuration.QueueConfig;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.Queue;

@Configuration
public class ActiveMQConfig {

    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Value("${spring.activemq.user}")
    private String username;

    @Value("${spring.activemq.password}")
    private String password;

    @Bean
    public QueueConfig queueConfig(){
        QueueConfig config = new QueueConfig(brokerUrl,username,password);
        return config;
    }

    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory(QueueConfig config) {
        return config.getActiveMQConnectionFactory();
    }

    @Bean(name="host.request.queue")
    public Queue hostRequestQueue(@Value("${host.request.queueName}") String queueName, QueueConfig config){
        return config.getQueueWithName(queueName);
    }

    @Bean(name="host.scanned.queue")
    public Queue scannedHostQueue(@Value("${host.scanned.queueName}") String queueName, QueueConfig config){
        return config.getQueueWithName(queueName);
    }

}
