package com.eaishipment.failureanalysis.mapper;

import com.eaishipment.failureanalysis.dto.FailureAnalysisResponse;
import com.eaishipment.failureanalysis.entity.ShipmentFailureAnalysis;

public class FailureAnalysisMapper {
    private FailureAnalysisMapper() {}

    public static FailureAnalysisResponse toResponse(ShipmentFailureAnalysis analysis) {
        return new FailureAnalysisResponse(
                analysis.getId(),
                analysis.getShipmentRequest().getId(),
                analysis.getDispatchBatchId(),
                analysis.getRetryCount(),
                analysis.getFailureMessage(),
                analysis.getStatus(),
                analysis.getAnalysisResult(),
                analysis.getAnalyzerName(),
                analysis.getAnalysisErrorMessage(),
                analysis.getAuditInfo().getCreatedAt(),
                analysis.getAuditInfo().getUpdatedAt());
    }
}
