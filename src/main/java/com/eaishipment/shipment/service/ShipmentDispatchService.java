package com.eaishipment.shipment.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eaishipment.global.exception.BusinessException;
import com.eaishipment.shipment.dto.ShipmentDispatchResponse;
import com.eaishipment.shipment.entity.ShipmentRequest;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.event.ShipmentDispatchMessage;
import com.eaishipment.shipment.mapper.ShipmentRequestMapper;
import com.eaishipment.shipment.producer.ShipmentDispatchProducer;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;

@Service
public class ShipmentDispatchService {
    private static final Logger log = LoggerFactory.getLogger(ShipmentDispatchService.class);

    private final ShipmentRequestRepository shipmentRequestRepository;
    private final ShipmentDispatchProducer shipmentDispatchProducer;

    public ShipmentDispatchService(
            ShipmentRequestRepository shipmentRequestRepository,
            ShipmentDispatchProducer shipmentDispatchProducer) {
        this.shipmentRequestRepository = shipmentRequestRepository;
        this.shipmentDispatchProducer = shipmentDispatchProducer;
    }

    @Transactional
    public ShipmentDispatchResponse dispatchShipment(Long id) {
        String dispatchBatchId = "MANUAL-" + UUID.randomUUID().toString().replace("-", "");
        return dispatchShipment(id, dispatchBatchId);
    }

    private ShipmentDispatchResponse dispatchShipment(Long id, String dispatchBatchId) {
        ShipmentRequest shipmentRequest = getShipmentRequestById(id);

        if (shipmentRequest.getProcessingInfo().getStatus() != ShipmentStatus.RECEIVED) {
            throw new BusinessException("Dispatch 대상이 아닙니다.");
        }

        log.info("Shipment dispatch requested. shipmentId={}, shipmentNo={}, dispatchBatchId={}",
                shipmentRequest.getId(),
                shipmentRequest.getRequestInfo().getShipmentNo(),
                dispatchBatchId);

        shipmentRequest.updateDispatchBatchId(dispatchBatchId);
        shipmentRequest.updateStatus(ShipmentStatus.PROCESSING, null);

        ShipmentDispatchMessage message = new ShipmentDispatchMessage(
                shipmentRequest.getId(),
                shipmentRequest.getRequestInfo().getShipmentNo(),
                dispatchBatchId);

        shipmentDispatchProducer.send(message);

        return ShipmentRequestMapper.toDispatchResponse(shipmentRequest);
    }

    @Transactional
    public int dispatchReceivedShipments(String dispatchBatchId) {
        List<ShipmentRequest> shipmentRequests = shipmentRequestRepository
                .findByProcessingInfo_Status(ShipmentStatus.RECEIVED);

        int count = 0;
        for (ShipmentRequest shipmentRequest : shipmentRequests) {
            try {
                dispatchShipment(shipmentRequest.getId(), dispatchBatchId);
                count++;
            } catch (Exception e) {
                log.error("Shipment dispatch scheduler item failed. shipmentId={}, dispatchBatchId={}",
                        shipmentRequest.getId(),
                        dispatchBatchId,
                        e);
            }

        }

        return count;
    }

    private ShipmentRequest getShipmentRequestById(Long id) {
        return shipmentRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("출고 지시를 찾을 수 없습니다."));
    }
}
