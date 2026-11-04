package com.tcs.VendorPulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VendorPulseApplication {

	public static void main(String[] args) {

		Dotenv dotenv = Dotenv.configure().load();
		dotenv.entries().foreach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		SpringApplication.run(VendorPulseApplication.class, args);
	}

}
