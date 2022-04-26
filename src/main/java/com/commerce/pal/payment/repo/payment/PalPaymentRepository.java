package com.commerce.pal.payment.repo.payment;

import com.commerce.pal.payment.model.payment.PalPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PalPaymentRepository extends JpaRepository<PalPayment, Long> {
    Optional<PalPayment> findPalPaymentByOrderRefAndTransRefAndStatus(String orderRef, String transRef, Integer status);
}
