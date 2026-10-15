package com.phenikaa.electricbilling.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final LockedAccountInterceptor lockedAccountInterceptor;

	public WebConfig(LockedAccountInterceptor lockedAccountInterceptor) {
		this.lockedAccountInterceptor = lockedAccountInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(lockedAccountInterceptor).addPathPatterns("/customer/**");
	}
}
