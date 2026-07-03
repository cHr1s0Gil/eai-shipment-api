package com.eaishipment.shipment.scheduler;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.eaishipment.shipment.service.ShipmentDispatchService;
import com.eaishipment.shipment.service.ShipmentTimeoutService;

@Component
public class ShipmentDispatchScheduler {
    private static final Logger log = LoggerFactory.getLogger(ShipmentDispatchScheduler.class);

    private final AtomicBoolean dispatchEnabled = new AtomicBoolean(true);
    private final AtomicBoolean timeoutCheckEnabled = new AtomicBoolean(true);

    private final ShipmentTimeoutService shipmentTimeoutService;
    private final ShipmentDispatchService shipmentDispatchService;

    public ShipmentDispatchScheduler(ShipmentTimeoutService shipmentTimeoutService, ShipmentDispatchService shipmentDispatchService) {
        this.shipmentTimeoutService = shipmentTimeoutService;
        this.shipmentDispatchService = shipmentDispatchService;
    }

    @Scheduled(cron = "${shipment.dispatch.scheduler.cron.default}")
    public void dispatchReceivedShipments() {
        if (!isDispatchEnabled()) {
            log.debug("Shipment dispatch scheduler skipped. scheduler disabled.");
            return;
        }

        String dispatchBatchId = "DISPATCH-" + UUID.randomUUID().toString().replace("-", "");
        int count = shipmentDispatchService.dispatchReceivedShipments(dispatchBatchId);

        if (count == 0) {
            log.debug("Shipment dispatch scheduler skipped. count=0");
            return;
        }

        log.info("Shipment dispatch scheduler completed. dispatchBatchId={} count={}", dispatchBatchId, count);
    }

    @Scheduled(cron = "${shipment.dispatch.scheduler.cron.default}")
    public void failStaleProcessingShipments() {
        if (!isTimeoutCheckEnabled()) {
            log.debug("Shipment timeout check scheduler skipped. timeout check disabled.");
            return;
        }

        int count = shipmentTimeoutService.failStaleProcessingShipments();

        if (count > 0) {
            log.warn("Stale PROCESSING shipments marked as FAILED. count={}", count);
        }
    }

    public void enableDispatch() {
        dispatchEnabled.set(true);
        log.info("Shipment dispatch scheduler enabled.");
    }

    public void disableDispatch() {
        dispatchEnabled.set(false);
        log.info("Shipment dispatch scheduler disabled.");
    }

    public boolean isDispatchEnabled() {
        return dispatchEnabled.get();
    }

    public void enableTimeoutCheck() {
        timeoutCheckEnabled.set(true);
        log.info("Shipment timeout check scheduler enabled.");
    }

    public void disableTimeoutCheck() {
        timeoutCheckEnabled.set(false);
        log.info("Shipment timeout check scheduler disabled.");
    }

    public boolean isTimeoutCheckEnabled() {
        return timeoutCheckEnabled.get();
    }
}
