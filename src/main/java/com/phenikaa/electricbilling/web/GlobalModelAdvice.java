package com.phenikaa.electricbilling.web;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Thuộc tính dùng chung cho mọi trang: người dùng hiện tại (cho thanh điều hướng) và ngày hiện tại. */
@ControllerAdvice
public class GlobalModelAdvice {

	/** Các ô văn bản được cắt khoảng trắng hai đầu trước khi kiểm tra; mật khẩu giữ nguyên. */
	private static final List<String> TRIMMED_FIELDS = List.of("username", "fullName", "address", "phone", "email",
			"meterNumber", "mailFrom", "companyName");

	private final Clock clock;

	public GlobalModelAdvice(Clock clock) {
		this.clock = clock;
	}

	@InitBinder
	public void trimTextFields(WebDataBinder binder) {
		StringTrimmerEditor trimmer = new StringTrimmerEditor(false);
		for (String field : TRIMMED_FIELDS) {
			binder.registerCustomEditor(String.class, field, trimmer);
		}
	}

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
