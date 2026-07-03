package com.eaishipment.shipment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eaishipment.global.exception.BusinessException;
import com.eaishipment.shipment.dto.ShipmentCreateRequest;
import com.eaishipment.shipment.dto.ShipmentCreateResponse;
import com.eaishipment.shipment.dto.ShipmentDetailResponse;
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
import com.eaishipment.shipment.mapper.ShipmentRequestMapper;
import com.eaishipment.shipment.repository.ShipmentRequestRepository;

@Service
public class ShipmentRequestService {
    private final ShipmentRequestRepository shipmentRequestRepository;

    public ShipmentRequestService(ShipmentRequestRepository shipmentRequestRepository) {
        this.shipmentRequestRepository = shipmentRequestRepository;
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

    private ShipmentRequest getShipmentRequestById(Long id) {
        return shipmentRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("출고 지시를 찾을 수 없습니다."));
    }
}
