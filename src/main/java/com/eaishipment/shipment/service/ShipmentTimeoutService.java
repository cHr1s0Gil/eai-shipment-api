package com.eaishipment.shipment.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eaishipment.shipment.entity.ShipmentRequest;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;

@Service
public class ShipmentTimeoutService {
    private static final Logger log = LoggerFactory.getLogger(ShipmentTimeoutService.class);

    private final ShipmentRequestRepository shipmentRequestRepository;

    public ShipmentTimeoutService(ShipmentRequestRepository shipmentRequestRepository) {
        this.shipmentRequestRepository = shipmentRequestRepository;
    }

    @Transactional
    public int failStaleProcessingShipments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        List<ShipmentRequest> shipmentRequests = shipmentRequestRepository
                .findByProcessingInfo_StatusAndAuditInfo_UpdatedAtBefore(ShipmentStatus.PROCESSING, threshold);

        int count = 0;
        for (ShipmentRequest shipmentRequest : shipmentRequests) {
            try {
                shipmentRequest.updateStatus(ShipmentStatus.FAILED, "Dispatch timeout");
                count++;
                log.warn(
                        "Stale PROCESSING shipment marked as FAILED. shipmentId={}, shipmentNo={}, dispatchBatchId={}, updatedAt={}, threshold={}",
                        shipmentRequest.getId(),
                        shipmentRequest.getRequestInfo().getShipmentNo(),
                        shipmentRequest.getProcessingInfo().getDispatchBatchId(),
                        shipmentRequest.getAuditInfo().getUpdatedAt(),
                        threshold);
            } catch (Exception e) {
                log.error(
                        "Failed to mark stale PROCESSING shipment as FAILED. shipmentId={}, shipmentNo={}, dispatchBatchId={}",
                        shipmentRequest.getId(),
                        shipmentRequest.getRequestInfo().getShipmentNo(),
                        shipmentRequest.getProcessingInfo().getDispatchBatchId(),
                        e);
            }
        }
        return count;
    }
}
