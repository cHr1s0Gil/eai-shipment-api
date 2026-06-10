package com.eaishipment.shipment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.eaishipment.shipment.dto.ShipmentCreateRequest;
import com.eaishipment.shipment.producer.ShipmentDispatchProducer;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;
import com.eaishipment.shipment.service.ShipmentRequestService;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShipmentRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentRequestService shipmentRequestService;

    @Autowired
    private ShipmentRequestRepository shipmentRequestRepository;

    @MockitoBean
    private ShipmentDispatchProducer shipmentDispatchProducer;

    @BeforeEach
    void setUp() {
        shipmentRequestRepository.deleteAll();
    }

    @Test
    void createShipment_returnsCreatedResponse() throws Exception {
        mockMvc.perform(post("/api/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "shipmentNo": "SHP-API-001",
                          "orderNo": "ORD-API-001",
                          "requestedAt": "2026-05-22T09:00:00",
                          "warehouseCode": "WH-SEOUL-01",
                          "customerCode": "CUST-001",
                          "customerName": "Test Customer",
                          "materialCode": "MAT-001",
                          "materialName": "Test Material",
                          "quantity": 10,
                          "unit": "EA"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("S"))
                .andExpect(jsonPath("$.data.shipmentNo").value("SHP-API-001"));
    }

    @Test
    void getShipmentList_returnsSavedShipments() throws Exception {
        saveShipmentAndCommit("SHP-API-002");

        mockMvc.perform(get("/api/shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("S"))
                .andExpect(jsonPath("$.data[0].shipmentNo").value("SHP-API-002"));
    }

    @Test
    void getShipmentByInvalidStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/shipments/status/INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("E"));
    }

    @Test
    void updateStatusToFailedWithoutMessage_returnsBadRequest() throws Exception {
        Long id = saveShipmentAndCommit("SHP-API-003");

        mockMvc.perform(patch("/api/shipments/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "status": "FAILED"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("E"));
    }

    @Test
    void retryNonFailedShipment_returnsBadRequest() throws Exception {
        Long id = saveShipmentAndCommit("SHP-API-004");

        mockMvc.perform(post("/api/shipments/{id}/retry", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("E"));
    }

    @Test
    void dispatchShipment_returnsSuccessResponse() throws Exception {
        Long id = saveShipmentAndCommit("SHP-API-005");

        mockMvc.perform(post("/api/shipments/{id}/dispatch", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("S"))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    void dispatchShipment_returnsProcessingStatusWhenShipmentNoContainsFail() throws Exception {
        Long id = saveShipmentAndCommit("SHP-FAIL-006");

        mockMvc.perform(post("/api/shipments/{id}/dispatch", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("S"))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.message").isEmpty());
    }

    @Test
    void dispatchShipment_returnsBadRequestWhenShipmentIsNotReceived() throws Exception {
        Long id = saveShipmentAndCommit("SHP-API-007");
        shipmentRequestService.dispatchShipment(id);

        mockMvc.perform(post("/api/shipments/{id}/dispatch", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("E"));
    }

    private Long saveShipmentAndCommit(String shipmentNo) {
        shipmentRequestService.createShipment(createRequest(shipmentNo));

        return shipmentRequestRepository
                .findAll()
                .stream()
                .filter(shipment -> shipment.getRequestInfo().getShipmentNo().equals(shipmentNo))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private ShipmentCreateRequest createRequest(String shipmentNo) {
        ShipmentCreateRequest request = new ShipmentCreateRequest();
        ReflectionTestUtils.setField(request, "shipmentNo", shipmentNo);
        ReflectionTestUtils.setField(request, "orderNo", "ORD-" + shipmentNo);
        ReflectionTestUtils.setField(request, "requestedAt", LocalDateTime.of(2026, 5, 22, 9, 0));
        ReflectionTestUtils.setField(request, "warehouseCode", "WH-SEOUL-01");
        ReflectionTestUtils.setField(request, "customerCode", "CUST-001");
        ReflectionTestUtils.setField(request, "customerName", "Test Customer");
        ReflectionTestUtils.setField(request, "materialCode", "MAT-001");
        ReflectionTestUtils.setField(request, "materialName", "Test Material");
        ReflectionTestUtils.setField(request, "quantity", 10);
        ReflectionTestUtils.setField(request, "unit", "EA");
        return request;
    }
}
