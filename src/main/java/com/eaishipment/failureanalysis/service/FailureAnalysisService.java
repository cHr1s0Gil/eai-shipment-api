package com.eaishipment.failureanalysis.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.eaishipment.failureanalysis.analyzer.FailureAnalysisContext;
import com.eaishipment.failureanalysis.analyzer.FailureAnalyzer;
import com.eaishipment.failureanalysis.config.OpenAIFailureAnalysisProperties;
import com.eaishipment.failureanalysis.entity.ShipmentFailureAnalysis;
import com.eaishipment.failureanalysis.repository.ShipmentFailureAnalysisRepository;
import com.eaishipment.global.exception.BusinessException;
import com.eaishipment.shipment.entity.ShipmentRequest;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;

@Service
public class FailureAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(FailureAnalysisService.class);

    private final ShipmentRequestRepository shipmentRequestRepository;
    private final ShipmentFailureAnalysisRepository shipmentFailureAnalysisRepository;
    private final FailureAnalyzer failureAnalyzer;
    private final OpenAIFailureAnalysisProperties openAIProperties;

    public FailureAnalysisService(
            ShipmentRequestRepository shipmentRequestRepository,
            ShipmentFailureAnalysisRepository shipmentFailureAnalysisRepository,
            FailureAnalyzer failureAnalyzer,
            OpenAIFailureAnalysisProperties openAIProperties) {

        this.shipmentRequestRepository = shipmentRequestRepository;
        this.shipmentFailureAnalysisRepository = shipmentFailureAnalysisRepository;
        this.failureAnalyzer = failureAnalyzer;
        this.openAIProperties = openAIProperties;
    }

    public ShipmentFailureAnalysis analyzeFailure(Long shipmentId) {
        ShipmentRequest shipmentRequest = shipmentRequestRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException("출고 지시를 찾을 수 없습니다."));

        if (shipmentRequest.getProcessingInfo().getStatus() != ShipmentStatus.FAILED) {
            throw new BusinessException("FAILED 상태의 출고지시만 장애 분석할 수 있습니다.");
        }

        if (!openAIProperties.isEnabled()) {
            throw new BusinessException("OpenAI 장애 분석 기능이 비활성화되어 있습니다.");
        }

        String dispatchBatchId = shipmentRequest.getProcessingInfo().getDispatchBatchId();
        if (!StringUtils.hasText(dispatchBatchId)) {
            throw new BusinessException("Batch ID가 없어 장애 분석을 실행할 수 없습니다.");
        }

        Optional<ShipmentFailureAnalysis> existingAnalysis = shipmentFailureAnalysisRepository
                .findByShipmentRequest_IdAndDispatchBatchId(shipmentId, dispatchBatchId);

        if (existingAnalysis.isPresent()) {
            return existingAnalysis.get();
        }

        FailureAnalysisContext context = new FailureAnalysisContext(
                shipmentRequest.getId(),
                shipmentRequest.getRequestInfo().getShipmentNo(),
                shipmentRequest.getProcessingInfo().getStatus(),
                shipmentRequest.getProcessingInfo().getMessage(),
                shipmentRequest.getProcessingInfo().getErrorPayload(),
                shipmentRequest.getProcessingInfo().getRetryCount(),
                dispatchBatchId,
                shipmentRequest.getAuditInfo().getUpdatedAt());

        ShipmentFailureAnalysis analysis = new ShipmentFailureAnalysis(
                shipmentRequest,
                dispatchBatchId,
                shipmentRequest.getProcessingInfo().getRetryCount(),
                shipmentRequest.getProcessingInfo().getMessage(),
                shipmentRequest.getProcessingInfo().getErrorPayload(),
                failureAnalyzer.getName());

        analysis = shipmentFailureAnalysisRepository.save(analysis);

        try {
            String result = failureAnalyzer.analyze(context);

            if (!StringUtils.hasText(result)) {
                throw new IllegalStateException("Analyzer returned an empty response.");
            }

            analysis.completeAnalysis(result);

        } catch (Exception e) {
            String errorMessage = null;
            if (StringUtils.hasText(e.getMessage())) {
                errorMessage = e.getMessage();
            } else {
                errorMessage = e.getClass().getSimpleName();
            }

            analysis.failAnalysis(errorMessage);

            log.error(
                    "Failure analysis failed. shipmentId={}, dispatchBatchId={}, analyzer={}",
                    shipmentId,
                    dispatchBatchId,
                    failureAnalyzer.getName(),
                    e);
        }

        return shipmentFailureAnalysisRepository.save(analysis);
    }
}
