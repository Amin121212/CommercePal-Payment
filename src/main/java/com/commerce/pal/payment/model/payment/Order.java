package com.commerce.pal.payment.model.payment;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "[dbo].[Order]")
public class Order {
    @Id
    @Column(name = "OrderId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;
    @Basic
    @Column(name = "OrderRef")
    private String orderRef;
    @Basic
    @Column(name = "CustomerId")
    private Long customerId;
    @Basic
    @Column(name = "MerchantId")
    private Long merchantId;
    @Basic
    @Column(name = "AgentId")
    private Long agentId;
    @Basic
    @Column(name = "BusinessId")
    private Long businessId;
    @Basic
    @Column(name = "PaymentMethod")
    private String paymentMethod;
    @Basic
    @Column(name = "SaleType")
    private String saleType;
    @Basic
    @Column(name = "Currency")
    private String currency;
    @Basic
    @Column(name = "CountryCode")
    private String countryCode;
    @Basic
    @Column(name = "TotalPrice")
    private BigDecimal totalPrice;
    @Basic
    @Column(name = "Discount")
    private BigDecimal discount;
    @Basic
    @Column(name = "Tax")
    private BigDecimal tax;
    @Basic
    @Column(name = "DeliveryPrice")
    private BigDecimal deliveryPrice;
    @Basic
    @Column(name = "Charge")
    private BigDecimal charge;
    @Basic
    @Column(name = "PromotionId")
    private Integer promotionId;
    @Basic
    @Column(name = "PromotionAmount")
    private BigDecimal promotionAmount;
    @Basic
    @Column(name = "ReferralUserType")
    private String referralUserType;
    @Basic
    @Column(name = "ReferralUserId")
    private Integer referralUserId;

    @Basic
    @Column(name = "OrderDate")
    private Timestamp orderDate;
    @Basic
    @Column(name = "IsAgentInitiated")
    private Integer isAgentInitiated;
    @Basic
    @Column(name = "IsUserAddressAssigned")
    private Integer isUserAddressAssigned;
    @Basic
    @Column(name = "PreferredLocationType")
    private String preferredLocationType;
    @Basic
    @Column(name = "UserAddressId")
    private Long userAddressId;
    @Basic
    @Column(name = "Status")
    private Integer status;
    @Basic
    @Column(name = "StatusDescription")
    private String statusDescription;
    @Basic
    @Column(name = "VerifiedBy")
    private String verifiedBy;
    @Basic
    @Column(name = "PaymentStatus")
    private Integer paymentStatus;
    @Basic
    @Column(name = "PaymentStatusDescription")
    private String paymentStatusDescription;
    @Basic
    @Column(name = "SahayRef")
    private String sahayRef;
    @Basic
    @Column(name = "BillerReference")
    private String billerReference;
    @Basic
    @Column(name = "ShippingStatus")
    private String shippingStatus;
    @Basic
    @Column(name = "PaymentDate")
    private Timestamp paymentDate;
}
