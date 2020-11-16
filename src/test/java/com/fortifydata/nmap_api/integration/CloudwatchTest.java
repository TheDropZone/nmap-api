package com.fortifydata.nmap_api.integration;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@ActiveProfiles("local-cloudwatch")
@RunWith(SpringRunner.class)
public class CloudwatchTest {

    @Test
    void contextLoads() throws InterruptedException {
        while(true){
            Thread.sleep(100);
        }
    }
}
