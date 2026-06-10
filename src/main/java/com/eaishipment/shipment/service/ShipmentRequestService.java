package com.eaishipment.shipment.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eaishipment.global.exception.BusinessException;
import com.eaishipment.shipment.dto.ShipmentCreateRequest;
import com.eaishipment.shipment.dto.ShipmentCreateResponse;
import com.eaishipment.shipment.dto.ShipmentDetailResponse;
import com.eaishipment.shipment.dto.ShipmentDispatchResponse;
import com.eaishipment.shipment.dto.ShipmentListResponse;
import com.eaishipment.shipment.dto.ShipmentRetryResponse;
import com.eaishipment.shipment.dto.ShipmentStatusUpdateRequest;
import com.eaishipment.shipment.dto.ShipmentStatusUpdateResponse;
import com.eaishipment.shipment.entity.CustomerInfo;
import com.eaishipment.shipment.entity.ShipmentItemInfo;
import com.eaishipment.shipment.entity.ShipmentRequest;
import com.eaishipment.shipment.entity.ShipmentRequestInfo;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.entity.WarehouseInfo;
import com.eaishipment.shipment.event.ShipmentDispatchMessage;
import com.eaishipment.shipment.mapper.ShipmentRequestMapper;
import com.eaishipment.shipment.producer.ShipmentDispatchProducer;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;

@Service
public class ShipmentRequestService {
    private static final Logger log = LoggerFactory.getLogger(ShipmentRequestService.class);

    private final ShipmentRequestRepository shipmentRequestRepository;

    private final ShipmentDispatchProducer shipmentDispatchProducer;

    public ShipmentRequestService(
            ShipmentRequestRepository shipmentRequestRepository,
            ShipmentDispatchProducer shipmentDispatchProducer) {
        this.shipmentRequestRepository = shipmentRequestRepository;
        this.shipmentDispatchProducer = shipmentDispatchProducer;
    }

    @Transactional
    public ShipmentCreateResponse createShipment(ShipmentCreateRequest request) {
        if (shipmentRequestRepository.existsByRequestInfo_ShipmentNo(request.getShipmentNo())) {
            throw new BusinessException("이미 등록된 출고 지시 번호입니다.");
        }

        ShipmentRequestInfo requestInfo = new ShipmentRequestInfo(request.getShipmentNo(), request.getOrderNo(),
                request.getRequestedAt());
        WarehouseInfo warehouseInfo = new WarehouseInfo(request.getWarehouseCode());
        CustomerInfo customerInfo = new CustomerInfo(request.getCustomerCode(), request.getCustomerName());
        ShipmentItemInfo itemInfo = new ShipmentItemInfo(request.getMaterialCode(), request.getMaterialName(),
                request.getQuantity(), request.getUnit());
        ShipmentRequest shipmentRequest = new ShipmentRequest(requestInfo, warehouseInfo, customerInfo, itemInfo);

        shipmentRequestRepository.save(shipmentRequest);

        return ShipmentCreateResponse.success(request.getShipmentNo());
    }

    @Transactional(readOnly = true)
    public List<ShipmentListResponse> getShipments() {
        return shipmentRequestRepository
                .findAll()
                .stream()
                .map(ShipmentRequestMapper::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShipmentDetailResponse getShipmentDetailById(Long id) {
        ShipmentRequest shipmentRequest = getShipmentRequestById(id);
        return ShipmentRequestMapper.toDetailResponse(shipmentRequest);
    }

    @Transactional(readOnly = true)
    public List<ShipmentListResponse> getShipmentByStatus(ShipmentStatus status) {
        return shipmentRequestRepository
                .findByProcessingInfo_Status(status)
                .stream()
                .map(ShipmentRequestMapper::toListResponse)
                .toList();
    }

    @Transactional
    public ShipmentStatusUpdateResponse updateStatus(Long id, ShipmentStatusUpdateRequest request) {
        ShipmentRequest shipmentRequest = getShipmentRequestById(id);
        if (shipmentRequest.getProcessingInfo().getStatus() == ShipmentStatus.SUCCESS) {
            throw new BusinessException("이미 처리 완료된 출고 지시는 상태를 변경할 수 없습니다.");
        }

        ShipmentStatus status = request.getStatus();
        String message = request.getMessage();

        if (status == ShipmentStatus.FAILED) {
            if (message == null || message.isBlank())
                throw new BusinessException("message는 필수 입니다.");
        }

        shipmentRequest.updateStatus(request.getStatus(), request.getMessage());
        return ShipmentRequestMapper.toUpdateResponse(shipmentRequest);
    }

    @Transactional
    public ShipmentRetryResponse retryShipment(Long id) {
        ShipmentRequest shipmentRequest = getShipmentRequestById(id);
        if (shipmentRequest.getProcessingInfo().getStatus() != ShipmentStatus.FAILED) {
            throw new BusinessException("재처리 대상이 아닙니다.");
        }

        shipmentRequest.retrySuccess();

        return ShipmentRequestMapper.toRetryResponse(shipmentRequest);
    }

    @Transactional
    public ShipmentDispatchResponse dispatchShipment(Long id) {
        ShipmentRequest shipmentRequest = getShipmentRequestById(id);

        if (shipmentRequest.getProcessingInfo().getStatus() != ShipmentStatus.RECEIVED) {
            throw new BusinessException("Dispatch 대상이 아닙니다.");
        }

        log.info("Shipment dispatch requested. shipmentId={}, shipmentNo={}",
                shipmentRequest.getId(),
                shipmentRequest.getRequestInfo().getShipmentNo());

        shipmentRequest.updateStatus(ShipmentStatus.PROCESSING, null);

        ShipmentDispatchMessage message = new ShipmentDispatchMessage(
                shipmentRequest.getId(),
                shipmentRequest.getRequestInfo().getShipmentNo());

        shipmentDispatchProducer.send(message);

        return ShipmentRequestMapper.toDispatchResponse(shipmentRequest);
    }

    @Transactional
    public void completeDispatch(Long id, String payload) {
        ShipmentRequest shipmentRequest = getShipmentRequestById(id);

        if (isWmsSendFailed(shipmentRequest)) {
            shipmentRequest.updateStatus(
                ShipmentStatus.FAILED, 
                "WMS transmission failed",
            payload
        );

            log.info("Shipment dispatch failed. shipmentId={}, shipmentNo={}, message={}",
                    shipmentRequest.getId(),
                    shipmentRequest.getRequestInfo().getShipmentNo(),
                    "WMS transmission failed");

            return;
        }

        shipmentRequest.updateStatus(ShipmentStatus.SUCCESS, null);

        log.info("Shipment dispatch completed. shipmentId={}, shipmentNo={}",
                shipmentRequest.getId(),
                shipmentRequest.getRequestInfo().getShipmentNo());
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
