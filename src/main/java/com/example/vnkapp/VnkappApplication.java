package com.example.vnkapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VnkappApplication {

	public static void main(String[] args) {
		SpringApplication.run(VnkappApplication.class, args);
	}

}
