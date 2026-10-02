package com.phenikaa.electricbilling.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.phenikaa.electricbilling.domain.AppUser;
import com.phenikaa.electricbilling.domain.Role;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByRole(Role role);
}
