package com.commerce.pal.payment.model.payment;

import lombok.*;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "PalPayment")
public class PalPayment {
    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @Column(name = "UserType")
    private String userType;
    @Basic
    @Column(name = "UserEmail")
    private String userEmail;
    @Basic
    @Column(name = "OrderRef")
    private String orderRef;
    @Basic
    @Column(name = "TransRef")
    private String transRef;
    @Basic
    @Column(name = "TransType")
    private String transType;
    @Basic
    @Column(name = "PaymentChannel")
    private String paymentChannel;
    @Basic
    @Column(name = "AccountNumber")
    private String accountNumber;
    @Basic
    @Column(name = "Amount")
    private Double amount;
    @Basic
    @Column(name = "Currency")
    private String currency;
    @Basic
    @Column(name = "Status")
    private Integer status;
    @Basic
    @Column(name = "RequestPayload")
    private String requestPayload;
    @Basic
    @Column(name = "RequestDate")
    private Timestamp requestDate;
    @Basic
    @Column(name = "BillTransRef")
    private String billTransRef;
    @Basic
    @Column(name = "ResponseDate")
    private Timestamp responseDate;
    @Basic
    @Column(name = "ResponsePayload")
    private String responsePayload;
    @Basic
    @Column(name = "FinalResponseMessage")
    private String finalResponseMessage;
    @Basic
    @Column(name = "FinalResponse")
    private String finalResponse;
    @Basic
    @Column(name = "FinalResponseDate")
    private Timestamp finalResponseDate;

}
