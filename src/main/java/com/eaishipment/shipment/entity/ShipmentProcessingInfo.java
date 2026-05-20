package com.eaishipment.shipment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class ShipmentProcessingInfo {
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ShipmentStatus status;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "error_message")
    private String errorMessage;

    protected ShipmentProcessingInfo() {}

    public ShipmentProcessingInfo(ShipmentStatus status, int retryCount, String errorMessage) {
        this.status = status;
        this.retryCount = retryCount;
        this.errorMessage = errorMessage;
    }

    public static ShipmentProcessingInfo received() {
        return new ShipmentProcessingInfo(ShipmentStatus.RECEIVED, 0, null);
    }

    public void updateStatus(ShipmentStatus status, String errorMessage) {
        this.status = status;
        this.errorMessage = status == ShipmentStatus.FAILED ? errorMessage : null;
    }

    public void retrySuccess() {
        this.retryCount++;
        this.status = ShipmentStatus.SUCCESS;
        this.errorMessage = null;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}
