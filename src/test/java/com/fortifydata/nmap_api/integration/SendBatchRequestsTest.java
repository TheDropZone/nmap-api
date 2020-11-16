package com.fortifydata.nmap_api.integration;

import com.fortifydata.nmap_api.model.HostRequest;
import com.fortifydata.nmap_api.model.NmapScanType;
import com.fortifydata.nmap_api.queue.HostRequestQueue;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.JMSException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.fail;

@SpringBootTest
@ActiveProfiles("local-prod")
@RunWith(SpringRunner.class)
class SendBatchRequestsTest {

    @Autowired
    private HostRequestQueue hostRequestQueue;

    @Test
    void contextLoads() throws IOException {

        try(
                InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("targetScans.csv");
                InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(streamReader);
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
        ) {
            List<HostRequest> requests = StreamSupport.stream(csvParser.spliterator(),false)
                    .flatMap(record -> StreamSupport.stream(record.spliterator(),false))
                    .map(target -> {
                        HostRequest request = new HostRequest();
                        request.setCompanyId(12l);
                        request.setScanId(1l);
                        request.setScanType(NmapScanType.STATUS);
                        request.setHost(target);
                        return request;
                    }).collect(Collectors.toList());

            for(int i = 0; i < 60 ; i++){
                hostRequestQueue.sendAll(requests);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

}
