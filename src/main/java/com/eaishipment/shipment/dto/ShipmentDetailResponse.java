package com.eaishipment.shipment.dto;

import java.time.LocalDateTime;

import com.eaishipment.shipment.entity.ShipmentStatus;

public class ShipmentDetailResponse {
    private Long id;
    private String shipmentNo;
    private String orderNo;
    private String dispatchBatchId;
    private LocalDateTime requestedAt;
    private String warehouseCode;
    private String customerCode;
    private String customerName;
    private String materialCode;
    private String materialName;
    private Integer quantity;
    private String unit;
    private ShipmentStatus status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int retryCount;

    public ShipmentDetailResponse(Long id, String shipmentNo, String orderNo, String dispatchBatchId, LocalDateTime requestedAt, String warehouseCode,
            String customerCode, String customerName, String materialCode, String materialName, Integer quantity,
            String unit, ShipmentStatus status, String message, LocalDateTime createdAt, LocalDateTime updatedAt, int retryCount) {
        this.id = id;
        this.shipmentNo = shipmentNo;
        this.orderNo = orderNo;
        this.dispatchBatchId = dispatchBatchId;
        this.requestedAt = requestedAt;
        this.warehouseCode = warehouseCode;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.quantity = quantity;
        this.unit = unit;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.retryCount = retryCount;
    }

    public Long getId() {
        return id;
    }

    public String getShipmentNo() {
        return shipmentNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getDispatchBatchId() {
        return dispatchBatchId;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
