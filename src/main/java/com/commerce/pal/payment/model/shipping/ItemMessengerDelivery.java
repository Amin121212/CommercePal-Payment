package com.commerce.pal.payment.model.shipping;

import lombok.*;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
public class ItemMessengerDelivery {
    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @Column(name = "OrderItemId")
    private Long orderItemId;
    @Basic
    @Column(name = "DeliveryType")
    private String deliveryType;
    @Basic
    @Column(name = "MessengerId")
    private Long messengerId;
    @Basic
    @Column(name = "MerchantId")
    private Long merchantId;
    @Basic
    @Column(name = "WareHouseId")
    private Long wareHouseId;
    @Basic
    @Column(name = "CustomerId")
    private Long customerId;
    @Basic
    @Column(name = "ValidationCode")
    private String validationCode;
    @Basic
    @Column(name = "ValidationStatus")
    private Integer validationStatus;
    @Basic
    @Column(name = "ValidationDate")
    private Timestamp validationDate;
    @Basic
    @Column(name = "DeliveryCode")
    private String deliveryCode;
    @Basic
    @Column(name = "DeliveryStatus")
    private Integer deliveryStatus;
    @Basic
    @Column(name = "DeliveryDate")
    private Timestamp deliveryDate;
    @Basic
    @Column(name = "ItemPickUpPhoto")
    private String itemPickUpPhoto;
    @Basic
    @Column(name = "ItemPickUpPhotoDate")
    private Timestamp itemPickUpPhotoDate;
    @Basic
    @Column(name = "Status")
    private Integer status;
    @Basic
    @Column(name = "CreatedDate")
    private Timestamp createdDate;
    @Basic
    @Column(name = "UpdatedDate")
    private Timestamp updatedDate;
}
