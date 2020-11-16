package com.fortifydata.nmap_api.integration;

import com.fortifydata.nmap_api.model.HostRequest;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@ActiveProfiles("local-receiver")
@RunWith(SpringRunner.class)
public class ReceiveRequestsTest {

    @Test
    void contextLoads() {
    }
}
