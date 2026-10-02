package com.phenikaa.electricbilling.web;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;

/** Thuộc tính dùng chung cho mọi trang: người dùng hiện tại (cho thanh điều hướng) và ngày hiện tại. */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

	private final Clock clock;

	/** ADMIN, CUSTOMER hoặc null nếu chưa đăng nhập. */
	@ModelAttribute("currentRole")
	public String currentRole() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
			return null;
		}
		return auth.getAuthorities().stream().findFirst()
				.map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).orElse(null);
	}

	@ModelAttribute("currentUsername")
	public String currentUsername() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
			return null;
		}
		return auth.getName();
	}

	@ModelAttribute("today")
	public LocalDate today() {
		return LocalDate.now(clock);
	}
}
