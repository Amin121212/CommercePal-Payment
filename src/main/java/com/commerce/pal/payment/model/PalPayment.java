package com.commerce.pal.payment.model;

import lombok.*;

import javax.persistence.*;
import java.sql.Date;
import java.util.Objects;

@Data
@Entity
public class PalPayment {
    @Basic
    @Column(name = "FinalResponseDate")
    private Date finalResponseDate;
    @Basic
    @Column(name = "FinalResponseMessage")
    private String finalResponseMessage;
    @Basic
    @Column(name = "FinalResponse")
    private String finalResponse;
    @Basic
    @Column(name = "ResponseDate")
    private Date responseDate;
    @Basic
    @Column(name = "ResponsePayload")
    private String responsePayload;
    @Basic
    @Column(name = "RequestDate")
    private Date requestDate;
    @Basic
    @Column(name = "RequestPayload")
    private String requestPayload;
    @Basic
    @Column(name = "Status")
    private int status;
    @Basic
    @Column(name = "Amount")
    private long amount;
    @Basic
    @Column(name = "Currency")
    private String currency;
    @Basic
    @Column(name = "AccountNumber")
    private String accountNumber;
    @Basic
    @Column(name = "PaymentChannel")
    private String paymentChannel;
    @Basic
    @Column(name = "TransType")
    private String transType;
    @Basic
    @Column(name = "TransRef")
    private String transRef;
    @Basic
    @Column(name = "UserId")
    private long userId;
    @Basic
    @Column(name = "UserType")
    private String userType;
    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

}
