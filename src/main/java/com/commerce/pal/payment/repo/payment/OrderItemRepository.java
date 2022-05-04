package com.commerce.pal.payment.repo.payment;

import com.commerce.pal.payment.model.payment.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findOrderItemsByOrderId(Long orderId);
}
