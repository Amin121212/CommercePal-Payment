package com.commerce.pal.payment.repo.payment;

import com.commerce.pal.payment.model.payment.AgentCashPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentCashPaymentRepository extends JpaRepository<AgentCashPayment, Long> {
}
