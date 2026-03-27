package com.globalbooks.orders.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.globalbooks.orders.model.Order;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Publishes order events to RabbitMQ topic exchange.
 */
@Component
public class OrderEventPublisher {

    private static final Logger LOG = Logger.getLogger(OrderEventPublisher.class.getName());

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper   objectMapper;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.payment}")
    private String paymentRoutingKey;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper   = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Publishes an order.created event to the payments queue ONLY.
     * ShippingService will be notified by PaymentsService after payment succeeds.
     */
    public void publishOrderCreated(Order order) {
        try {
            String payload = objectMapper.writeValueAsString(order);

            // Payment routing: order.exchange → order.payment → payments.queue
            rabbitTemplate.convertAndSend(exchange, paymentRoutingKey, payload);
            LOG.info("Published payment event for orderId: " + order.getOrderId());

        } catch (JsonProcessingException e) {
            LOG.log(Level.SEVERE, "Failed to serialize order for messaging: " + order.getOrderId(), e);
            throw new RuntimeException("Messaging error for order: " + order.getOrderId(), e);
        }
    }
}
