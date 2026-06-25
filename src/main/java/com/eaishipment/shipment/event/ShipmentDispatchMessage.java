package com.eaishipment.shipment.event;

public class ShipmentDispatchMessage {
    private Long shipmentId;
    private String shipmentNo;
    private String dispatchBatchId;

    public ShipmentDispatchMessage() {}

    public ShipmentDispatchMessage(Long shipmentId, String shipmentNo, String dispatchBatchId) {
        this.shipmentId = shipmentId;
        this.shipmentNo = shipmentNo;
        this.dispatchBatchId = dispatchBatchId;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public String getShipmentNo() {
        return shipmentNo;
    }

    public String getDispatchBatchId() {
        return dispatchBatchId;
    }
}
