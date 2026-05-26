package com.eaishipment.shipment.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for creating a shipment request")
public class ShipmentCreateRequest {
    @Schema(description = "Unique shipment request number from ERP", example = "SHP-20260522-001")
    @NotBlank(message = "shipmentNo is required.")
    private String shipmentNo;

    @Schema(description = "ERP order number", example = "ORD-20260522-001")
    @NotBlank(message = "orderNo is required.")
    private String orderNo;

    @Schema(description = "Time when ERP requested shipment", example = "2026-05-22T09:00:00")
    @NotNull(message = "requestedAt is required.")
    private LocalDateTime requestedAt;

    @Schema(description = "Warehouse code", example = "WH-SEOUL-01")
    @NotBlank(message = "warehouseCode is required.")
    private String warehouseCode;

    @Schema(description = "Customer code", example = "CUST-001")
    @NotBlank(message = "customerCode is required.")
    private String customerCode;

    @Schema(description = "Customer name", example = "Seoul Distribution")
    @NotBlank(message = "customerName is required.")
    private String customerName;

    @Schema(description = "Material code", example = "MAT-001")
    @NotBlank(message = "materialCode is required.")
    private String materialCode;

    @Schema(description = "Material name", example = "Water 500ml")
    @NotBlank(message = "materialName is required.")
    private String materialName;

    @Schema(description = "Shipment quantity", example = "100")
    @NotNull(message = "quantity is required.")
    @Min(value = 1, message = "quantity must be greater than or equal to 1.")
    private Integer quantity;

    @Schema(description = "Quantity unit", example = "EA")
    @NotBlank(message = "unit is required.")
    private String unit;

    public String getShipmentNo() {
        return shipmentNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }
}
