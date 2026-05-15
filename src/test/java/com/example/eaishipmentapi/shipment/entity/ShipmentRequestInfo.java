package com.example.eaishipmentapi.shipment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ShipmentRequestInfo {
    @Column(name = "shipment_no")
    private String shipmentNo;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    protected ShipmentRequestInfo() {}

    public ShipmentRequestInfo(String shipmentInfo, String orderNo, LocalDateTime requestedAt) {
        this.shipmentNo = shipmentInfo;
        this.orderNo = orderNo;
        this.requestedAt = requestedAt;
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
}
