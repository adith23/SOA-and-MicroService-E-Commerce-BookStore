package com.globalbooks.orders.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Standard error payload for OrdersService across JSON and XML representations.
 */
@JacksonXmlRootElement(localName = "errorResponse", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
public class ErrorResponse {

    @JacksonXmlProperty(localName = "error", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private String error;

    @JacksonXmlProperty(localName = "details", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private String details;

    public ErrorResponse() {
    }

    public ErrorResponse(String error, String details) {
        this.error = error;
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
