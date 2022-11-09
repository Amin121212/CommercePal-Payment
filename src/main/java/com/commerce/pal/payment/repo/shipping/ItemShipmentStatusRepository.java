package com.commerce.pal.payment.repo.shipping;

import com.commerce.pal.payment.model.shipping.ItemShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemShipmentStatusRepository extends JpaRepository<ItemShipmentStatus, Integer> {

    List<ItemShipmentStatus> findItemShipmentStatusesByItemId(Long item);

    Optional<ItemShipmentStatus> findItemShipmentStatusByItemIdAndShipmentStatus(Long item, Integer status);
}
