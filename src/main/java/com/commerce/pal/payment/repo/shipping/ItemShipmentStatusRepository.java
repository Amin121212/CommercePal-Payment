package com.commerce.pal.payment.repo.shipping;

import com.commerce.pal.payment.model.shipping.ItemShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemShipmentStatusRepository extends JpaRepository<ItemShipmentStatus, Integer> {
}
