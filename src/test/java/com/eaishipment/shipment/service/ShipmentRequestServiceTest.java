package com.eaishipment.shipment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.eaishipment.shipment.dto.ShipmentCreateResponse;
import com.eaishipment.shipment.dto.ShipmentDetailResponse;
import com.eaishipment.shipment.dto.ShipmentRetryResponse;
import com.eaishipment.shipment.dto.ShipmentStatusUpdateRequest;
import com.eaishipment.shipment.dto.ShipmentStatusUpdateResponse;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.producer.ShipmentDispatchProducer;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("test")
class ShipmentRequestServiceTest {

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
    void createShipment_savesShipmentWithReceivedStatus() {
        ShipmentCreateRequest request = createRequest("SHP-TEST-001");

        ShipmentCreateResponse response = shipmentRequestService.createShipment(request);

        assertThat(response.getShipmentNo()).isEqualTo("SHP-TEST-001");
        assertThat(shipmentRequestRepository.count()).isEqualTo(1);
        assertThat(shipmentRequestService.getShipments())
                .hasSize(1)
                .first()
                .extracting("status")
                .isEqualTo(ShipmentStatus.RECEIVED);
    }

    @Test
    void createShipment_throwsExceptionWhenShipmentNoIsDuplicated() {
        ShipmentCreateRequest request = createRequest("SHP-TEST-002");
        shipmentRequestService.createShipment(request);

        assertThatThrownBy(() -> shipmentRequestService.createShipment(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getShipmentDetailById_returnsSavedShipment() {
        Long id = saveShipmentAndGetId("SHP-TEST-003");

        ShipmentDetailResponse response = shipmentRequestService.getShipmentDetailById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getShipmentNo()).isEqualTo("SHP-TEST-003");
        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.RECEIVED);
    }

    @Test
    void getShipmentDetailById_throwsExceptionWhenShipmentDoesNotExist() {
        assertThatThrownBy(() -> shipmentRequestService.getShipmentDetailById(999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateStatus_changesStatusToFailedWithMessage() {
        Long id = saveShipmentAndGetId("SHP-TEST-004");
        ShipmentStatusUpdateRequest request = statusRequest(ShipmentStatus.FAILED, "WMS stock shortage");

        ShipmentStatusUpdateResponse response = shipmentRequestService.updateStatus(id, request);

        assertThat(response.getShipmentNo()).isEqualTo("SHP-TEST-004");
        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo("WMS stock shortage");
    }

    @Test
    void updateStatus_throwsExceptionWhenFailedStatusHasNoMessage() {
        Long id = saveShipmentAndGetId("SHP-TEST-005");
        ShipmentStatusUpdateRequest request = statusRequest(ShipmentStatus.FAILED, null);

        assertThatThrownBy(() -> shipmentRequestService.updateStatus(id, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void retryShipment_changesFailedShipmentToSuccess() {
        Long id = saveShipmentAndGetId("SHP-TEST-006");
        shipmentRequestService.updateStatus(id, statusRequest(ShipmentStatus.FAILED, "Temporary WMS error"));

        ShipmentRetryResponse response = shipmentRequestService.retryShipment(id);

        assertThat(response.getShipmentNo()).isEqualTo("SHP-TEST-006");
        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.SUCCESS);
        assertThat(response.getRetryCount()).isEqualTo(1);
        assertThat(response.getMessage()).isNull();
    }

    @Test
    void retryShipment_throwsExceptionWhenShipmentIsNotFailed() {
        Long id = saveShipmentAndGetId("SHP-TEST-007");

        assertThatThrownBy(() -> shipmentRequestService.retryShipment(id))
                .isInstanceOf(BusinessException.class);
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

    private ShipmentStatusUpdateRequest statusRequest(ShipmentStatus status, String message) {
        ShipmentStatusUpdateRequest request = new ShipmentStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", status);
        ReflectionTestUtils.setField(request, "message", message);
        return request;
    }
}
