package com.fortifydata.nmap_api.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fortifydata.nmap_api.model.HostRequest;
import com.fortifydata.nmap_api.model.NmapScanType;
import com.fortifydata.nmap_api.queue.HostRequestQueue;
import com.fortifydata.nmap_api.queue.ScannedHostQueue;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.JMSException;
import java.util.List;

import static org.junit.Assert.fail;

@SpringBootTest
@ActiveProfiles("local")
@RunWith(SpringRunner.class)
class SendRequestsTest {

	@Autowired
	private HostRequestQueue hostRequestQueue;

	@Test
	void contextLoads() throws JsonProcessingException, JMSException {
		HostRequest request = new HostRequest();
		request.setCompanyId(1l);
		request.setScanType(NmapScanType.STATUS);
		request.setHost("matt.beginnertriathlete.com");

		HostRequest request2 = new HostRequest();
		request2.setCompanyId(2l);
		request2.setScanType(NmapScanType.STATUS);
		request2.setHost("dev2.beginnertriathlete.com");

		HostRequest request3 = new HostRequest();
		request3.setCompanyId(3l);
		request3.setScanType(NmapScanType.STATUS_FULL);
		request3.setHost("irc-wolf.hackthissite.org");

		HostRequest request4 = new HostRequest();
		request4.setCompanyId(4l);
		request4.setScanType(NmapScanType.STATUS_FULL);
		request4.setHost("109.166.244.14");

		hostRequestQueue.sendAll(List.of(request,request2,request3,request4));
	}

}
