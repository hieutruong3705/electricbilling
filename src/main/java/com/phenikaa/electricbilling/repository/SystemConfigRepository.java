package com.phenikaa.electricbilling.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.phenikaa.electricbilling.domain.SystemConfig;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
}
