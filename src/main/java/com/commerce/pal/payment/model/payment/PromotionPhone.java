package com.commerce.pal.payment.model.payment;

import lombok.*;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
public class PromotionPhone {
    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @Column(name = "DeviceId")
    private String deviceId;
    @Basic
    @Column(name = "Phone")
    private String phone;
    @Basic
    @Column(name = "Channel")
    private String channel;
    @Basic
    @Column(name = "Status")
    private Integer status;
    @Basic
    @Column(name = "CreatedDate")
    private Timestamp createdDate;
    @Basic
    @Column(name = "TransRef")
    private String transRef;
    @Basic
    @Column(name = "SahayRef")
    private String sahayRef;
    @Basic
    @Column(name = "ResponsePayload")
    private String responsePayload;
    @Basic
    @Column(name = "ProcessStatus")
    private String processStatus;
    @Basic
    @Column(name = "ProcessMessage")
    private String processMessage;
    @Basic
    @Column(name = "ProcessedDate")
    private Timestamp processedDate;

}
