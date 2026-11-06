package com.ranny.fotografia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class FotografiaApplication {

	public static void main(String[] args) {
		SpringApplication.run(FotografiaApplication.class, args);
	}

}
