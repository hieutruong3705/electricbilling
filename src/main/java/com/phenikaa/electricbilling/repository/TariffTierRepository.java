package com.phenikaa.electricbilling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.phenikaa.electricbilling.domain.TariffTier;

public interface TariffTierRepository extends JpaRepository<TariffTier, Long> {

	List<TariffTier> findAllByOrderByTierNoAsc();
}
