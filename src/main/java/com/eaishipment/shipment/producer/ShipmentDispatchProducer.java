package com.eaishipment.shipment.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.eaishipment.shipment.event.ShipmentDispatchMessage;

@Component
public class ShipmentDispatchProducer {
    private static final Logger log = LoggerFactory.getLogger(ShipmentDispatchProducer.class);
    private static final String TOPIC = "shipment-dispatch";

    private final KafkaTemplate<String, ShipmentDispatchMessage> kafkaTemplate;

    public ShipmentDispatchProducer(KafkaTemplate<String, ShipmentDispatchMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(ShipmentDispatchMessage message) {
        log.info("Shipment dispatch message publishing. topic={}, shipmentId={}, shipmentNo={}",
                TOPIC,
                message.getShipmentId(),
                message.getShipmentNo());

        kafkaTemplate.send(TOPIC, message.getShipmentNo(), message);

        log.info("Shipment dispatch message published. topic={}, shipmentId={}, shipmentNo={}",
                TOPIC,
                message.getShipmentId(),
                message.getShipmentNo());
    }
}
