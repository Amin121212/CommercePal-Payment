package com.commerce.pal.payment.model.shipping;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Date;
import java.util.Objects;

@Entity
public class ItemMessengerDelivery {
    private long id;
    private int orderItemId;
    private String deliveryType;
    private long messengerId;
    private Long merchantId;
    private Long wareHouseId;
    private Long customerId;
    private String validationCode;
    private int validationStatus;
    private Date validationDate;
    private Integer status;
    private Date createdDate;
    private Date updatedDate;

    @Id
    @Column(name = "Id")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Basic
    @Column(name = "OrderItemId")
    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    @Basic
    @Column(name = "DeliveryType")
    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    @Basic
    @Column(name = "MessengerId")
    public long getMessengerId() {
        return messengerId;
    }

    public void setMessengerId(long messengerId) {
        this.messengerId = messengerId;
    }

    @Basic
    @Column(name = "MerchantId")
    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    @Basic
    @Column(name = "WareHouseId")
    public Long getWareHouseId() {
        return wareHouseId;
    }

    public void setWareHouseId(Long wareHouseId) {
        this.wareHouseId = wareHouseId;
    }

    @Basic
    @Column(name = "CustomerId")
    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    @Basic
    @Column(name = "ValidationCode")
    public String getValidationCode() {
        return validationCode;
    }

    public void setValidationCode(String validationCode) {
        this.validationCode = validationCode;
    }

    @Basic
    @Column(name = "ValidationStatus")
    public int getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(int validationStatus) {
        this.validationStatus = validationStatus;
    }

    @Basic
    @Column(name = "ValidationDate")
    public Date getValidationDate() {
        return validationDate;
    }

    public void setValidationDate(Date validationDate) {
        this.validationDate = validationDate;
    }

    @Basic
    @Column(name = "Status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Basic
    @Column(name = "CreatedDate")
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Basic
    @Column(name = "UpdatedDate")
    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemMessengerDelivery that = (ItemMessengerDelivery) o;
        return id == that.id && orderItemId == that.orderItemId && messengerId == that.messengerId && validationStatus == that.validationStatus && Objects.equals(deliveryType, that.deliveryType) && Objects.equals(merchantId, that.merchantId) && Objects.equals(wareHouseId, that.wareHouseId) && Objects.equals(customerId, that.customerId) && Objects.equals(validationCode, that.validationCode) && Objects.equals(validationDate, that.validationDate) && Objects.equals(status, that.status) && Objects.equals(createdDate, that.createdDate) && Objects.equals(updatedDate, that.updatedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderItemId, deliveryType, messengerId, merchantId, wareHouseId, customerId, validationCode, validationStatus, validationDate, status, createdDate, updatedDate);
    }
}
