package com.eaishipment.shipment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eaishipment.global.exception.BusinessException;
import com.eaishipment.shipment.entity.ShipmentRequest;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;

@Service
public class ShipmentDispatchResultService {
    private static final Logger log = LoggerFactory.getLogger(ShipmentDispatchResultService.class);

    private final ShipmentRequestRepository shipmentRequestRepository;

    public ShipmentDispatchResultService(ShipmentRequestRepository shipmentRequestRepository) {
        this.shipmentRequestRepository = shipmentRequestRepository;
    }

    @Transactional
    public void completeDispatch(Long id, String payload) {
        ShipmentRequest shipmentRequest = getShipmentRequestById(id);

        if (isWmsSendFailed(shipmentRequest)) {
            shipmentRequest.updateStatus(
                    ShipmentStatus.FAILED,
                    "WMS transmission failed",
                    payload);

            log.info("Shipment dispatch failed. shipmentId={}, shipmentNo={}, message={}, dispatchBatchId={}",
                    shipmentRequest.getId(),
                    shipmentRequest.getRequestInfo().getShipmentNo(),
                    "WMS transmission failed",
                    shipmentRequest.getProcessingInfo().getDispatchBatchId());

            return;
        }

        shipmentRequest.updateStatus(ShipmentStatus.SUCCESS, null);

        log.info("Shipment dispatch completed. shipmentId={}, shipmentNo={}, dispatchBatchId={}",
                shipmentRequest.getId(),
                shipmentRequest.getRequestInfo().getShipmentNo(),
                shipmentRequest.getProcessingInfo().getDispatchBatchId());
    }

    private ShipmentRequest getShipmentRequestById(Long id) {
        return shipmentRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("출고 지시를 찾을 수 없습니다."));
    }

    private boolean isWmsSendFailed(ShipmentRequest shipmentRequest) {
        return shipmentRequest.getRequestInfo()
                .getShipmentNo()
                .contains("FAIL");
    }
}

