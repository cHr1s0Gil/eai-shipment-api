package com.eaishipment.shipment.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.eaishipment.shipment.event.ShipmentDispatchMessage;

@Component
public class ShipmentDispatchProducer {
    private static final String TOPIC = "shipment-dispatch";

    private final KafkaTemplate<String, ShipmentDispatchMessage> kafkaTemplate;
    
    public ShipmentDispatchProducer(KafkaTemplate<String, ShipmentDispatchMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(ShipmentDispatchMessage message) {
        kafkaTemplate.send(TOPIC, message.getShipmentNo(), message);
    }
}
