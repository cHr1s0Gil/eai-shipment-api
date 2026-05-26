package com.eaishipment.shipment.dto;

import com.eaishipment.shipment.entity.ShipmentStatus;

public class ShipmentStatusUpdateResponse {
    private String shipmentNo;
    private ShipmentStatus status;
    private String message;

    public ShipmentStatusUpdateResponse(String shipmentNo, ShipmentStatus status, String message) {
        this.shipmentNo = shipmentNo;
        this.status = status;
        this.message = message;
    }

    public String getShipmentNo() {
        return shipmentNo;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
