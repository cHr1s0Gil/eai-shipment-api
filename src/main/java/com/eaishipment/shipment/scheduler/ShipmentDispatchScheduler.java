package com.eaishipment.shipment.scheduler;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.eaishipment.shipment.service.ShipmentRequestService;

@Component
public class ShipmentDispatchScheduler {
    private static final Logger log = LoggerFactory.getLogger(ShipmentDispatchScheduler.class);

    private final AtomicBoolean enabled = new AtomicBoolean(true);

    private final ShipmentRequestService shipmentRequestService;

    public ShipmentDispatchScheduler(ShipmentRequestService shipmentRequestService) {
        this.shipmentRequestService = shipmentRequestService;
    }

    @Scheduled(cron = "${shipment.dispatch.scheduler.cron.default}")
    public void dispatchReceivedShipments() {
        if (!isEnabled()) {
            log.debug("Shipment dispatch scheduler skipped. scheduler disabled.");
            return;
        }

        String dispatchBatchId = "DISPATCH-" + UUID.randomUUID().toString().replace("-", "");
        int count = shipmentRequestService.dispatchReceivedShipments(dispatchBatchId);

        if (count == 0) {
            log.debug("Shipment dispatch scheduler skipped. count=0");
            return;
        }

        log.info("Shipment dispatch scheduler completed. dispatchBatchId={} count={}", dispatchBatchId, count);
    }

    public void enable() {
        enabled.set(true);
        log.info("Shipment dispatch scheduler enabled.");
    }

    public void disable() {
        enabled.set(false);
        log.info("Shipment dispatch scheduler disabled.");
    }

    public boolean isEnabled() {
        return enabled.get();
    }
}
