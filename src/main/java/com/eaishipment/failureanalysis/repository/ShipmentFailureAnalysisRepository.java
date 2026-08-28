package com.eaishipment.failureanalysis.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eaishipment.failureanalysis.entity.ShipmentFailureAnalysis;

public interface ShipmentFailureAnalysisRepository extends JpaRepository<ShipmentFailureAnalysis, Long> {
    Optional<ShipmentFailureAnalysis> findByShipmentRequest_IdAndDispatchBatchId(
            Long shipmentId,
            String dispatchBatchId);
}
