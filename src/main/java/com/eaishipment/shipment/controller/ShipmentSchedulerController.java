package com.eaishipment.shipment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eaishipment.global.response.ApiResponse;
import com.eaishipment.shipment.dto.ShipmentSchedulerStatusResponse;
import com.eaishipment.shipment.scheduler.ShipmentDispatchScheduler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/api/shipments/scheduler")
@Tag(name = "Shipment Scheduler", description = "APIs for checking and controlling the automatic shipment dispatch scheduler.")
public class ShipmentSchedulerController {
    private final ShipmentDispatchScheduler shipmentDispatchScheduler;

    public ShipmentSchedulerController(ShipmentDispatchScheduler shipmentDispatchScheduler) {
        this.shipmentDispatchScheduler = shipmentDispatchScheduler;
    }

    @GetMapping
    @Operation(summary = "Get shipment scheduler status", description = "Returns whether the automatic shipment dispatch scheduler is currently enabled.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment scheduler status returned")
    public ResponseEntity<ApiResponse<ShipmentSchedulerStatusResponse>> getScheduleStatus() {
        boolean schedulerStatus = shipmentDispatchScheduler.isEnabled();
        return createResponse("Shipment scheduler status returned.", schedulerStatus);
    }

    @PatchMapping("/enable")
    @Operation(summary = "Enable shipment scheduler", description = "Enables the automatic shipment dispatch scheduler. This endpoint should be restricted to authorized operators.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment scheduler enabled")
    public ResponseEntity<ApiResponse<ShipmentSchedulerStatusResponse>> enableSchedule() {
        if (shipmentDispatchScheduler.isEnabled()) {
            return createResponse("Shipment scheduler is already enabled.", true);
        }

        shipmentDispatchScheduler.enable();
        return createResponse("Shipment scheduler enabled.", true);
    }

    @PatchMapping("/disable")
    @Operation(summary = "Disable shipment scheduler", description = "Disables the automatic shipment dispatch scheduler. This endpoint should be restricted to authorized operators.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment scheduler disabled")
    public ResponseEntity<ApiResponse<ShipmentSchedulerStatusResponse>> disableSchedule() {
        if (!shipmentDispatchScheduler.isEnabled()) {
            return createResponse("Shipment scheduler is already disabled.", false);
        }

        shipmentDispatchScheduler.disable();
        return createResponse("Shipment scheduler disabled.", false);
    }

    private ResponseEntity<ApiResponse<ShipmentSchedulerStatusResponse>> createResponse(String message,
            boolean schedulerStatus) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(message, new ShipmentSchedulerStatusResponse(schedulerStatus)));
    }
}
