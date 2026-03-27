package com.globalbooks.orders.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for POST /api/v1/orders
 */
@JacksonXmlRootElement(localName = "createOrderRequest", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
public class CreateOrderRequest {

    public static final String ORDER_CONTRACT_NAMESPACE = "http://globalbooks.com/order-contract/v1";

    @NotBlank(message = "customerId is required and must match pattern C[0-9]{4}")
    @JacksonXmlProperty(localName = "customerId", namespace = ORDER_CONTRACT_NAMESPACE)
    private String customerId;

    @NotEmpty(message = "items must contain at least one entry")
    @Valid
    @JacksonXmlElementWrapper(localName = "items", namespace = ORDER_CONTRACT_NAMESPACE)
    @JacksonXmlProperty(localName = "item", namespace = ORDER_CONTRACT_NAMESPACE)
    private List<OrderItem> items;

    @Valid
    @JacksonXmlProperty(localName = "shippingAddress", namespace = ORDER_CONTRACT_NAMESPACE)
    private ShippingAddress shippingAddress;

    @JacksonXmlProperty(localName = "totalAmount", namespace = ORDER_CONTRACT_NAMESPACE)
    private BigDecimal totalAmount;

    public CreateOrderRequest() {}

    public String getCustomerId()           { return customerId; }
    public List<OrderItem> getItems()       { return items; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public BigDecimal getTotalAmount()      { return totalAmount; }

    public void setCustomerId(String customerId)           { this.customerId = customerId; }
    public void setItems(List<OrderItem> items)            { this.items = items; }
    public void setShippingAddress(ShippingAddress addr)   { this.shippingAddress = addr; }
    public void setTotalAmount(BigDecimal totalAmount)     { this.totalAmount = totalAmount; }
}
