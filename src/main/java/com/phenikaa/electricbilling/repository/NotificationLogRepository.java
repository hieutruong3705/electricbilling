package com.phenikaa.electricbilling.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.phenikaa.electricbilling.domain.NotificationLog;
import com.phenikaa.electricbilling.domain.NotificationStatus;
import com.phenikaa.electricbilling.domain.NotificationType;

public interface NotificationLogRepository
		extends JpaRepository<NotificationLog, Long>, JpaSpecificationExecutor<NotificationLog> {

	boolean existsByBillIdAndTypeAndStatus(Long billId, NotificationType type, NotificationStatus status);
}
