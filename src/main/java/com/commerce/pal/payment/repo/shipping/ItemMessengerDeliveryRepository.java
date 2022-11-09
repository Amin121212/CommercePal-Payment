package com.commerce.pal.payment.repo.shipping;

import com.commerce.pal.payment.model.shipping.ItemMessengerDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemMessengerDeliveryRepository extends JpaRepository<ItemMessengerDelivery, Long> {

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeIn(Long item, Long messenger, String[] deliveryType);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndMerchantIdAndDeliveryTypeIn(Long item, Long merchant, String[] deliveryType);

    List<ItemMessengerDelivery> findItemMessengerDeliveriesByMessengerIdAndStatusInAndDeliveryStatusIn(Long messenger, Integer[] status, Integer[] deliveryStatus);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByIdAndMessengerId(Long deliveryId, Long messenger);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndDeliveryTypeAndValidationStatus(Long item, String type, Integer status);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndCustomerId(Long item, Long messenger);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeAndDeliveryStatus(Long item, Long messenger, String Type, Integer status);
}
