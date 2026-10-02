package com.phenikaa.electricbilling.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.phenikaa.electricbilling.domain.Customer;
import com.phenikaa.electricbilling.domain.CustomerStatus;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

	Optional<Customer> findByUserUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	boolean existsByMeterNumber(String meterNumber);

	boolean existsByMeterNumberAndIdNot(String meterNumber, Long id);

	long countByStatus(CustomerStatus status);

	/** Gợi ý chọn hộ khi ghi chỉ số. */
	@Query("""
			select c from Customer c
			where c.status = com.phenikaa.electricbilling.domain.CustomerStatus.ACTIVE
			  and (lower(c.fullName) like :kw or lower(c.customerCode) like :kw
			       or lower(c.meterNumber) like :kw or c.phone like :kw)
			order by c.customerCode
			""")
	List<Customer> searchActive(String kw, org.springframework.data.domain.Pageable pageable);
}
