package com.commerce.pal.payment.model.portal;

import lombok.*;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
public class WareHouse {
    @Basic
    @Column(name = "CreatedDate")
    private Timestamp createdDate;
    @Basic
    @Column(name = "Status")
    private Integer status;
    @Basic
    @Column(name = "PhysicalAddress")
    private String physicalAddress;
    @Basic
    @Column(name = "Longitude")
    private String longitude;
    @Basic
    @Column(name = "Latitude")
    private String latitude;
    @Basic
    @Column(name = "WareHouseName")
    private String wareHouseName;
    @Basic
    @Column(name = "ServiceAreaId")
    private Integer serviceAreaId;
    @Basic
    @Column(name = "RegionId")
    private Integer regionId;
    @Id
    @Column(name = "Id")
    private Integer id;

}
