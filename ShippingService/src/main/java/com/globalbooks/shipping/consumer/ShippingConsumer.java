package com.globalbooks.shipping.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RabbitMQ consumer for ShippingService.
 */
@Component
public class ShippingConsumer {

    private static final Logger LOG = Logger.getLogger(ShippingConsumer.class.getName());
    private final ObjectMapper objectMapper;

    public ShippingConsumer() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    }

    /**
     * Handles order.created events from shipping.queue.
     * On success → ACKed automatically.
     * On exception → retry 3 times, then dead-letter to shipping.dlq.
     */
    @RabbitListener(queues = "shipping.queue")
    public void handleShippingEvent(String message) {
        LOG.info("ShippingService received message: " + message.substring(0, Math.min(message.length(), 80)) + "...");

        try {
            Map<?, ?> orderData    = objectMapper.readValue(message, Map.class);
            String orderId         = (String) orderData.get("orderId");
            String customerId      = (String) orderData.get("customerId");
            Map<?, ?> address      = (Map<?, ?>) orderData.get("shippingAddress");

            LOG.info(String.format("Scheduling shipment | orderId=%s | customer=%s",
                orderId, customerId));

            scheduleShipment(orderId, customerId, address);

            LOG.info("Shipment scheduled successfully for orderId: " + orderId);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Shipment scheduling failed – will retry or go to DLQ", e);
            throw new RuntimeException("Shipment scheduling failed: " + e.getMessage(), e);
        }
    }

    /**
     * Simulates shipment scheduling.
     */
    private void scheduleShipment(String orderId, String customerId, Map<?, ?> address) {
        String destination = address != null
            ? address.get("city") + ", " + address.get("country")
            : "Unknown";

        LOG.info("  → Generating shipment label for order: " + orderId);
        LOG.info("  → Destination: " + destination);

        // Simulate tracking number generation
        String trackingNumber = "GB-" + orderId + "-" + System.currentTimeMillis() % 10000;
        LOG.info("  → Tracking number assigned: " + trackingNumber);
        LOG.info("  → Courier notified (simulated). Expected delivery: 3-5 business days.");
    }
}
