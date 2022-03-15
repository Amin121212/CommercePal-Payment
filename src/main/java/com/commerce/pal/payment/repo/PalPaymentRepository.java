package com.commerce.pal.payment.repo;

import com.commerce.pal.payment.model.PalPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PalPaymentRepository extends JpaRepository<PalPayment, Long> {
}
