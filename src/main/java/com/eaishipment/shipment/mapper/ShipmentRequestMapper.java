package com.eaishipment.shipment.mapper;

import com.eaishipment.shipment.dto.ShipmentDetailResponse;
import com.eaishipment.shipment.dto.ShipmentDispatchResponse;
import com.eaishipment.shipment.dto.ShipmentListResponse;
import com.eaishipment.shipment.dto.ShipmentRetryResponse;
import com.eaishipment.shipment.dto.ShipmentStatusUpdateResponse;
import com.eaishipment.shipment.entity.ShipmentRequest;

public class ShipmentRequestMapper {
    private ShipmentRequestMapper() {}

    public static ShipmentListResponse toListResponse(ShipmentRequest request) {
        return new ShipmentListResponse(
            request.getId(),
            request.getRequestInfo().getShipmentNo(),
            request.getRequestInfo().getOrderNo(),
            request.getCustomerInfo().getCustomerName(),
            request.getItemInfo().getMaterialName(),
            request.getItemInfo().getQuantity(),
            request.getProcessingInfo().getStatus(),
            request.getRequestInfo().getRequestedAt(),
            request.getAuditInfo().getCreatedAt()
        );    
    }

    public static ShipmentDetailResponse toDetailResponse(ShipmentRequest request) {
        return new ShipmentDetailResponse(
            request.getId(),
            request.getRequestInfo().getShipmentNo(), 
            request.getRequestInfo().getOrderNo(),
            request.getProcessingInfo().getDispatchBatchId(), 
            request.getRequestInfo().getRequestedAt(),
            request.getWarehouseInfo().getWarehouseCode(),
            request.getCustomerInfo().getCustomerCode(),
            request.getCustomerInfo().getCustomerName(),
            request.getItemInfo().getMaterialCode(), 
            request.getItemInfo().getMaterialName(), 
            request.getItemInfo().getQuantity(), 
            request.getItemInfo().getUnit(),
            request.getProcessingInfo().getStatus(),
            request.getProcessingInfo().getMessage(), 
            request.getAuditInfo().getCreatedAt(),
            request.getAuditInfo().getUpdatedAt(),
            request.getProcessingInfo().getRetryCount()
        );
    }

    public static ShipmentStatusUpdateResponse toUpdateResponse(ShipmentRequest request) {
        return new ShipmentStatusUpdateResponse(
            request.getRequestInfo().getShipmentNo(), 
            request.getProcessingInfo().getStatus(), 
            request.getProcessingInfo().getMessage()
        );
    }

    public static ShipmentRetryResponse toRetryResponse(ShipmentRequest request) {
        return new ShipmentRetryResponse(
            request.getRequestInfo().getShipmentNo(), 
            request.getProcessingInfo().getStatus(), 
            request.getProcessingInfo().getRetryCount(),
            request.getProcessingInfo().getMessage()
        );
    }

    public static ShipmentDispatchResponse toDispatchResponse(ShipmentRequest request) {
        return new ShipmentDispatchResponse(
            request.getRequestInfo().getShipmentNo(),
            request.getProcessingInfo().getStatus(),
            request.getProcessingInfo().getMessage()
        );
    }
}
