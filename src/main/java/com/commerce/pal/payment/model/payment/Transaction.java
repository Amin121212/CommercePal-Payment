package com.commerce.pal.payment.model.payment;

import lombok.*;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
public class Transaction {
    @Basic
    @Column(name = "Country")
    private String country;
    @Basic
    @Column(name = "TransRef")
    private String transRef;
    @Basic
    @Column(name = "TransDate")
    private Timestamp transDate;
    @Basic
    @Column(name = "Narration")
    private String narration;
    @Basic
    @Column(name = "AvailableBalance")
    private BigDecimal availableBalance;
    @Basic
    @Column(name = "DrCr")
    private String drCr;
    @Basic
    @Column(name = "Channel")
    private String channel;
    @Basic
    @Column(name = "Currency")
    private String currency;
    @Basic
    @Column(name = "Amount")
    private BigDecimal amount;
    @Basic
    @Column(name = "Account")
    private String account;
    @Basic
    @Column(name = "AccountType")
    private String accountType;
    @Basic
    @Column(name = "PaymentType")
    private String paymentType;
    @Basic
    @Column(name = "TransType")
    private String transType;
    @Basic
    @Column(name = "MsgType")
    private String msgType;
    @Id
    @Column(name = "Id")
    private Long id;

}
