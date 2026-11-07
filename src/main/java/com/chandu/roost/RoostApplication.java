
package com.chandu.roost;

import com.chandu.roost.probe.ProbeProperties;
import com.chandu.roost.ratelimit.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableConfigurationProperties({ProbeProperties.class, RateLimitProperties.class})
@EnableCaching
@EnableScheduling
@EnableAsync
public class RoostApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoostApplication.class, args);
	}
}
