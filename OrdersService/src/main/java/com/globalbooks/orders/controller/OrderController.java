package com.globalbooks.orders.controller;

import com.globalbooks.orders.model.CreateOrderRequest;
import com.globalbooks.orders.model.Order;
import com.globalbooks.orders.service.OrderService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

/**
 * REST controller for OrdersService.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/v1/orders
     * Create a new order.
     *
     * Request body: CreateOrderRequest (JSON or XML)
     * Response: 201 Created with Location header and order body
     */
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
                 produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        System.out.println("\n[OrdersService] ======= INCOMING REST REQUEST =======");
        System.out.println("[OrdersService] Authenticated POST /api/v1/orders received.");
        System.out.println("[OrdersService] Processing order for Customer ID: " + request.getCustomerId());
        
        Order created;
        try {
            created = orderService.createOrder(request);
            System.out.println("[OrdersService] SUCCESS: Order created successfully. OrderId=" + created.getOrderId());
        } catch (Exception e) {
            System.out.println("[OrdersService] ERROR: Failed to create order: " + e.getMessage());
            throw e;
        }

        // Build Location header: /api/v1/orders/{orderId}
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getOrderId())
            .toUri();

        System.out.println("[OrdersService] Returning 201 Created to Adapter.");
        System.out.println("[OrdersService] =======================================\n");
        return ResponseEntity.created(location).body(created);
    }

    /**
     * GET /api/v1/orders/{id}
     * Retrieve an order by ID.
     *
     * Response: 200 OK with order body, or 404 Not Found
     */
    @GetMapping(value = "/{id}",
                produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        return orderService.getOrderById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/orders
     * List all orders.
     *
     * Response: 200 OK with array of orders
     */
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

}
