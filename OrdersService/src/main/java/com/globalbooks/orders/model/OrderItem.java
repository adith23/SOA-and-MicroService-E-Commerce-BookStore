package com.globalbooks.orders.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Single line item within an order.
 */
public class OrderItem {

    @NotBlank(message = "bookId is required")
    @JacksonXmlProperty(localName = "bookId", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private String bookId;

    @JacksonXmlProperty(localName = "title", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private String title;   // populated from CatalogService

    @Min(value = 1, message = "quantity must be at least 1")
    @Max(value = 99, message = "quantity must not exceed 99")
    @JacksonXmlProperty(localName = "quantity", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private int quantity;

    @JacksonXmlProperty(localName = "unitPrice", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private BigDecimal unitPrice;  // populated from CatalogService

    public OrderItem() {}

    public OrderItem(String bookId, String title, int quantity, BigDecimal unitPrice) {
        this.bookId    = bookId;
        this.title     = title;
        this.quantity  = quantity;
        this.unitPrice = unitPrice;
    }

    public String getBookId()         { return bookId; }
    public String getTitle()          { return title; }
    public int getQuantity()          { return quantity; }
    public BigDecimal getUnitPrice()  { return unitPrice; }

    public void setBookId(String bookId)           { this.bookId = bookId; }
    public void setTitle(String title)             { this.title = title; }
    public void setQuantity(int quantity)          { this.quantity = quantity; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
