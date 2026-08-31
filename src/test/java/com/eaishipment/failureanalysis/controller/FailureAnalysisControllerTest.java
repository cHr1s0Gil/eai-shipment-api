package com.eaishipment.failureanalysis.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.eaishipment.failureanalysis.analyzer.FailureAnalysisContext;
import com.eaishipment.failureanalysis.analyzer.FailureAnalyzer;
import com.eaishipment.failureanalysis.repository.ShipmentFailureAnalysisRepository;
import com.eaishipment.shipment.dto.ShipmentCreateRequest;
import com.eaishipment.shipment.entity.ShipmentRequest;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;
import com.eaishipment.shipment.service.ShipmentRequestService;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "analysis.openai.enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FailureAnalysisControllerTest {

    private static final String API_KEY_HEADER = "x-api-key";
    private static final String API_KEY_VALUE = "api-key-test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentRequestService shipmentRequestService;

    @Autowired
    private ShipmentRequestRepository shipmentRequestRepository;

    @Autowired
    private ShipmentFailureAnalysisRepository shipmentFailureAnalysisRepository;

    @MockitoBean
    private FailureAnalyzer failureAnalyzer;

    @BeforeEach
    void setUp() {
        shipmentFailureAnalysisRepository.deleteAll();
        shipmentRequestRepository.deleteAll();
    }

    @Test
    void analyzeFailure_returnsCompleteAnalysisForFailedShipment() throws Exception {
        Long shipmentId = saveShipment("SHP-ANALYSIS-001", ShipmentStatus.FAILED);
        when(failureAnalyzer.getName()).thenReturn("TestAnalyzer");
        when(failureAnalyzer.analyze(any(FailureAnalysisContext.class)))
                .thenReturn("{\"summary\":\"duplicate shipment\"}");

        mockMvc.perform(post("/api/analyses/shipments/{shipmentId}/failure", shipmentId)
                .header(API_KEY_HEADER, API_KEY_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("S"))
                .andExpect(jsonPath("$.data.shipmentId").value(shipmentId))
                .andExpect(jsonPath("$.data.dispatchBatchId").value("BATCH-SHP-ANALYSIS-001"))
                .andExpect(jsonPath("$.data.status").value("COMPLETE"))
                .andExpect(jsonPath("$.data.analyzerName").value("TestAnalyzer"))
                .andExpect(jsonPath("$.data.analysisResult").value("{\"summary\":\"duplicate shipment\"}"));
    }

    @Test
    void analyzeFailure_returnsBadRequestForNonFailedShipment() throws Exception {
        Long shipmentId = saveShipment("SHP-ANALYSIS-002", ShipmentStatus.RECEIVED);

        mockMvc.perform(post("/api/analyses/shipments/{shipmentId}/failure", shipmentId)
                .header(API_KEY_HEADER, API_KEY_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("E"));

        verifyNoInteractions(failureAnalyzer);
    }

    @Test
    void analyzeFailure_returnsUnauthorizedWithoutApiKey() throws Exception {
        Long shipmentId = saveShipment("SHP-ANALYSIS-003", ShipmentStatus.FAILED);

        mockMvc.perform(post("/api/analyses/shipments/{shipmentId}/failure", shipmentId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("E"))
                .andExpect(jsonPath("$.message").value("Invalid API key"));

        verifyNoInteractions(failureAnalyzer);
    }

    private Long saveShipment(String shipmentNo, ShipmentStatus status) {
        shipmentRequestService.createShipment(createRequest(shipmentNo));

        ShipmentRequest shipmentRequest = shipmentRequestRepository.findAll()
                .stream()
                .filter(shipment -> shipment.getRequestInfo().getShipmentNo().equals(shipmentNo))
                .findFirst()
                .orElseThrow();

        if (status == ShipmentStatus.FAILED) {
            shipmentRequest.updateDispatchBatchId("BATCH-" + shipmentNo);
            shipmentRequest.updateStatus(
                    ShipmentStatus.FAILED,
                    "Duplicate shipment key rejected by WMS",
                    "{\"code\":\"DUPLICATE_KEY\"}");
            shipmentRequestRepository.saveAndFlush(shipmentRequest);
        }

        return shipmentRequest.getId();
    }

    private ShipmentCreateRequest createRequest(String shipmentNo) {
        ShipmentCreateRequest request = new ShipmentCreateRequest();
        ReflectionTestUtils.setField(request, "shipmentNo", shipmentNo);
        ReflectionTestUtils.setField(request, "orderNo", "ORD-" + shipmentNo);
        ReflectionTestUtils.setField(request, "requestedAt", LocalDateTime.of(2026, 8, 29, 9, 0));
        ReflectionTestUtils.setField(request, "warehouseCode", "WH-TEST-01");
        ReflectionTestUtils.setField(request, "customerCode", "CUST-001");
        ReflectionTestUtils.setField(request, "customerName", "Test Customer");
        ReflectionTestUtils.setField(request, "materialCode", "MAT-001");
        ReflectionTestUtils.setField(request, "materialName", "Test Material");
        ReflectionTestUtils.setField(request, "quantity", 1);
        ReflectionTestUtils.setField(request, "unit", "EA");
        return request;
    }
}
