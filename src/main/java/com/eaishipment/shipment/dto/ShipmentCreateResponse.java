package com.eaishipment.shipment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response body after creating a shipment request")
public class ShipmentCreateResponse {
    @Schema(description = "Created shipment request number", example = "SHP-20260522-001")
    private final String shipmentNo;

    private ShipmentCreateResponse(String shipmentNo) {
        this.shipmentNo = shipmentNo;
    }

    public static ShipmentCreateResponse success(String shipmentNo) {
        return new ShipmentCreateResponse(shipmentNo);
    }

    public String getShipmentNo() {
        return shipmentNo;
    }
}
