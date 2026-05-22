package com.eaishipment.shipment.dto;

import java.time.LocalDateTime;

import com.eaishipment.shipment.entity.ShipmentStatus;

public class ShipmentDetailResponse {
    private Long id;
    private String shipmentNo;
    private String orderNo;
    private LocalDateTime requestedAt;
    private String warehouseCode;
    private String customerCode;
    private String customerName;
    private String materialCode;
    private String materialName;
    private Integer quantity;
    private String unit;
    private ShipmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    

    public ShipmentDetailResponse(Long id, String shipmentNo, String orderNo, LocalDateTime requestedAt, String warehouseCode,
            String customerCode, String customerName, String materialCode, String materialName, Integer quantity,
            String unit, ShipmentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.shipmentNo = shipmentNo;
        this.orderNo = orderNo;
        this.requestedAt = requestedAt;
        this.warehouseCode = warehouseCode;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.quantity = quantity;
        this.unit = unit;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
