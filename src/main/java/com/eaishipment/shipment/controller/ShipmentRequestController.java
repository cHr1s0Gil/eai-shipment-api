package com.eaishipment.shipment.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eaishipment.global.response.ApiResponse;
import com.eaishipment.shipment.dto.ShipmentCreateRequest;
import com.eaishipment.shipment.dto.ShipmentCreateResponse;
import com.eaishipment.shipment.service.ShipmentRequestService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentRequestController {
    private final ShipmentRequestService shipmentRequestService;
    
    public ShipmentRequestController(ShipmentRequestService shipmentRequestService) {
        this.shipmentRequestService = shipmentRequestService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentCreateResponse>> createShipment(
        @Valid @RequestBody ShipmentCreateRequest request
    ) {
        ShipmentCreateResponse response = shipmentRequestService.createShipment(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("출고 지시 수신 성공", response));
    }
    
}
