package com.example.eaishipmentapi.shipment.entity;

public enum ShipmentStatus {
    RECEIVED("출고 지시 수신 완료"),
    PROCESSING("처리 중"),
    SUCCESS("처리 성공"),
    FAILED("처리 실패");

    private final String description;

    ShipmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}