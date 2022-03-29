package com.commerce.pal.payment.repo;

import com.commerce.pal.payment.model.PalPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.expression.spel.ast.OpAnd;

import java.util.Optional;

public interface PalPaymentRepository extends JpaRepository<PalPayment, Long> {
    Optional<PalPayment> findPalPaymentByOrderRefAndTransRefAndStatus(String orderRef, String transRef, Integer status);
}
