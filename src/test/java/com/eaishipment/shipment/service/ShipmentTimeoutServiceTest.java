package com.eaishipment.shipment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.eaishipment.shipment.dto.ShipmentCreateRequest;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.producer.ShipmentDispatchProducer;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("test")
class ShipmentTimeoutServiceTest {

    @Autowired
    private ShipmentRequestService shipmentRequestService;

    @Autowired
    private ShipmentDispatchService shipmentDispatchService;

    @Autowired
    private ShipmentTimeoutService shipmentTimeoutService;

    @Autowired
    private ShipmentRequestRepository shipmentRequestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ShipmentDispatchProducer shipmentDispatchProducer;

    @BeforeEach
    void setUp() {
        shipmentRequestRepository.deleteAll();
    }

    @Test
    void failStaleProcessingShipments_changesOldProcessingShipmentToFailed() {
        Long id = saveShipmentAndGetId("SHP-TIMEOUT-001");
        shipmentDispatchService.dispatchShipment(id);
        jdbcTemplate.update(
                "update shipment_request set updated_at = ? where id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(15)),
                id);

        int count = shipmentTimeoutService.failStaleProcessingShipments();

        assertThat(count).isEqualTo(1);
        assertThat(shipmentRequestRepository.findById(id).orElseThrow().getProcessingInfo().getStatus())
                .isEqualTo(ShipmentStatus.FAILED);
        assertThat(shipmentRequestRepository.findById(id).orElseThrow().getProcessingInfo().getMessage())
                .isEqualTo("Dispatch timeout");
    }

    @Test
    void failStaleProcessingShipments_doesNotChangeRecentProcessingShipment() {
        Long id = saveShipmentAndGetId("SHP-TIMEOUT-002");
        shipmentDispatchService.dispatchShipment(id);

        int count = shipmentTimeoutService.failStaleProcessingShipments();

        assertThat(count).isEqualTo(0);
        assertThat(shipmentRequestRepository.findById(id).orElseThrow().getProcessingInfo().getStatus())
                .isEqualTo(ShipmentStatus.PROCESSING);
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
}
