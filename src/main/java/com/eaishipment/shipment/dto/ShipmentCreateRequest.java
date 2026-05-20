package com.eaishipment.shipment.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ShipmentCreateRequest {
    @NotBlank(message = "출고 지시 번호는 필수입니다.")
    private String shipmentNo;

    @NotBlank(message = "주문 번호는 필수입니다.")
    private String orderNo;

    @NotNull(message = "요청 일시는 필수입니다.")
    private LocalDateTime requestedAt;

    @NotBlank(message = "창고 코드는 필수입니다.")
    private String warehouseCode;

    @NotBlank(message = "고객 코드는 필수입니다.")
    private String customerCode;

    @NotBlank(message = "고객명은 필수입니다.")
    private String customerName;

    @NotBlank(message = "품목 코드는 필수입니다.")
    private String materialCode;

    @NotBlank(message = "품목명은 필수입니다.")
    private String materialName;

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private Integer quantity;

    @NotBlank(message = "단위는 필수입니다.")
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
