package com.phenikaa.electricbilling.config;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.phenikaa.electricbilling.domain.AppUser;
import com.phenikaa.electricbilling.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

/** Nạp tài khoản từ CSDL cho Spring Security; tài khoản bị khóa được đánh dấu disabled. */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser user = userRepository.findByUsername(username.trim().toLowerCase())
				.orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
		return User.withUsername(user.getUsername())
				.password(user.getPasswordHash())
				.roles(user.getRole().name())
				.disabled(!user.isEnabled())
				.build();
	}
}
