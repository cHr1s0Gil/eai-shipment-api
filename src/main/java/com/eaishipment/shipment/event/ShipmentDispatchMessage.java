package com.eaishipment.shipment.event;

public class ShipmentDispatchMessage {
    private Long shipmentId;
    private String shipmentNo;

    public ShipmentDispatchMessage() {}

    public ShipmentDispatchMessage(Long shipmentId, String shipmentNo) {
        this.shipmentId = shipmentId;
        this.shipmentNo = shipmentNo;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public String getShipmentNo() {
        return shipmentNo;
    }
}
