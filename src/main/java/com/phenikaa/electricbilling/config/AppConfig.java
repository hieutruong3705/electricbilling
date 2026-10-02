package com.phenikaa.electricbilling.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableAsync
public class AppConfig {

	/** Đồng hồ hệ thống; tách thành bean để kiểm thử có thể cố định ngày. */
	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}
