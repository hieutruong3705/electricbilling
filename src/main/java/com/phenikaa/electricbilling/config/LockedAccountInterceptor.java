package com.phenikaa.electricbilling.config;

import java.io.IOException;
import java.security.Principal;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.phenikaa.electricbilling.repository.AppUserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** Hộ bị khóa khi đang đăng nhập: hủy phiên đang mở và đưa về trang đăng nhập (BR-15). */
@Component
@RequiredArgsConstructor
public class LockedAccountInterceptor implements HandlerInterceptor {

	private final AppUserRepository userRepository;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		Principal principal = request.getUserPrincipal();
		if (principal == null) {
			return true;
		}
		boolean locked = userRepository.findByUsername(principal.getName()).map(u -> !u.isEnabled()).orElse(true);
		if (!locked) {
			return true;
		}
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		response.sendRedirect(request.getContextPath() + "/login?locked");
		return false;
	}
}
