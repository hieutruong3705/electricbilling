package com.phenikaa.electricbilling.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.phenikaa.electricbilling.domain.AppUser;
import com.phenikaa.electricbilling.domain.Role;
import com.phenikaa.electricbilling.repository.AppUserRepository;
import com.phenikaa.electricbilling.service.SettingsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Khi khởi động: tạo biểu giá/tham số mặc định và tài khoản ADMIN đầu tiên nếu chưa có. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SettingsService settingsService;

	@Value("${app.admin.username}")
	private String adminUsername;

	@Value("${app.admin.password}")
	private String adminPassword;

	@Override
	public void run(ApplicationArguments args) {
		settingsService.seedDefaults();
		if (!userRepository.existsByRole(Role.ADMIN)) {
			userRepository.save(new AppUser(adminUsername.toLowerCase(), passwordEncoder.encode(adminPassword), Role.ADMIN));
			log.info("Đã tạo tài khoản quản trị mặc định '{}'", adminUsername);
		}
	}
}
