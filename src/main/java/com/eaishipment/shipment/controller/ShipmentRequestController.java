package com.eaishipment.shipment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eaishipment.global.response.ApiResponse;
import com.eaishipment.shipment.dto.ShipmentCreateRequest;
import com.eaishipment.shipment.dto.ShipmentCreateResponse;
import com.eaishipment.shipment.dto.ShipmentDetailResponse;
import com.eaishipment.shipment.dto.ShipmentDispatchResponse;
import com.eaishipment.shipment.dto.ShipmentListResponse;
import com.eaishipment.shipment.dto.ShipmentRetryResponse;
// import com.eaishipment.shipment.dto.ShipmentStatusUpdateRequest;
// import com.eaishipment.shipment.dto.ShipmentStatusUpdateResponse;
import com.eaishipment.shipment.entity.ShipmentStatus;
import com.eaishipment.shipment.service.ShipmentDispatchService;
import com.eaishipment.shipment.service.ShipmentRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shipments")
@Tag(name = "Shipment Request", description = "Shipment registration, lookup, status update, and retry APIs")
public class ShipmentRequestController {
	private final ShipmentRequestService shipmentRequestService;
	private final ShipmentDispatchService shipmentDispatchService;

	public ShipmentRequestController(ShipmentRequestService shipmentRequestService,
			ShipmentDispatchService shipmentDispatchService) {
		this.shipmentRequestService = shipmentRequestService;
		this.shipmentDispatchService = shipmentDispatchService;
	}

	@PostMapping
	@Operation(summary = "Create shipment request", description = "Receives an ERP shipment request and stores it with RECEIVED status.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Shipment request created")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or duplicated shipment number")
	public ResponseEntity<ApiResponse<ShipmentCreateResponse>> createShipment(
			@Valid @RequestBody ShipmentCreateRequest request) {
		ShipmentCreateResponse response = shipmentRequestService.createShipment(request);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Shipment request received", response));
	}

	@GetMapping
	@Operation(summary = "Get shipment list", description = "Returns all stored shipment requests.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment list returned")
	public ResponseEntity<ApiResponse<List<ShipmentListResponse>>> getShipmentList() {
		List<ShipmentListResponse> response = shipmentRequestService.getShipments();
		String message = response.isEmpty() ? "No shipment data found." : "Shipment list returned";

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success(message, response));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get shipment detail", description = "Returns one shipment request by id.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment detail returned")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid id format or shipment not found")
	public ResponseEntity<ApiResponse<ShipmentDetailResponse>> getShipmentDetail(
			@Parameter(description = "Shipment request id", example = "1") @PathVariable("id") Long id) {
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success("Shipment detail returned",
						shipmentRequestService.getShipmentDetailById(id)));
	}

	@GetMapping("/status/{status}")
	@Operation(summary = "Get shipment list by status", description = "Returns shipment requests filtered by RECEIVED, PROCESSING, SUCCESS, or FAILED.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment list returned")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid shipment status")
	public ResponseEntity<ApiResponse<List<ShipmentListResponse>>> getShipmentListByStatus(
			@Parameter(description = "Shipment status", example = "RECEIVED") @PathVariable("status") ShipmentStatus status) {
		List<ShipmentListResponse> response = shipmentRequestService.getShipmentByStatus(status);
		String message = response.isEmpty() ? "No shipment data found." : "Shipment list returned";

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success(message, response));
	}

	/*
	 * Manual status changes are intentionally disabled. Shipment status must be changed
	 * only by the dispatch, consumer result, timeout, and retry flows.
	 *
	 * @PatchMapping("/{id}/status")
	 * public ResponseEntity<ApiResponse<ShipmentStatusUpdateResponse>> updateShipmentStatus(
	 *         @PathVariable("id") Long id,
	 *         @Valid @RequestBody ShipmentStatusUpdateRequest request) {
	 *     return ResponseEntity.ok(ApiResponse.success(
	 *             "Shipment status updated", shipmentRequestService.updateStatus(id, request)));
	 * }
	 */

	@PostMapping("/{id}/retry")
	@Operation(summary = "Retry failed shipment", description = "Retries only FAILED shipment requests by republishing a dispatch message. The immediate status is PROCESSING.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment retry dispatch requested")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Shipment is not retryable or not found")
	public ResponseEntity<ApiResponse<ShipmentRetryResponse>> retry(
			@Parameter(description = "Shipment request id", example = "1") @PathVariable("id") Long id) {
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success("Shipment retry dispatch requested", shipmentDispatchService.retryShipment(id)));
	}

	@PostMapping("/{id}/dispatch")
	@Operation(summary = "Dispatch shipment", description = "Simulates sending a RECEIVED shipment request to WMS.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment dispatch completed")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Shipment is not dispatchable or not found")
	public ResponseEntity<ApiResponse<ShipmentDispatchResponse>> dispatchShipment(
			@PathVariable("id") Long id) {
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success("Shipment Dispatch complete", shipmentDispatchService.dispatchShipment(id)));
	}
}
