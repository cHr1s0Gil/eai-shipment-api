package com.eaishipment.failureanalysis.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Failure analysis status")
public enum FailureAnalysisStatus {
    PENDING("Analysis pending"),
    COMPLETE("Analysis completed"),
    FAILED("Analysis failed");

    private final String description;

    FailureAnalysisStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
