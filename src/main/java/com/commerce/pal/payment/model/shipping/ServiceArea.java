package com.commerce.pal.payment.model.shipping;

import lombok.*;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "ServiceArea")
public class ServiceArea {
    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Basic
    @Column(name = "RegionId")
    private Integer regionId;
    @Basic
    @Column(name = "RegionCode")
    private String regionCode;
    @Basic
    @Column(name = "ServiceArea")
    private String serviceArea;
    @Basic
    @Column(name = "Status")
    private Integer status;
    @Basic
    @Column(name = "CreatedDate")
    private Timestamp createdDate;

}
