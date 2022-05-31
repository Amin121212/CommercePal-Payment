package com.commerce.pal.payment.repo.payment;

import com.commerce.pal.payment.model.payment.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findOrderByOrderRef(String orderRef);

    Optional<Order> findOrderByOrderId(Long id);

    List<Order> findOrdersByStatusAndPaymentStatusOrderByOrderIdDesc(Integer status, Integer payment, Pageable page);
}
