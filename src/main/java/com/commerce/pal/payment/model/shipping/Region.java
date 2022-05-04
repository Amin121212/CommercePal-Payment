package com.commerce.pal.payment.model.shipping;

import lombok.*;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "Region", schema = "dbo", catalog = "CommercePal")
public class Region {
    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Basic
    @Column(name = "RegionCode")
    private String regionCode;
    @Basic
    @Column(name = "RegionName")
    private String regionName;
    @Basic
    @Column(name = "Country")
    private String country;
    @Basic
    @Column(name = "Status")
    private Integer status;
    @Basic
    @Column(name = "CreatedDate")
    private Timestamp createdDate;

}
