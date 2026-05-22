package com.eaishipment.shipment.mapper;

import com.eaishipment.shipment.dto.ShipmentDetailResponse;
import com.eaishipment.shipment.dto.ShipmentListResponse;
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
            request.getRequestInfo().getRequestedAt(),
            request.getWarehouseInfo().getWarehouseCode(),
            request.getCustomerInfo().getCustomerCode(),
            request.getCustomerInfo().getCustomerName(),
            request.getItemInfo().getMaterialCode(), 
            request.getItemInfo().getMaterialName(), 
            request.getItemInfo().getQuantity(), 
            request.getItemInfo().getUnit(),
            request.getProcessingInfo().getStatus(), 
            request.getAuditInfo().getCreatedAt(),
            request.getAuditInfo().getUpdatedAt()
        );
    }
}
