package com.eaishipment.shipment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.eaishipment.shipment.dto.ShipmentCreateRequest;
import com.eaishipment.shipment.dto.ShipmentDetailResponse;
import com.eaishipment.shipment.entity.ShipmentRequest;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.producer.ShipmentDispatchProducer;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("test")
class ShipmentDispatchResultServiceTest {

    @Autowired
    private ShipmentRequestService shipmentRequestService;

    @Autowired
    private ShipmentDispatchService shipmentDispatchService;

    @Autowired
    private ShipmentDispatchResultService shipmentDispatchResultService;

    @Autowired
    private ShipmentRequestRepository shipmentRequestRepository;

    @MockitoBean
    private ShipmentDispatchProducer shipmentDispatchProducer;

    @BeforeEach
    void setUp() {
        shipmentRequestRepository.deleteAll();
    }

    @Test
    void completeDispatch_changesProcessingShipmentToSuccess() {
        Long id = saveShipmentAndGetId("SHP-RESULT-001");
        String payload = "{\"shipmentId\":" + id + ",\"shipmentNo\":\"SHP-RESULT-001\"}";

        shipmentDispatchService.dispatchShipment(id);
        shipmentDispatchResultService.completeDispatch(id, getDispatchBatchId(id), payload);

        ShipmentDetailResponse response = shipmentRequestService.getShipmentDetailById(id);
        ShipmentRequest shipmentRequest = shipmentRequestRepository.findById(id).orElseThrow();

        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.SUCCESS);
        assertThat(response.getMessage()).isNull();
        assertThat(shipmentRequest.getProcessingInfo().getErrorPayload()).isNull();
    }

    @Test
    void completeDispatch_changesProcessingShipmentToFailedWhenShipmentNoContainsFail() {
        Long id = saveShipmentAndGetId("SHP-FAIL-RESULT-001");
        String payload = "{\"shipmentId\":" + id + ",\"shipmentNo\":\"SHP-FAIL-RESULT-001\"}";

        shipmentDispatchService.dispatchShipment(id);
        shipmentDispatchResultService.completeDispatch(id, getDispatchBatchId(id), payload);

        ShipmentDetailResponse response = shipmentRequestService.getShipmentDetailById(id);
        ShipmentRequest shipmentRequest = shipmentRequestRepository.findById(id).orElseThrow();

        assertThat(response.getShipmentNo()).isEqualTo("SHP-FAIL-RESULT-001");
        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo("WMS transmission failed");
        assertThat(shipmentRequest.getProcessingInfo().getErrorPayload()).isEqualTo(payload);
    }

    @Test
    void completeDispatch_ignoresStaleDispatchBatchMessage() {
        Long id = saveShipmentAndGetId("SHP-RESULT-STALE-001");
        String payload = "{\"shipmentId\":" + id + ",\"shipmentNo\":\"SHP-RESULT-STALE-001\"}";

        shipmentDispatchService.dispatchShipment(id);
        shipmentDispatchResultService.completeDispatch(id, "MANUAL-STALE-BATCH", payload);

        ShipmentRequest shipmentRequest = shipmentRequestRepository.findById(id).orElseThrow();
        assertThat(shipmentRequest.getProcessingInfo().getStatus()).isEqualTo(ShipmentStatus.PROCESSING);
        assertThat(shipmentRequest.getProcessingInfo().getMessage()).isNull();
        assertThat(shipmentRequest.getProcessingInfo().getErrorPayload()).isNull();
    }

    private Long saveShipmentAndGetId(String shipmentNo) {
        shipmentRequestService.createShipment(createRequest(shipmentNo));

        return shipmentRequestRepository
                .findAll()
                .stream()
                .filter(shipment -> shipment.getRequestInfo().getShipmentNo().equals(shipmentNo))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private String getDispatchBatchId(Long id) {
        return shipmentRequestRepository.findById(id)
                .orElseThrow()
                .getProcessingInfo()
                .getDispatchBatchId();
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
