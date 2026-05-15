package com.example.eaishipmentapi.shipment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ShipmentItemInfo {
    @Column(name = "material_code")
    private String materialCode;

    @Column(name = "material_name")
    private String materialName;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit")
    private String unit;

    protected ShipmentItemInfo() {}

    public ShipmentItemInfo(String materialCode, String materialName, Integer quantity, String unit) {
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.quantity = quantity;
        this.unit = unit;
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
