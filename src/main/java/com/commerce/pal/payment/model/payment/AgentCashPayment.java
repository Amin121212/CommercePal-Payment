package com.commerce.pal.payment.model.payment;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
public class AgentCashPayment {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @Column(name = "order_ref")
    private String orderRef;
    @Basic
    @Column(name = "payment_ref")
    private String paymentRef;
    @Basic
    @Column(name = "customer_id")
    private Long customerId;
    @Basic
    @Column(name = "customer_email")
    private String customerEmail;
    @Basic
    @Column(name = "currency")
    private String currency;
    @Basic
    @Column(name = "amount")
    private BigDecimal amount;
    @Basic
    @Column(name = "validation_code")
    private String validationCode;
    @Basic
    @Column(name = "validation_expiry_date")
    private Timestamp validationExpiryDate;
    @Basic
    @Column(name = "status")
    private Integer status;
    @Basic
    @Column(name = "requested_date")
    private Timestamp requestedDate;
    @Basic
    @Column(name = "processing_agent_id")
    private Long processingAgentId;
    @Basic
    @Column(name = "response_payload")
    private String responsePayload;
    @Basic
    @Column(name = "processing_date")
    private Timestamp processingDate;

}
