package com.eaishipment.failureanalysis.analyzer;

import java.time.LocalDateTime;

import com.eaishipment.shipment.entity.ShipmentStatus;

public record FailureAnalysisContext(
    Long shipmentId,
    String shipmentNo,
    ShipmentStatus status,
    String failureMessage,
    String errorPayload,
    int retryCount,
    String dispatchBatchId,
    LocalDateTime lastUpdatedAt
) {
}
