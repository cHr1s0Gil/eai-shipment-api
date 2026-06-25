package com.eaishipment.shipment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class ShipmentProcessingInfo {
    @Column(name = "dispatch_batch_id")
    private String dispatchBatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ShipmentStatus status;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "message")
    private String message;

    @Column(name = "error_payload", columnDefinition = "CLOB")
    private String errorPayload;

    protected ShipmentProcessingInfo() {}

    public ShipmentProcessingInfo(String dispatchBatchId, ShipmentStatus status, int retryCount, String message) {
        this.dispatchBatchId = dispatchBatchId;
        this.status = status;
        this.retryCount = retryCount;
        this.message = message;
    }

    public static ShipmentProcessingInfo received() {
        return new ShipmentProcessingInfo(null, ShipmentStatus.RECEIVED, 0, null);
    }

    public void updateDispatchBatchId(String dispatchBatchId) {
        this.dispatchBatchId = dispatchBatchId;
    }

    public void updateStatus(ShipmentStatus status, String message) {
        updateStatus(status, message, null);
    }

    public void updateStatus(ShipmentStatus status, String message, String errorPayload) {
        this.status = status;

        if (status == ShipmentStatus.FAILED) {
            this.message = message;
            this.errorPayload = errorPayload;
            return;
        }

        this.message = null;
        this.errorPayload = null;
    }

    public void retrySuccess() {
        this.retryCount++;
        this.status = ShipmentStatus.SUCCESS;
        this.message = null;
        this.errorPayload = null;
    }

    public String getDispatchBatchId() {
        return dispatchBatchId;
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

    public String getErrorPayload() {
        return errorPayload;
    }

}
