package com.eaishipment.shipment.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eaishipment.shipment.entity.ShipmentRequest;
import com.eaishipment.shipment.entity.ShipmentStatus;

public interface ShipmentRequestRepository extends JpaRepository<ShipmentRequest, Long> {
    boolean existsByRequestInfo_ShipmentNo(String shipmentNo);
    List<ShipmentRequest> findByProcessingInfo_Status(ShipmentStatus status);
    List<ShipmentRequest> findByProcessingInfo_StatusAndAuditInfo_UpdatedAtBefore(
        ShipmentStatus status,
        LocalDateTime updatedAt
    );
}
