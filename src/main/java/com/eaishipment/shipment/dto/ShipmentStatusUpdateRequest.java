package com.eaishipment.shipment.dto;

import com.eaishipment.shipment.entity.ShipmentStatus;

import jakarta.validation.constraints.NotNull;

public class ShipmentStatusUpdateRequest {
    @NotNull(message = "상태값은 필수입니다.")
    private ShipmentStatus status;
    private String message;

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
