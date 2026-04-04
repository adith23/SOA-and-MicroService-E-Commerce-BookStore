package com.globalbooks.shipping.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for ShippingService.
 * Declares the shipping queue, exchange, DLQ, and bindings.
 */
@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE   = "order.exchange";
    public static final String SHIPPING_QUEUE   = "shipping.queue";
    public static final String SHIPPING_DLQ     = "shipping.dlq";
    public static final String DLX_EXCHANGE     = "dlx.exchange";
    public static final String SHIPPING_ROUTING = "order.shipping";

    // Exchange declarations 

    /** Topic exchange for order events match OrdersService */
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

    /** Shipping queue with DLX and TTL configured */
    @Bean
    public Queue shippingQueue() {
        return QueueBuilder.durable(SHIPPING_QUEUE)
            .withArgument("x-dead-letter-exchange",    DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "shipping.dlq")
            .withArgument("x-message-ttl",              30000)
            .build();
    }

    /** Dead-letter queue for failed shipping messages */
    @Bean
    public Queue shippingDlq() {
        return QueueBuilder.durable(SHIPPING_DLQ).build();
    }

    // Bindings

    @Bean
    public Binding shippingBinding() {
        return BindingBuilder.bind(shippingQueue())
            .to(orderExchange())
            .with(SHIPPING_ROUTING);
    }

    @Bean
    public Binding shippingDlqBinding() {
        return BindingBuilder.bind(shippingDlq())
            .to(dlxExchange())
            .with("shipping.dlq");
    }

    // Message converter 

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
