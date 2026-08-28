package com.eaishipment.failureanalysis.entity;

import com.eaishipment.shipment.entity.AuditInfo;
import com.eaishipment.shipment.entity.ShipmentRequest;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "shipment_failure_analysis", uniqueConstraints = {
        @UniqueConstraint(name = "uk_failure_analysis_shipment_batch", columnNames = { "shipment_id",
                "dispatch_batch_id" })
})
public class ShipmentFailureAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private ShipmentRequest shipmentRequest;

    @Column(name = "dispatch_batch_id", nullable = false)
    private String dispatchBatchId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failure_message")
    private String failureMessage;

    @Lob
    @Column(name = "error_payload_snapshot")
    private String errorPayloadSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false)
    private FailureAnalysisStatus status = FailureAnalysisStatus.PENDING;

    @Lob
    @Column(name = "analysis_result")
    private String analysisResult;

    @Column(name = "analyzer_name", nullable = false)
    private String analyzerName;

    @Column(name = "analysis_error_message")
    private String analysisErrorMessage;

    @Embedded
    private AuditInfo auditInfo;

    protected ShipmentFailureAnalysis() {
    }

    public ShipmentFailureAnalysis(
            ShipmentRequest shipmentRequest,
            String dispatchBatchId,
            int retryCount,
            String failureMessage,
            String errorPayloadSnapshot,
            String analyzerName) {
        this.shipmentRequest = shipmentRequest;
        this.dispatchBatchId = dispatchBatchId;
        this.retryCount = retryCount;
        this.failureMessage = failureMessage;
        this.errorPayloadSnapshot = errorPayloadSnapshot;
        this.analyzerName = analyzerName;
    }

    public void completeAnalysis(String analysisResult) {
        this.status = FailureAnalysisStatus.COMPLETE;
        this.analysisResult = analysisResult;
        this.analysisErrorMessage = null;
    }

    public void failAnalysis(String analysisErrorMessage) {
        this.status = FailureAnalysisStatus.FAILED;
        this.analysisErrorMessage = analysisErrorMessage;
        this.analysisResult = null;
    }

    @PrePersist
    public void prePersist() {
        this.auditInfo = AuditInfo.createNow();
    }

    @PreUpdate
    public void preUpdate() {
        this.auditInfo.update();
    }

    public Long getId() {
        return id;
    }

    public ShipmentRequest getShipmentRequest() {
        return shipmentRequest;
    }

    public String getDispatchBatchId() {
        return dispatchBatchId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public String getErrorPayloadSnapshot() {
        return errorPayloadSnapshot;
    }

    public FailureAnalysisStatus getStatus() {
        return status;
    }

    public String getAnalysisResult() {
        return analysisResult;
    }

    public String getAnalyzerName() {
        return analyzerName;
    }

    public String getAnalysisErrorMessage() {
        return analysisErrorMessage;
    }

    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

}
