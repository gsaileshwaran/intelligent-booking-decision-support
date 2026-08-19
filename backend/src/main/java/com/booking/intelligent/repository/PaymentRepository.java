package com.booking.intelligent.repository;

import com.booking.intelligent.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingBookingId(Long bookingId);
    Optional<Payment> findByTransactionRef(String transactionRef);
}
