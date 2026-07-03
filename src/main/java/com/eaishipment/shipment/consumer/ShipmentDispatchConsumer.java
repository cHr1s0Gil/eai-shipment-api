package com.eaishipment.shipment.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.eaishipment.shipment.event.ShipmentDispatchMessage;
import com.eaishipment.shipment.service.ShipmentDispatchResultService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ShipmentDispatchConsumer {
    private static final Logger log = LoggerFactory.getLogger(ShipmentDispatchConsumer.class);

    private final ShipmentDispatchResultService shipmentDispatchResultService;
    private final ObjectMapper objectMapper;

    public ShipmentDispatchConsumer(
            ShipmentDispatchResultService shipmentDispatchResultService,
            ObjectMapper objectMapper) {
        this.shipmentDispatchResultService = shipmentDispatchResultService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "shipment-dispatch", groupId = "eai-shipment-api")
    public void consume(ShipmentDispatchMessage message) {
        String payload = toPayload(message);
        log.info("Shipment dispatch message consumed. shipmentId={}, shipmentNo={}, dispatchBatchId={}",
                message.getShipmentId(),
                message.getShipmentNo(),
                message.getDispatchBatchId());

        shipmentDispatchResultService.completeDispatch(message.getShipmentId(), payload);

        log.info("Shipment dispatch message processed. shipmentId={}, shipmentNo={}, dispatchBatchId={}",
                message.getShipmentId(),
                message.getShipmentNo(),
                message.getDispatchBatchId());
    }

    private String toPayload(ShipmentDispatchMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch(JacksonException e) {
            log.warn("Failed to serialize shipment dispatch message. shipmentId={}, shipmentNo={}, dispatchBatchId={}",
                message.getShipmentId(),
                message.getShipmentNo(),
                message.getDispatchBatchId(),
                e);
            return message.toString();
        }
    }
}
