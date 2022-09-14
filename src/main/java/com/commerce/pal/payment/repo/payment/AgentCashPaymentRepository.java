package com.commerce.pal.payment.repo.payment;

import com.commerce.pal.payment.model.payment.AgentCashPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentCashPaymentRepository extends JpaRepository<AgentCashPayment, Long> {

    Optional<AgentCashPayment> findAgentCashPaymentByOrderRefAndPaymentRefAndStatus(String orderRef, String transRef, Integer status);

    Optional<AgentCashPayment> findAgentCashPaymentByPaymentRef(String ref);
}
