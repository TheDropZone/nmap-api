package com.fortifydata.nmap_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NmapApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NmapApiApplication.class, args);
	}

}
