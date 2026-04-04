package com.globalbooks.payments.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for PaymentsService.
 * Declares the payments queue, exchange, DLQ, and bindings.
 */
@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE   = "order.exchange";
    public static final String PAYMENTS_QUEUE   = "payments.queue";
    public static final String PAYMENTS_DLQ     = "payments.dlq";
    public static final String DLX_EXCHANGE     = "dlx.exchange";
    public static final String PAYMENT_ROUTING  = "order.payment";

    // Exchange declarations

    /** Topic exchange for order events – must match OrdersService */
    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(ORDER_EXCHANGE)
            .durable(true)
            .build();
    }

    /** Direct exchange for dead-letter routing */
    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE)
            .durable(true)
            .build();
    }

    // Queue declarations

    /** Payments queue with DLX and TTL configured */
    @Bean
    public Queue paymentsQueue() {
        return QueueBuilder.durable(PAYMENTS_QUEUE)
            .withArgument("x-dead-letter-exchange",    DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "payments.dlq")
            .withArgument("x-message-ttl",              30000)
            .build();
    }

    /** Dead-letter queue for failed payment messages */
    @Bean
    public Queue paymentsDlq() {
        return QueueBuilder.durable(PAYMENTS_DLQ).build();
    }

    // Bindings

    @Bean
    public Binding paymentsBinding() {
        return BindingBuilder.bind(paymentsQueue())
            .to(orderExchange())
            .with(PAYMENT_ROUTING);
    }

    @Bean
    public Binding paymentsDlqBinding() {
        return BindingBuilder.bind(paymentsDlq())
            .to(dlxExchange())
            .with("payments.dlq");
    }

    // Message converter

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
