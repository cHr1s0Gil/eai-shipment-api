package com.eaishipment.shipment.dto;

import com.eaishipment.shipment.entity.ShipmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response body after updating shipment status")
public class ShipmentStatusUpdateResponse {
    @Schema(description = "Shipment request number", example = "SHP-20260522-001")
    private String shipmentNo;

    @Schema(description = "Updated shipment status", example = "FAILED")
    private ShipmentStatus status;

    @Schema(description = "Status message or failure reason", example = "WMS stock shortage")
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
