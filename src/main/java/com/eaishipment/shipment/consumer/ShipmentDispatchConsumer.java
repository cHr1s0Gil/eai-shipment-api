package com.eaishipment.shipment.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.eaishipment.shipment.event.ShipmentDispatchMessage;
import com.eaishipment.shipment.service.ShipmentRequestService;

@Component
public class ShipmentDispatchConsumer {
    private final ShipmentRequestService shipmentRequestService;

    public ShipmentDispatchConsumer(ShipmentRequestService shipmentRequestService) {
        this.shipmentRequestService = shipmentRequestService;
    }

    @KafkaListener(topics =  "shipment-dispatch", groupId = "eai-shipment-api")
    public void consume(ShipmentDispatchMessage message) {
        shipmentRequestService.completeDispatch(message.getShipmentId());
    }
}
