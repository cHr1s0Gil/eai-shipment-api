package com.eaishipment.failureanalysis.dto;

import java.time.LocalDateTime;

import com.eaishipment.failureanalysis.entity.FailureAnalysisStatus;

public record FailureAnalysisResponse(
        Long analysisId,
        Long shipmentId,
        String dispatchBatchId,
        int retryCount,
        String failureMessage,
        FailureAnalysisStatus status,
        String analysisResult,
        String analyzerName,
        String analysisErrorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
