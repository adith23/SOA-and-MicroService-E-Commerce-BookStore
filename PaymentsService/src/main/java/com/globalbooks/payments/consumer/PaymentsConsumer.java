package com.globalbooks.payments.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RabbitMQ consumer for PaymentsService.
 */
@Component
public class PaymentsConsumer {

    private static final Logger LOG = Logger.getLogger(PaymentsConsumer.class.getName());

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.shipping}")
    private String shippingRoutingKey;

    public PaymentsConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    }

    /**
     * Handles order.created events from payments.queue.
     * On success → processes payment, then forwards to shipping.queue.
     * On exception → Spring retry triggers (3 attempts), then DLQ.
     */
    @RabbitListener(queues = "payments.queue")
    public void handlePaymentEvent(String message) {
        LOG.info("PaymentsService received message: " + message.substring(0, Math.min(message.length(), 80)) + "...");

        try {
            // Parse order event
            Map<?, ?> orderData = objectMapper.readValue(message, Map.class);
            String orderId     = (String) orderData.get("orderId");
            String customerId  = (String) orderData.get("customerId");
            Object totalAmount = orderData.get("totalAmount");

            LOG.info(String.format("Processing payment | orderId=%s | customerId=%s | total=%s",
                orderId, customerId, totalAmount));

            // Step 1: Process payment
            processPayment(orderId, customerId, totalAmount);
            LOG.info("Payment processed successfully for orderId: " + orderId);

            // Step 2: Forward to ShippingService
            // Only reaches here if payment succeeded (no exception thrown)
            rabbitTemplate.convertAndSend(exchange, shippingRoutingKey, message);
            LOG.info("Forwarded to shipping queue for orderId: " + orderId);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Payment processing failed – will retry or go to DLQ", e);
            // Re-throw to trigger Spring AMQP retry mechanism
            // If all retries fail → message goes to payments.dlq
            // ShippingService is NEVER notified for failed payments
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Simulates payment processing.
     * In production: call payment gateway (Stripe, PayPal, etc.)
     */
    private void processPayment(String orderId, String customerId, Object amount) {
        LOG.info(String.format("  → Charging customer %s for order %s, amount: %s USD",
            customerId, orderId, amount));

        // Simulate a payment gateway call (no real API in this demo)
        LOG.info("  → Payment gateway response: APPROVED");
        LOG.info("  → Payment record stored for orderId: " + orderId);
    }
}
