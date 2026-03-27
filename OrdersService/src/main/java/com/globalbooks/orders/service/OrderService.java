package com.globalbooks.orders.service;

import com.globalbooks.orders.messaging.OrderEventPublisher;
import com.globalbooks.orders.model.CreateOrderRequest;
import com.globalbooks.orders.model.Order;
import com.globalbooks.orders.model.Order.OrderStatus;
import com.globalbooks.orders.model.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Business logic for order creation and retrieval.
 */
@Service
public class OrderService {

    private final OrderEventPublisher eventPublisher;

    // In-memory order store (simulates a database)
    private final Map<String, Order> orderStore = new HashMap<>();
    private final AtomicInteger orderCounter = new AtomicInteger(1);

    public OrderService(OrderEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new order from the incoming request.
     * Calculates total, sets status to PENDING, persists, and publishes events.
     */
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();

        // Generate orderId in format ORD-YYYYMMDD-NNN
        String orderId = String.format("ORD-%s-%03d",
            LocalDateTime.now().toLocalDate().toString().replace("-", ""),
            orderCounter.getAndIncrement());

        order.setOrderId(orderId);
        order.setCustomerId(request.getCustomerId());
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(OrderStatus.PENDING);
        order.setCurrency("USD");
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> enrichedItems = normalizeItems(request.getItems());
        order.setItems(enrichedItems);

        BigDecimal total = calculateTotal(enrichedItems);
        if (request.getTotalAmount() != null && total.compareTo(request.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("totalAmount does not match priced order items");
        }
        order.setTotalAmount(total);

        // Persist
        orderStore.put(orderId, order);

        // Publish to RabbitMQ for PaymentsService and ShippingService
        eventPublisher.publishOrderCreated(order);

        return order;
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Retrieves an order by its ID.
     */
    public Optional<Order> getOrderById(String orderId) {
        return Optional.ofNullable(orderStore.get(orderId));
    }

    /**
     * Returns all orders.
     */
    public List<Order> getAllOrders() {
        return List.copyOf(orderStore.values());
    }

    /**
     * Enriches order items with prices.
     * NOTE: In the BPEL orchestration, CatalogService.getBookPrice() is called
     * in a loop for each item. This service just stores the final enriched data.
     */
    private List<OrderItem> normalizeItems(List<OrderItem> items) {
        // Mock prices matching CatalogService in-memory catalog
        Map<String, BigDecimal> priceMap = Map.of(
            "B001", new BigDecimal("31.99"),
            "B002", new BigDecimal("44.99"),
            "B003", new BigDecimal("39.99"),
            "B004", new BigDecimal("49.99"),
            "B005", new BigDecimal("34.99"),
            "B006", new BigDecimal("55.99")
        );
        Map<String, String> titleMap = Map.of(
            "B001", "Clean Code",
            "B002", "Design Patterns",
            "B003", "The Pragmatic Programmer",
            "B004", "Microservices Patterns",
            "B005", "Clean Architecture",
            "B006", "Domain-Driven Design"
        );

        return items.stream().map(item -> {
            BigDecimal price = item.getUnitPrice() != null
                ? item.getUnitPrice()
                : priceMap.getOrDefault(item.getBookId(), new BigDecimal("9.99"));
            String title     = titleMap.getOrDefault(item.getBookId(), "Unknown Title");
            return new OrderItem(item.getBookId(), title, item.getQuantity(), price);
        }).toList();
    }
}
