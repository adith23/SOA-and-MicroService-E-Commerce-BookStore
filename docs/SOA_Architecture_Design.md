# SOA Architecture Design Document
## GlobalBooks Inc. – Monolith to SOA Migration
### CCS3341 SOA & Microservices

---

## 1. Executive Summary

GlobalBooks Inc. is migrating its legacy monolithic Java application to a Service-Oriented Architecture (SOA). This document defines the service boundaries, architectural principles, design decisions, integration patterns, and governance model applied during the migration.

The resulting architecture exposes four independent services:

| Service | Protocol | Technology |
|---------|----------|-----------|
| CatalogService | SOAP (WSDL) | Java JAX-WS WAR |
| OrdersService | REST (JSON) | Spring Boot |
| PaymentsService | Async Messaging | Spring Boot + RabbitMQ |
| ShippingService | Async Messaging | Spring Boot + RabbitMQ |

---

## 2. SOA Design Principles Applied (Q1)

The decomposition of the monolith follows Thomas Erl's eight foundational SOA principles:

### 2.1 Standardised Service Contract
Each service exposes a formal, technology-neutral contract:
- **CatalogService** – WSDL 1.1 document (`catalog-v1.wsdl`) with XSD-defined types
- **OrdersService** – OpenAPI 3.0 specification (`openapi.yaml`) with JSON Schema
- **PaymentsService / ShippingService** – AMQP message schema documented in `definitions.json`

### 2.2 Service Loose Coupling
Services communicate only through their contracts. No service calls another service's internal methods or shares a database table. Dependencies are:
- BPEL → CatalogService via SOAP
- BPEL → OrdersService via REST
- OrdersService → RabbitMQ exchange (fire-and-forget)
- PaymentsService/ShippingService → consume from RabbitMQ queues

### 2.3 Service Abstraction
Internal implementation details are hidden:
- CatalogService's internal data store (in-memory `HashMap`) is invisible to consumers
- OrdersService's JPA entity structure is not exposed in the REST response
- Consumers interact only through WSDL operations and REST endpoints

### 2.4 Service Reusability
- `CatalogService.getBookPrice()` is invoked by the BPEL PlaceOrder process for each order item and is also available directly to legacy SOAP clients
- `CatalogService.searchBooks()` is reusable by any future search feature

### 2.5 Service Autonomy
Each service is independently deployable:
- CatalogService deploys as a WAR to Tomcat 9
- OrdersService, PaymentsService, ShippingService deploy as Spring Boot JARs with embedded servers
- Each service can be restarted, scaled, or updated without affecting the others

### 2.6 Service Statelessness
- SOAP requests carry all required context in the envelope body (bookId, credentials in WS-Security header)
- REST endpoints are stateless; order state is persisted in the OrdersService data store and referenced by `orderId`
- BPEL process instances manage their own workflow state via the ODE engine

### 2.7 Service Discoverability
CatalogService is registered in the Apache jUDDI UDDI v3 registry with:
- Business key, service key, binding template
- Reference to WSDL location
- Category classification (e-commerce, book catalog)

### 2.8 Service Composability
The BPEL PlaceOrder process composes all four services into a single end-to-end business workflow:
1. Receives a PlaceOrder request from the client
2. Loops through each item → invokes CatalogService.getBookPrice() per item
3. Invokes OrdersService to create the order
4. OrdersService publishes to RabbitMQ → triggers PaymentsService and ShippingService

---

## 3. Service Decomposition Rationale (Q2)

### 3.1 Decomposition Approach: Domain-Driven Decomposition
Services were identified by mapping each major business domain:

| Business Domain | Service | Boundary Rationale |
|----------------|---------|-------------------|
| Book catalogue | CatalogService | Read-heavy, reused by multiple consumers, cacheable |
| Order management | OrdersService | Transactional, owns order lifecycle |
| Payment processing | PaymentsService | Financial domain, requires audit trail, async safe |
| Shipment coordination | ShippingService | External courier integration, inherently async |

### 3.2 Key Benefit: Independent Scalability
During promotional events (e.g., holiday sales), only OrdersService and PaymentsService experience peak load. With the monolith, the entire application had to be scaled. In the SOA:
- CatalogService (read-heavy) scales horizontally behind a load balancer with response caching
- PaymentsService scales by adding more consumer instances on the RabbitMQ queue
- ShippingService remains at baseline capacity — its throughput is naturally throttled by the queue

### 3.3 Key Challenge: Distributed Data Consistency
Without a shared database, ensuring that a book is not "sold" without a successful payment requires a carefully choreographed flow:
- The BPEL engine maintains workflow state across service invocations
- RabbitMQ provides at-least-once delivery via publisher confirms and consumer acknowledgements
- If PaymentsService fails, the message is retried up to 3 times (exponential backoff), then routed to a dead-letter queue for manual investigation
- This is fundamentally more complex than a single ACID transaction in the monolith

---

## 4. Architecture Diagram

```
                                 ┌────────────────────┐
                                 │  UDDI Registry     │
                                 │  (jUDDI)           │  ← Service discovery
                                 └────────────────────┘

┌─────────────┐                  ┌─────────────────────────────────────┐
│  Legacy     │  SOAP/HTTPS      │          BPEL Engine                │
│  Partner    │─────────────────▶│          (Apache ODE)               │
│  (WS client)│  WS-Security     │          PlaceOrder Process         │
└─────────────┘                  │                                     │
                                 │  1. Receive PlaceOrderRequest        │
┌─────────────┐  SOAP/HTTPS      │  2. ForEach item:                   │
│  Modern     │─────────────────▶│     → getBookPrice() [LOOP]         │
│  Web/Mobile │                  │  3. Invoke OrdersService             │
│  Client     │                  │  4. Reply to client                  │
└─────────────┘                  └──────────┬───────────┬──────────────┘
  ALL clients enter                         │           │
  through BPEL                  SOAP invoke │           │ REST invoke
                                (loop/item) │           │
                         ┌──────────────────┘           └──────────────────────┐
                         ▼                                                     ▼
              ┌──────────────────────┐                         ┌───────────────────────┐
              │  CatalogService      │                         │  OrdersService         │
              │  (Java JAX-WS WAR)   │                         │  (Spring Boot)         │
              │  Port: 8080/Tomcat9  │                         │  Port: 8081            │
              │  ✅ WS-Security       │                         │  ✅ OAuth2 JWT          │
              └──────────────────────┘                         └────────────┬───────────┘
                                                                            │ Publishes
                                                                            ▼ AMQP
                                                               ┌────────────────────────┐
                                                               │  RabbitMQ (ESB)        │
                                                               │  Exchange: order.exchange│
                                                               │  Topic type, durable    │
                                                               └───────┬────────┬────────┘
                                                              Consumes │        │ Consumes
                                                                       ▼        ▼
                                                           ┌───────────────┐ ┌───────────────┐
                                                           │ Payments      │ │ Shipping      │
                                                           │ Service       │ │ Service       │
                                                           │ Port: 8082    │ │ Port: 8083    │
                                                           └───────────────┘ └───────────────┘

Security:
  WS-Security (UsernameToken) ──▶ CatalogService
  OAuth2 JWT Bearer            ──▶ OrdersService

Flow: Client → BPEL → [Loop: getBookPrice()] → OrdersService → RabbitMQ → Payments + Shipping
```

---

## 5. Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| SOAP engine | JAX-WS RI (Metro) | Native Java EE; integrates with Tomcat via `WSServlet` |
| REST framework | Spring Boot 3.x | Industry standard; Spring Security OAuth2 built-in |
| Message broker | RabbitMQ | Supports AMQP; topic exchange enables fan-out to multiple consumers |
| BPEL engine | Apache ODE 1.3.8 | Only open-source BPEL 2.0 engine; assigned technology |
| UDDI registry | Apache jUDDI 3.3.10 | UDDI v3 compliant; free; deploys on Tomcat |
| OAuth2 provider | Keycloak | Open-source; production-grade; Docker-friendly |
| Servlet container | Tomcat 9.0.x | Compatible with both JAX-WS WAR and ODE/jUDDI |

---

## 6. Error Handling Strategy

| Layer | Strategy |
|-------|---------|
| SOAP | `@WebFault` annotated exceptions → SOAP `<Fault>` elements in response |
| REST | `@ControllerAdvice` → standardised JSON error responses with HTTP status codes |
| BPEL | `<faultHandlers>` catch SOAP faults from partner services; process terminates gracefully |
| RabbitMQ | 3 retries with exponential backoff (1s, 2s, 4s); then route to Dead-Letter Queue |
| DLQ | Operations team monitors `payments.dlq` and `shipping.dlq`; manual replay |

---

## 7. QoS Configuration

- **Reliable Messaging:** RabbitMQ publisher confirms + consumer acknowledgements (manual ACK mode)
- **Durable Queues:** Queues and messages marked persistent; survive broker restart
- **Prefetch Count:** Set to 10 to limit concurrent unacknowledged messages per consumer
- **Message TTL:** 30,000ms in primary queues; expired messages route to DLQ

---

## 8. ODE-to-OrdersService Integration Note

The executable PlaceOrder process uses Apache ODE as the synchronous orchestrator.

1. `PlaceOrder.bpel` receives the client order request.
2. The process loops through each line item and invokes `CatalogService.getBookPrice()` over SOAP.
3. After computing the final total, ODE invokes `OrdersService` directly over HTTP `POST /api/v1/orders`.
4. `OrdersService` persists the order, returns the created order payload, and publishes the payment event to RabbitMQ.

To keep this direct integration explicit and deployable, the BPEL package includes:

- `orders.wsdl` using WSDL 1.1 HTTP binding for the REST partner call
- `order-contract.xsd` defining the XML payload exchanged between ODE and OrdersService
- `orders.endpoint` supplying default HTTP headers for the trusted machine bearer token and XML content negotiation
