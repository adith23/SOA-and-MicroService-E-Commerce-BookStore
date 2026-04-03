package com.globalbooks.orders.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Shipping address for an order.
 */
public class ShippingAddress {

    @NotBlank(message = "street is required")
    @JacksonXmlProperty(localName = "street", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private String street;

    @NotBlank(message = "city is required")
    @JacksonXmlProperty(localName = "city", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private String city;

    @NotBlank(message = "country is required")
    @JacksonXmlProperty(localName = "country", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private String country;

    @NotBlank(message = "postalCode is required")
    @JacksonXmlProperty(localName = "postalCode", namespace = CreateOrderRequest.ORDER_CONTRACT_NAMESPACE)
    private String postalCode;

    public ShippingAddress() {}

    public ShippingAddress(String street, String city, String country, String postalCode) {
        this.street     = street;
        this.city       = city;
        this.country    = country;
        this.postalCode = postalCode;
    }

    public String getStreet()     { return street; }
    public String getCity()       { return city; }
    public String getCountry()    { return country; }
    public String getPostalCode() { return postalCode; }

    public void setStreet(String street)         { this.street = street; }
    public void setCity(String city)             { this.city = city; }
    public void setCountry(String country)       { this.country = country; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
}
