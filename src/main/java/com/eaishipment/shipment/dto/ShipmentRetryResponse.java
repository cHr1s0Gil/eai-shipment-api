package com.eaishipment.shipment.dto;

import com.eaishipment.shipment.entity.ShipmentStatus;

public class ShipmentRetryResponse {
    private String shipmentNo;
    private ShipmentStatus status;
    private int retryCount;
    private String message;
    
    public ShipmentRetryResponse(String shipmentNo, ShipmentStatus status, int retryCount, String message) {
        this.shipmentNo = shipmentNo;
        this.status = status;
        this.retryCount = retryCount;
        this.message = message;
    }

    public String getShipmentNo() {
        return shipmentNo;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getMessage() {
        return message;
    }
    
}
