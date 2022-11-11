package com.commerce.pal.payment.model.shipping;

import lombok.*;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
public class ItemCustomerPickUp {
    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @Column(name = "OrderItemId")
    private Long orderItemId;
    @Basic
    @Column(name = "CollectionType")
    private String collectionType;
    @Basic
    @Column(name = "AgentId")
    private Long agentId;
    @Basic
    @Column(name = "WareHouseId")
    private Long wareHouseId;
    @Basic
    @Column(name = "CustomerId")
    private Long customerId;
    @Basic
    @Column(name = "CollectionCode")
    private String collectionCode;
    @Basic
    @Column(name = "CollectionStatus")
    private Integer collectionStatus;
    @Basic
    @Column(name = "CollectionDate")
    private Timestamp collectionDate;
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
