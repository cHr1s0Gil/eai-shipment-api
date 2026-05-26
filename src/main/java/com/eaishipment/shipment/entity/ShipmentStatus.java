package com.eaishipment.shipment.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Shipment processing status")
public enum ShipmentStatus {
    RECEIVED("Shipment request received"),
    PROCESSING("Processing"),
    SUCCESS("Processed successfully"),
    FAILED("Processing failed");

    private final String description;

    ShipmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
