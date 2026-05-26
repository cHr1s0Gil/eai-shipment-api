package com.eaishipment.shipment.dto;

import com.eaishipment.shipment.entity.ShipmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for updating shipment status")
public class ShipmentStatusUpdateRequest {
    @Schema(description = "Target shipment status", example = "FAILED", allowableValues = {
            "RECEIVED", "PROCESSING", "SUCCESS", "FAILED"
    })
    @NotNull(message = "status is required.")
    private ShipmentStatus status;

    @Schema(description = "Failure reason. Required only when status is FAILED.", example = "WMS stock shortage")
    private String message;

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
