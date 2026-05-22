package com.eaishipment.shipment.dto;

import java.time.LocalDateTime;

import com.eaishipment.shipment.entity.ShipmentStatus;

public class ShipmentListResponse {
    private Long id;
    private String shipmentNo;
    private String orderNo;
    private String customerName;
    private String materialName;
    private Integer quantity;
    private ShipmentStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime createdAt;

    public ShipmentListResponse(Long id, String shipmentNo, String orderNo, String customerName, String materialName,
            Integer quantity, ShipmentStatus status, LocalDateTime requestedAt, LocalDateTime createdAt) {
        this.id = id;
        this.shipmentNo = shipmentNo;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.materialName = materialName;
        this.quantity = quantity;
        this.status = status;
        this.requestedAt = requestedAt;
        this.createdAt = createdAt;
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

    public String getCustomerName() {
        return customerName;
    }

    public String getMaterialName() {
        return materialName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
