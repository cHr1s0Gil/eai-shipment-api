package com.eaishipment.shipment.dto;

public class ShipmentSchedulerStatusResponse {
    private boolean enabled;

    public ShipmentSchedulerStatusResponse(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
