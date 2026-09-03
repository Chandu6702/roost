package com.chandu.roost;

import com.chandu.roost.config.ProbeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ProbeProperties.class)
public class RoostApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoostApplication.class, args);
	}

}
