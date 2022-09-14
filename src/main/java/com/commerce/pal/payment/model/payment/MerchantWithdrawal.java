package com.commerce.pal.payment.model.payment;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Entity
public class MerchantWithdrawal {
    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @Column(name = "MerchantId")
    private Long merchantId;
    @Basic
    @Column(name = "TransRef")
    private String transRef;
    @Basic
    @Column(name = "WithdrawalMethod")
    private String withdrawalMethod;
    @Basic
    @Column(name = "WithdrawalType")
    private String withdrawalType;
    @Basic
    @Column(name = "Account")
    private String account;
    @Basic
    @Column(name = "Amount")
    private BigDecimal amount;
    @Basic
    @Column(name = "ValidationCode")
    private String validationCode;
    @Basic
    @Column(name = "ValidationDate")
    private Timestamp validationDate;
    @Basic
    @Column(name = "Status")
    private Integer status;
    @Basic
    @Column(name = "RequestDate")
    private Timestamp requestDate;
    @Basic
    @Column(name = "ResponseStatus")
    private Integer responseStatus;
    @Basic
    @Column(name = "ResponseDescription")
    private String responseDescription;
    @Basic
    @Column(name = "ResponseDate")
    private Timestamp responseDate;

}
