package com.commerce.pal.payment.model.payment;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "OrderItem")
public class OrderItem {
    @Id
    @Column(name = "ItemId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long itemId;
    @Basic
    @Column(name = "OrderId")
    private Long orderId;
    @Basic
    @Column(name = "SubOrderNumber")
    private String subOrderNumber;
    @Basic
    @Column(name = "ProductLinkingId")
    private Long productLinkingId;
    @Basic
    @Column(name = "SubProductId")
    private Long subProductId;
    @Basic
    @Column(name = "MerchantId")
    private Long merchantId;
    @Basic
    @Column(name = "UnitPrice")
    private BigDecimal unitPrice;
    @Basic
    @Column(name = "Quantity")
    private Integer quantity;
    @Basic
    @Column(name = "IsDiscount")
    private Integer isDiscount;
    @Basic
    @Column(name = "DiscountType")
    private String discountType;
    @Basic
    @Column(name = "DiscountValue")
    private BigDecimal discountValue;
    @Basic
    @Column(name = "DiscountAmount")
    private BigDecimal discountAmount;
    @Basic
    @Column(name = "TotalAmount")
    private BigDecimal totalAmount;
    @Basic
    @Column(name = "TaxValue")
    private BigDecimal taxValue;
    @Basic
    @Column(name = "TaxAmount")
    private BigDecimal taxAmount;
    @Basic
    @Column(name = "TotalDiscount")
    private BigDecimal totalDiscount;
    @Basic
    @Column(name = "DeliveryPrice")
    private BigDecimal deliveryPrice;
    @Basic
    @Column(name = "UserShipmentStatus")
    private Integer userShipmentStatus;
    @Basic
    @Column(name = "ShipmentType")
    private String shipmentType;
    @Basic
    @Column(name = "ShipmentTypeComments")
    private String shipmentTypeComments;
    @Basic
    @Column(name = "ShipmentStatus")
    private Integer shipmentStatus;
    @Basic
    @Column(name = "ShipmentUpdateDate")
    private Timestamp shipmentUpdateDate;
    @Basic
    @Column(name = "AssignedWareHouseId")
    private Integer assignedWareHouseId;
    @Basic
    @Column(name = "FinalizingWareHouseId")
    private Integer finalizingWareHouseId;
    @Basic
    @Column(name = "Status")
    private Integer status;
    @Basic
    @Column(name = "StatusDescription")
    private String statusDescription;
    @Basic
    @Column(name = "CreatedDate")
    private Timestamp createdDate;

}
