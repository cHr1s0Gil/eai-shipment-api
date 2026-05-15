package com.example.eaishipmentapi.shipment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class WarehouseInfo {
    @Column(name = "warehouse_code")
    private String warehouseCode;

    protected WarehouseInfo() {}

    public WarehouseInfo(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

}
