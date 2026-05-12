package com.tlat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TlatApplication {

	public static void main(String[] args) {
		SpringApplication.run(TlatApplication.class, args);
	}

}
