package com.commerce.pal.payment.repo.shipping;

import com.commerce.pal.payment.model.shipping.ItemMessengerDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemMessengerDeliveryRepository extends JpaRepository<ItemMessengerDelivery, Long> {

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByMerchantIdAndOrderItemId(Long merchant, Long item);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndMessengerIdAndDeliveryTypeIn(Long item, Long messenger, String[] deliveryType);

    Optional<ItemMessengerDelivery> findItemMessengerDeliveryByOrderItemIdAndMerchantIdAndDeliveryTypeIn(Long item, Long merchant, String[] deliveryType);
}
