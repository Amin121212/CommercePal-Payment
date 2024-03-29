package com.commerce.pal.payment.model.shipping;

import lombok.*;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "ItemShipmentComment")
public class ItemShipmentComment {
    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Basic
    @Column(name = "ItemShipmentId")
    private Integer itemShipmentId;
    @Basic
    @Column(name = "UserType")
    private String userType;
    @Basic
    @Column(name = "UserId")
    private long userId;
    @Basic
    @Column(name = "ShipmentStatus")
    private Integer shipmentStatus;
    @Basic
    @Column(name = "Comments")
    private String comments;
    @Basic
    @Column(name = "Status")
    private Integer status;
    @Basic
    @Column(name = "CreatedDate")
    private Timestamp createdDate;

}
