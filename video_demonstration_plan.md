# GlobalBooks SOA & Microservices - Video Demonstration Plan

**Target Duration**: 15 Minutes
**Objective**: Demonstrate all features required for the CCS3341 Coursework submission.

---

## 📅 Timeline Overview
| Topic | Start time | End Time | Duration |
|-------|------------|----------|----------|
| 1. System Architecture Overview | 0:00 | 2:00 | 2 mins |
| 2. CatalogService (SOAP) & WS-Security | 2:00 | 4:30 | 2.5 mins |
| 3. OrdersService (REST) & OAuth2 | 4:30 | 7:00 | 2.5 mins |
| 4. BPEL Orchestration (PlaceOrder) | 7:00 | 10:00 | 3 mins |
| 5. Messaging & Integration (RabbitMQ) | 10:00 | 12:30 | 2.5 mins |
| 6. Error Handling & Failure Scenarios | 12:30 | 14:30 | 2 mins |
| 7. Conclusion & Wrap-up | 14:30 | 15:00 | 30 secs |

---

## 🎬 Detailed Script & Actions

### Part 1: System Architecture Overview (0:00 - 2:00)
**What to Show:**
* Display the **SOA Architecture Design Document** diagram on screen.
* Keep 6 command prompt terminals open in the background to show all services are running (Tomcat, RabbitMQ, AuthServer, OrdersService, PaymentsService, ShippingService).

**What to Say:**
* *"Hello, my name is [Your Name] and this is my coursework demonstration for the GlobalBooks Microservices migration."*
* *"The system relies on a Service-Oriented Architecture dividing the monolithic application into 4 decoupled sub-domains: Catalog, Orders, Payments, and Shipping."*
* *"The CatalogService exposes SOAP endpoints for legacy integration, while OrdersService provides a REST API. BPEL Orchestration handles the PlaceOrder workflow, and asynchronous messaging is handled by RabbitMQ between Payments and Shipping."*
* *"Security is enforced via WS-Security for SOAP, and OAuth2 for REST."*

### Part 2: CatalogService (SOAP) & WS-Security (2:00 - 4:30)
**What to Show:**
* Open a browser to show the live WSDL: `http://localhost:8080/CatalogService/catalog?wsdl`
* Open **SoapUI**. 
* Show the WS-Security configuration headers in the SoapUI request properties (e.g., UsernameToken credentials).
* Trigger a `GetBook` or `GetBookPrice` request using SoapUI.
* Show the successful XML response.

**What to Say:**
* *"First, I will demonstrate the CatalogService, which runs on Apache Tomcat and is implemented in Java JAX-WS."*
* *"Here is the published WSDL file. I will use SoapUI to send a valid request calculating a book price or checking the catalog."*
* *"To meet the security requirements, this SOAP endpoint requires WS-Security. Notice that my SoapUI request includes the UsernameToken credentials in the header. If I send an invalid token, access is denied. With valid credentials, the catalog data is successfully returned."*

### Part 3: OrdersService (REST) & OAuth2 (4:30 - 7:00)
**What to Show:**
* Open **Postman**.
* Attempt to hit `GET /orders/{id}` or `POST /orders` **without** a token to show a `401 Unauthorized`.
* Execute a request to your **AuthServer** (`POST http://localhost:9000/oauth2/token`) to retrieve an OAuth2 Access Token.
* Paste the Bearer token into the `POST /orders` authorization tab and execute the request.
* Show the `201 Created` or `200 OK` JSON response.

**What to Say:**
* *"Next is the OrdersService, implemented using Spring Boot as a modern REST API."*
* *"Because this service requires OAuth2 security, an unauthenticated request will fail with a 401 response."*
* *"By making an authentication request to the Authorization Server, I can retrieve a valid JWT access token. I will embed this token as a Bearer Header in the request."*
* *"When I re-submit the POST request to create an order, we receive a successful JSON response."*

### Part 4: BPEL Orchestration - PlaceOrder (7:00 - 10:00)
**What to Show:**
* Open the **Apache ODE Dashboard** (`http://localhost:8080/ode`) to show the `PlaceOrder` process is ACTIVE.
* Open SoapUI and target the BPEL process endpoint (`http://localhost:8080/ode/processes/PlaceOrderService`).
* Submit a valid PlaceOrder SOAP request.
* Show the Tomcat console logs where you can see BPEL mapping variables, doing a price lookup from CatalogService, and proxying to OrdersService.

**What to Say:**
* *"Our centralized PlaceOrder functionality is orchestrated using a BPEL engine, Apache ODE."*
* *"The BPEL process handles a Receive activity, loops or extracts attributes, invokes a synchronous price lookup in the CatalogService, and then passes the data to the OrdersService REST API."*
* *"By triggering this workflow via SoapUI, we can see in the Tomcat console the trace prints: the engine successfully extracted the book ID, queried the SOAP Service, and then chained the final creation to the REST endpoint before replying to the client."*

### Part 5: Messaging Interaction (10:00 - 12:30)
**What to Show:**
* Open the **RabbitMQ Management Dashboard** (`http://localhost:15672`).
* Navigate to the **Exchanges** tab (`order.exchange`) and **Queues** tab (`payments.queue`, `shipping.queue`).
* Show the Spring Boot console terminals for `PaymentsService` and `ShippingService`.
* Put through an Order in Postman or BPEL, and immediately highlight the console output for Payments and Shipping where it says `Received message... Processing payment...`.

**What to Say:**
* *"When an order is successfully placed, the OrdersService uses asynchronous, event-driven messaging to decouple operations."*
* *"It publishes an `order.created` event to a topic exchange on RabbitMQ."*
* *"Our PaymentsService and ShippingService act as consumers. You can see the message pass through the RabbitMQ dashboard spikes, and in the terminal consoles, both services simultaneously pick up the message and perform their tasks without blocking the main order thread."*

### Part 6: Error Handling & Failure Scenarios (12:30 - 14:30)
**What to Show:**
* **Scenario A (BPEL Fault):** Submit an invalid `PlaceOrder` request missing a `bookId` in SoapUI. Show the BPEL `Fault` reply (`PlaceOrderFault: Book ID must not be empty`).
* **Scenario B (RabbitMQ Retry/DLQ):** Trigger a message format error or simulate a downed Payment Service. Go to the RabbitMQ Dashboard and show the message routed to the Dead Letter Queue (`payments.dlq` or `shipping.dlq`).

**What to Say:**
* *"To assure robustness and Quality of Service, error handling mechanisms are in place."*
* *"In the BPEL orchestration, if invalid input is provided—such as an empty Book ID—the execution throws a structured Fault instead of a generic timeout, handled by BPEL fault handlers."*
* *"For messaging, we implement dead-letter queues. If a message repeatedly fails to be processed by a consumer like the PaymentsService, it is routed to a DLQ, ensuring no data loss and preserving reliability."*

### Part 7: Conclusion & Wrap-up (14:30 - 15:00)
**What to Show:**
* Turn back to a summary slide/document or your IDE showing the code structure.

**What to Say:**
* *"This completes the demonstration. In summary, I have demonstrated the Catalog SOAP Web service, the Orders REST API, BPEL Orchestration, asynchronous RabbitMQ communication, integrated security contexts, and robustness through fault handling."*
* *"Thank you for your time."*

---

## 📽️ Submission Checklist
1. **Quality Check**: Make sure your text size in SoapUI, Postman, and the terminal is large enough to read on video easily (font size 14-16 recommended).
2. **Audio Check**: Use a quiet room and verify the mic is picking up your voice clearly.
3. **Upload**: Upload the resulting `.mp4` into YouTube (Unlisted), OneDrive, or Google Drive (Ensure link sharing is set to "Anyone with the link can view").
4. **Attach Link**: Paste the URL prominently into your final Reflective Report and/or SOA Documentation.
