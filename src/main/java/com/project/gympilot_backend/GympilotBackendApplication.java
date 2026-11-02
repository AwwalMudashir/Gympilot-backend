package com.project.gympilot_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GympilotBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GympilotBackendApplication.class, args);
	}

}
