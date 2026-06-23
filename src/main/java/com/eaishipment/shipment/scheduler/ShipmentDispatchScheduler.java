package com.eaishipment.shipment.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.eaishipment.shipment.service.ShipmentRequestService;

@Component
public class ShipmentDispatchScheduler {
    private static final Logger log = LoggerFactory.getLogger(ShipmentDispatchScheduler.class);

    private final ShipmentRequestService shipmentRequestService;

    public ShipmentDispatchScheduler(ShipmentRequestService shipmentRequestService) {
        this.shipmentRequestService = shipmentRequestService;
    }

    @Scheduled(cron = "${shipment.dispatch.scheduler.cron.default}")
    public void dispatchReceivedShipments() {
        int count = shipmentRequestService.dispatchReceivedShipments();

        if (count == 0) {
            log.debug("Shipment dispatch scheduler skipped. count=0");
            return;
        }

        log.info("Shipment dispatch scheduler completed. count={}", count);
    }
}
