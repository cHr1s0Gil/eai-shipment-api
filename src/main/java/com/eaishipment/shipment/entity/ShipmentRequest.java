package com.eaishipment.shipment.entity;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "shipment_request")
public class ShipmentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private ShipmentRequestInfo requestInfo;

    @Embedded
    private WarehouseInfo warehouseInfo;

    @Embedded
    private CustomerInfo customerInfo; 

    @Embedded
    private ShipmentItemInfo itemInfo;

    @Embedded
    private ShipmentProcessingInfo processingInfo;

    @Embedded
    private AuditInfo auditInfo;

    protected ShipmentRequest() {}

    public ShipmentRequest(
        ShipmentRequestInfo requestInfo, 
        WarehouseInfo warehouseInfo, 
        CustomerInfo customerInfo,
        ShipmentItemInfo itemInfo
    ) {
        this.requestInfo = requestInfo;
        this.warehouseInfo = warehouseInfo;
        this.customerInfo = customerInfo;
        this.itemInfo = itemInfo;
        this.processingInfo = ShipmentProcessingInfo.received();
    }

    @PrePersist
    private void prePersist() {
        this.auditInfo = AuditInfo.createNow();
    }

    @PreUpdate
    private void preUpdate() {
        this.auditInfo.update();
    }

    public void updateStatus(ShipmentStatus status, String errorMessage) {
        this.processingInfo.updateStatus(status, errorMessage);
    }

    public void retrySuccess() {
        this.processingInfo.retrySuccess();
    }

    public Long getId() {
        return id;
    }

    public ShipmentRequestInfo getRequestInfo() {
        return requestInfo;
    }

    public WarehouseInfo getWarehouseInfo() {
        return warehouseInfo;
    }

    public CustomerInfo getCustomerInfo() {
        return customerInfo;
    }

    public ShipmentItemInfo getItemInfo() {
        return itemInfo;
    }

    public ShipmentProcessingInfo getProcessingInfo() {
        return processingInfo;
    }

    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

}
