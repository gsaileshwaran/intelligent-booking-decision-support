package com.booking.intelligent.repository;

import com.booking.intelligent.entity.CustomerNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, Long> {
    List<CustomerNotification> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserUserIdAndIsReadFalse(Long userId);
}
