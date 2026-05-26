package com.eaishipment.shipment.dto;

import com.eaishipment.shipment.entity.ShipmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response body after retrying a failed shipment request")
public class ShipmentRetryResponse {
    @Schema(description = "Shipment request number", example = "SHP-20260522-001")
    private String shipmentNo;

    @Schema(description = "Shipment status after retry", example = "SUCCESS")
    private ShipmentStatus status;

    @Schema(description = "Retry count after retry processing", example = "1")
    private int retryCount;

    @Schema(description = "Status message after retry", nullable = true)
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
