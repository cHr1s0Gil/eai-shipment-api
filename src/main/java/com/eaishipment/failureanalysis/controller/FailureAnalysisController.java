package com.eaishipment.failureanalysis.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eaishipment.failureanalysis.dto.FailureAnalysisResponse;
import com.eaishipment.failureanalysis.entity.ShipmentFailureAnalysis;
import com.eaishipment.failureanalysis.mapper.FailureAnalysisMapper;
import com.eaishipment.failureanalysis.service.FailureAnalysisService;
import com.eaishipment.global.response.ApiResponse;

@RestController
@RequestMapping("/api/analyses")
public class FailureAnalysisController {
    private final FailureAnalysisService failureAnalysisService;

    public FailureAnalysisController(FailureAnalysisService failureAnalysisService) {
        this.failureAnalysisService = failureAnalysisService;
    }

    @PostMapping("/shipments/{shipmentId}/failure")
    public ResponseEntity<ApiResponse<FailureAnalysisResponse>> analyzeFailure(
            @PathVariable("shipmentId") Long shipmentId) {
            ShipmentFailureAnalysis analysis = failureAnalysisService.analyzeFailure(shipmentId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Failure analysis completed", FailureAnalysisMapper.toResponse(analysis)));
    }
}
