package com.globalbooks.orders.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order domain model.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "order", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
public class Order {

    @JacksonXmlProperty(localName = "orderId", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private String orderId;

    @JacksonXmlProperty(localName = "customerId", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private String customerId;

    @JacksonXmlElementWrapper(localName = "items", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    @JacksonXmlProperty(localName = "item", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private List<OrderItem> items;

    @JacksonXmlProperty(localName = "totalAmount", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private BigDecimal totalAmount;

    @JacksonXmlProperty(localName = "currency", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private String currency;

    @JacksonXmlProperty(localName = "status", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private OrderStatus status;

    @JacksonXmlProperty(localName = "shippingAddress", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private ShippingAddress shippingAddress;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JacksonXmlProperty(localName = "createdAt", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JacksonXmlProperty(localName = "updatedAt", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
    }

    public Order() {}

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
