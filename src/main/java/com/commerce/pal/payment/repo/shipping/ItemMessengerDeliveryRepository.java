package com.commerce.pal.payment.repo.shipping;

import com.commerce.pal.payment.model.shipping.ItemMessengerDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemMessengerDeliveryRepository extends JpaRepository<ItemMessengerDelivery, Long> {

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeIn(Long item, Long messenger, String[] deliveryType);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeInAndValidationStatus (Long item, Long messenger, String[] deliveryType, Integer status);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndMerchantIdAndDeliveryTypeIn(Long item, Long merchant, String[] deliveryType);

    List<ItemMessengerDelivery> findItemMessengerDeliveriesByMessengerIdAndStatusInAndDeliveryStatusIn(Long messenger, Integer[] status, Integer[] deliveryStatus);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByIdAndMessengerId(Long deliveryId, Long messenger);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndDeliveryTypeAndValidationStatus(Long item, String type, Integer status);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndCustomerId(Long item, Long messenger);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndDeliveryTypeAndDeliveryStatus(Long item, String Type, Integer status);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeAndValidationStatus(Long item, Long messenger, String type, Integer status);
}
