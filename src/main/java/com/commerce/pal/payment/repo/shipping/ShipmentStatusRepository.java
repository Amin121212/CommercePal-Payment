package com.commerce.pal.payment.repo.shipping;

import com.commerce.pal.payment.model.shipping.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ShipmentStatusRepository extends JpaRepository<ShipmentStatus, Integer> {
    Optional<ShipmentStatus> findShipmentStatusByCode(String code);

    @Query(value = "SELECT Code FROM ShipmentStatus ORDER BY Code ASC", nativeQuery = true)
    List<Integer> findShipmentStatusByCodeAndCode(String c,String b);



}
