package com.eaishipment.shipment.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eaishipment.global.response.ApiResponse;
import com.eaishipment.shipment.dto.ShipmentCreateRequest;
import com.eaishipment.shipment.dto.ShipmentCreateResponse;
import com.eaishipment.shipment.dto.ShipmentDetailResponse;
import com.eaishipment.shipment.dto.ShipmentListResponse;
import com.eaishipment.shipment.dto.ShipmentStatusUpdateRequest;
import com.eaishipment.shipment.dto.ShipmentStatusUpdateResponse;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.service.ShipmentRequestService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;


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
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShipmentListResponse>>> getShipmentList() {
        List<ShipmentListResponse> response = shipmentRequestService.getShipments();
        String message = response.isEmpty() ? "출고 지시 데이터가 없습니다." : "출고 지시 목록 조회 성공";
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success(message, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentDetailResponse>> getShipmentDetail(@PathVariable("id") Long id) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success("출고 상세정보 조회 성공 id: " + id, shipmentRequestService.getShipmentDetailById(id)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ShipmentListResponse>>> getShipmentListByStatus(@PathVariable("status") ShipmentStatus status) {
        List<ShipmentListResponse> response = shipmentRequestService.getShipmentByStatus(status);
        String message = response.isEmpty() ? "출고 지시 데이터가 없습니다." : "출고 지시 목록 조회 성공";
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success(message, response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ShipmentStatusUpdateResponse>> updateShipmentStatus(
        @PathVariable("id") Long id,
        @Valid @RequestBody ShipmentStatusUpdateRequest request) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse
            .success("출고 지시 상태 변경 성공", shipmentRequestService.updateStatus(id, request)));
    }
}
