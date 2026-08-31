package com.eaishipment.failureanalysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.eaishipment.failureanalysis.analyzer.FailureAnalysisContext;
import com.eaishipment.failureanalysis.analyzer.FailureAnalyzer;
import com.eaishipment.failureanalysis.config.OpenAIFailureAnalysisProperties;
import com.eaishipment.failureanalysis.entity.FailureAnalysisStatus;
import com.eaishipment.failureanalysis.entity.ShipmentFailureAnalysis;
import com.eaishipment.failureanalysis.repository.ShipmentFailureAnalysisRepository;
import com.eaishipment.global.exception.BusinessException;
import com.eaishipment.shipment.entity.AuditInfo;
import com.eaishipment.shipment.entity.CustomerInfo;
import com.eaishipment.shipment.entity.ShipmentItemInfo;
import com.eaishipment.shipment.entity.ShipmentRequest;
import com.eaishipment.shipment.entity.ShipmentRequestInfo;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.entity.WarehouseInfo;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;

@ExtendWith(MockitoExtension.class)
class FailureAnalysisServiceTest {

    private static final Long SHIPMENT_ID = 1L;
    private static final String DISPATCH_BATCH_ID = "BATCH-TEST-001";

    @Mock
    private ShipmentRequestRepository shipmentRequestRepository;

    @Mock
    private ShipmentFailureAnalysisRepository shipmentFailureAnalysisRepository;

    @Mock
    private FailureAnalyzer failureAnalyzer;

    private OpenAIFailureAnalysisProperties openAIProperties;
    private FailureAnalysisService failureAnalysisService;

    @BeforeEach
    void setUp() {
        openAIProperties = new OpenAIFailureAnalysisProperties();
        openAIProperties.setEnabled(true);
        openAIProperties.setModel("test-model");

        failureAnalysisService = new FailureAnalysisService(
                shipmentRequestRepository,
                shipmentFailureAnalysisRepository,
                failureAnalyzer,
                openAIProperties);
    }

    @Test
    void analyzeFailure_throwsExceptionWhenShipmentDoesNotExist() {
        when(shipmentRequestRepository.findById(SHIPMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> failureAnalysisService.analyzeFailure(SHIPMENT_ID))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(shipmentFailureAnalysisRepository, failureAnalyzer);
    }

    @Test
    void analyzeFailure_throwsExceptionWhenShipmentIsNotFailed() {
        ShipmentRequest shipmentRequest = createShipment(ShipmentStatus.RECEIVED, DISPATCH_BATCH_ID);
        when(shipmentRequestRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(shipmentRequest));

        assertThatThrownBy(() -> failureAnalysisService.analyzeFailure(SHIPMENT_ID))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(shipmentFailureAnalysisRepository, failureAnalyzer);
    }

    @Test
    void analyzeFailure_throwsExceptionBeforeSavingWhenFeatureIsDisabled() {
        ShipmentRequest shipmentRequest = createShipment(ShipmentStatus.FAILED, DISPATCH_BATCH_ID);
        openAIProperties.setEnabled(false);
        when(shipmentRequestRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(shipmentRequest));

        assertThatThrownBy(() -> failureAnalysisService.analyzeFailure(SHIPMENT_ID))
                .isInstanceOf(BusinessException.class);

        verify(shipmentFailureAnalysisRepository, never()).save(any());
        verifyNoInteractions(failureAnalyzer);
    }

    @Test
    void analyzeFailure_throwsExceptionWhenDispatchBatchIdIsMissing() {
        ShipmentRequest shipmentRequest = createShipment(ShipmentStatus.FAILED, null);
        when(shipmentRequestRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(shipmentRequest));

        assertThatThrownBy(() -> failureAnalysisService.analyzeFailure(SHIPMENT_ID))
                .isInstanceOf(BusinessException.class);

        verify(shipmentFailureAnalysisRepository, never()).save(any());
        verifyNoInteractions(failureAnalyzer);
    }

    @Test
    void analyzeFailure_returnsExistingAnalysisWithoutCallingAnalyzer() {
        ShipmentRequest shipmentRequest = createShipment(ShipmentStatus.FAILED, DISPATCH_BATCH_ID);
        ShipmentFailureAnalysis existingAnalysis = createAnalysis(shipmentRequest);
        existingAnalysis.completeAnalysis("existing result");

        when(shipmentRequestRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(shipmentRequest));
        when(shipmentFailureAnalysisRepository.findByShipmentRequest_IdAndDispatchBatchId(
                SHIPMENT_ID,
                DISPATCH_BATCH_ID)).thenReturn(Optional.of(existingAnalysis));

        ShipmentFailureAnalysis result = failureAnalysisService.analyzeFailure(SHIPMENT_ID);

        assertThat(result).isSameAs(existingAnalysis);
        assertThat(result.getAnalysisResult()).isEqualTo("existing result");
        verify(shipmentFailureAnalysisRepository, never()).save(any());
        verifyNoInteractions(failureAnalyzer);
    }

    @Test
    void analyzeFailure_savesCompleteAnalysisWhenAnalyzerSucceeds() {
        ShipmentRequest shipmentRequest = createShipment(ShipmentStatus.FAILED, DISPATCH_BATCH_ID);
        String analyzerResult = "{\"summary\":\"duplicate shipment\"}";

        mockNewAnalysis(shipmentRequest);
        when(failureAnalyzer.getName()).thenReturn("TestAnalyzer");
        when(failureAnalyzer.analyze(any(FailureAnalysisContext.class))).thenReturn(analyzerResult);

        ShipmentFailureAnalysis result = failureAnalysisService.analyzeFailure(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(FailureAnalysisStatus.COMPLETE);
        assertThat(result.getAnalysisResult()).isEqualTo(analyzerResult);
        assertThat(result.getAnalysisErrorMessage()).isNull();
        assertThat(result.getAnalyzerName()).isEqualTo("TestAnalyzer");
        verify(shipmentFailureAnalysisRepository, times(2)).save(any());

        ArgumentCaptor<FailureAnalysisContext> contextCaptor = ArgumentCaptor.forClass(FailureAnalysisContext.class);
        verify(failureAnalyzer).analyze(contextCaptor.capture());
        FailureAnalysisContext context = contextCaptor.getValue();
        assertThat(context.shipmentId()).isEqualTo(SHIPMENT_ID);
        assertThat(context.shipmentNo()).isEqualTo("SHP-TEST-001");
        assertThat(context.status()).isEqualTo(ShipmentStatus.FAILED);
        assertThat(context.failureMessage()).isEqualTo("WMS transmission failed");
        assertThat(context.errorPayload()).contains("DUPLICATE_KEY");
        assertThat(context.dispatchBatchId()).isEqualTo(DISPATCH_BATCH_ID);
    }

    @Test
    void analyzeFailure_savesFailedAnalysisWhenAnalyzerThrowsException() {
        ShipmentRequest shipmentRequest = createShipment(ShipmentStatus.FAILED, DISPATCH_BATCH_ID);

        mockNewAnalysis(shipmentRequest);
        when(failureAnalyzer.getName()).thenReturn("TestAnalyzer");
        when(failureAnalyzer.analyze(any(FailureAnalysisContext.class)))
                .thenThrow(new IllegalStateException("OpenAI unavailable"));

        ShipmentFailureAnalysis result = failureAnalysisService.analyzeFailure(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(FailureAnalysisStatus.FAILED);
        assertThat(result.getAnalysisResult()).isNull();
        assertThat(result.getAnalysisErrorMessage()).isEqualTo("OpenAI unavailable");
        verify(shipmentFailureAnalysisRepository, times(2)).save(any());
    }

    @Test
    void analyzeFailure_savesFailedAnalysisWhenAnalyzerReturnsBlankResponse() {
        ShipmentRequest shipmentRequest = createShipment(ShipmentStatus.FAILED, DISPATCH_BATCH_ID);

        mockNewAnalysis(shipmentRequest);
        when(failureAnalyzer.getName()).thenReturn("TestAnalyzer");
        when(failureAnalyzer.analyze(any(FailureAnalysisContext.class))).thenReturn("   ");

        ShipmentFailureAnalysis result = failureAnalysisService.analyzeFailure(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(FailureAnalysisStatus.FAILED);
        assertThat(result.getAnalysisResult()).isNull();
        assertThat(result.getAnalysisErrorMessage()).isEqualTo("Analyzer returned an empty response.");
        verify(shipmentFailureAnalysisRepository, times(2)).save(any());
    }

    private void mockNewAnalysis(ShipmentRequest shipmentRequest) {
        when(shipmentRequestRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(shipmentRequest));
        when(shipmentFailureAnalysisRepository.findByShipmentRequest_IdAndDispatchBatchId(
                SHIPMENT_ID,
                DISPATCH_BATCH_ID)).thenReturn(Optional.empty());
        when(shipmentFailureAnalysisRepository.save(any(ShipmentFailureAnalysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ShipmentFailureAnalysis createAnalysis(ShipmentRequest shipmentRequest) {
        return new ShipmentFailureAnalysis(
                shipmentRequest,
                DISPATCH_BATCH_ID,
                shipmentRequest.getProcessingInfo().getRetryCount(),
                shipmentRequest.getProcessingInfo().getMessage(),
                shipmentRequest.getProcessingInfo().getErrorPayload(),
                "TestAnalyzer");
    }

    private ShipmentRequest createShipment(ShipmentStatus status, String dispatchBatchId) {
        ShipmentRequest shipmentRequest = new ShipmentRequest(
                new ShipmentRequestInfo(
                        "SHP-TEST-001",
                        "ORD-TEST-001",
                        LocalDateTime.of(2026, 8, 29, 9, 0)),
                new WarehouseInfo("WH-TEST-01"),
                new CustomerInfo("CUST-001", "Test Customer"),
                new ShipmentItemInfo("MAT-001", "Test Material", 1, "EA"));

        ReflectionTestUtils.setField(shipmentRequest, "id", SHIPMENT_ID);
        ReflectionTestUtils.setField(shipmentRequest, "auditInfo", AuditInfo.createNow());

        if (dispatchBatchId != null) {
            shipmentRequest.updateDispatchBatchId(dispatchBatchId);
        }

        if (status != ShipmentStatus.RECEIVED) {
            shipmentRequest.updateStatus(
                    status,
                    "WMS transmission failed",
                    "{\"code\":\"DUPLICATE_KEY\"}");
        }

        return shipmentRequest;
    }
}
