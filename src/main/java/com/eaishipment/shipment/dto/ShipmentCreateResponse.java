package com.eaishipment.shipment.dto;

public class ShipmentCreateResponse {
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