package com.fortifydata.nmap_api;

import com.ProActiveQueue.ProActiveQueueClient.Configuration.AwsConfig;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.ecs.AmazonECSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ProActiveQueue.ProActiveQueueClient.Configuration.AwsConfig.AwsConfigurationValues;

import java.util.List;

@Configuration
public class AwsConfiguration {

    @Value("${aws.accessKeyId}")
    private String accessKeyId;

    @Value("${aws.secretAccessKey}")
    private String secretAccessKey;

    @Value("${aws.activemq.brokerName}")
    private String activemqBroker;

    @Value("${aws.activemq.namespace}")
    private String activemqNamespace;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("#{'${aws.ecs.securityGroups}'.split(',')}")
    private List<String> securityGroups;

    @Value("#{'${aws.ecs.vpcSubnets}'.split(',')}")
    private List<String> vpcSubnets;

    @Bean
    public AwsConfig awsConfig(){
        AwsConfigurationValues configValues = new AwsConfigurationValues();
        configValues.setAwsAccessKey(accessKeyId);
        configValues.setAwsActiveMQBrokerName(activemqBroker);
        configValues.setAwsActiveMQNamespace(activemqNamespace);
        configValues.setAwsRegion(awsRegion);
        configValues.setAwsSecretKey(secretAccessKey);
        configValues.setEcsSecurityGroups(securityGroups);
        configValues.setEcsVpcSubnets(vpcSubnets);
        AwsConfig config = new AwsConfig(configValues);
        return config;
    }

    @Bean
    public AmazonCloudWatch cloudWatchClient(AwsConfig config){
        return config.getCloudWatchClient();
    }

    @Bean
    public AmazonECSClient ecsClient(AwsConfig config){
        return config.getECSClient();
    }
}
