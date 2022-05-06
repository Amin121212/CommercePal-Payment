package com.commerce.pal.payment.repo.payment;

import com.commerce.pal.payment.model.payment.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findOrderItemsByOrderId(Long orderId);

    @Query(value = "SELECT MerchantId  FROM OrderItem WHERE OrderId = ?1  GROUP BY MerchantId", nativeQuery = true)
    List<Long> findAllByOrderId(Long orderId);

    @Query(value = "SELECT OrderId  FROM OrderItem WHERE MerchantId = :merchantId AND UserShipmentStatus IN :status AND Status = 3 GROUP BY OrderId", nativeQuery = true)
    List<Long> findByMerchantIdAndShipmentStatus(Long merchantId, List<Integer> status);

    List<OrderItem> findOrderItemsByOrderIdAndMerchantId(Long orderId, Long merchant);
}
