package com.eaishipment.shipment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eaishipment.shipment.entity.ShipmentRequest;

public interface ShipmentRequestRepository extends JpaRepository<ShipmentRequest, Long> {
    boolean existsByRequestInfo_ShipmentNo(String shipmentNo);
    List<ShipmentRequest> findByProcessingInfo_Status(String status);
}
