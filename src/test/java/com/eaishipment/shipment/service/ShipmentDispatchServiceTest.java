package com.eaishipment.shipment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.eaishipment.global.exception.BusinessException;
import com.eaishipment.shipment.dto.ShipmentCreateRequest;
import com.eaishipment.shipment.dto.ShipmentDispatchResponse;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.event.ShipmentDispatchMessage;
import com.eaishipment.shipment.producer.ShipmentDispatchProducer;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("test")
class ShipmentDispatchServiceTest {

    @Autowired
    private ShipmentRequestService shipmentRequestService;

    @Autowired
    private ShipmentDispatchService shipmentDispatchService;

    @Autowired
    private ShipmentRequestRepository shipmentRequestRepository;

    @MockitoBean
    private ShipmentDispatchProducer shipmentDispatchProducer;

    @BeforeEach
    void setUp() {
        shipmentRequestRepository.deleteAll();
    }

    @Test
    void dispatchShipment_changesReceivedShipmentToProcessing() {
        Long id = saveShipmentAndGetId("SHP-DISPATCH-001");

        ShipmentDispatchResponse response = shipmentDispatchService.dispatchShipment(id);

        assertThat(response.getShipmentNo()).isEqualTo("SHP-DISPATCH-001");
        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.PROCESSING);
        assertThat(response.getMessage()).isNull();

        assertThat(shipmentRequestRepository.findById(id).orElseThrow().getProcessingInfo().getDispatchBatchId())
                .startsWith("MANUAL-");
        verify(shipmentDispatchProducer, times(1)).send(any(ShipmentDispatchMessage.class));
    }

    @Test
    void dispatchShipment_throwsExceptionWhenShipmentIsNotReceived() {
        Long id = saveShipmentAndGetId("SHP-DISPATCH-002");
        shipmentDispatchService.dispatchShipment(id);

        assertThatThrownBy(() -> shipmentDispatchService.dispatchShipment(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void dispatchReceivedShipments_dispatchesOnlyReceivedShipments() {
        Long receivedId = saveShipmentAndGetId("SHP-DISPATCH-003");
        Long failedId = saveShipmentAndGetId("SHP-DISPATCH-004");
        shipmentRequestService.updateStatus(failedId, statusRequest(ShipmentStatus.FAILED, "test failure"));

        int count = shipmentDispatchService.dispatchReceivedShipments("DISPATCH-TEST-BATCH");

        assertThat(count).isEqualTo(1);
        assertThat(shipmentRequestRepository.findById(receivedId).orElseThrow().getProcessingInfo().getStatus())
                .isEqualTo(ShipmentStatus.PROCESSING);
        assertThat(shipmentRequestRepository.findById(failedId).orElseThrow().getProcessingInfo().getStatus())
                .isEqualTo(ShipmentStatus.FAILED);
        verify(shipmentDispatchProducer, times(1)).send(any(ShipmentDispatchMessage.class));
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

    private com.eaishipment.shipment.dto.ShipmentStatusUpdateRequest statusRequest(ShipmentStatus status, String message) {
        com.eaishipment.shipment.dto.ShipmentStatusUpdateRequest request = new com.eaishipment.shipment.dto.ShipmentStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", status);
        ReflectionTestUtils.setField(request, "message", message);
        return request;
    }
}
