# GlobalBooks Inc. – SOA & Microservices Project
### CCS3341 Coursework | Due: 27 March 2026

---

## Project Structure

```
SOA & Microservices/
├── CatalogService/              ← Java JAX-WS SOAP WAR (Tomcat 9)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/globalbooks/catalog/
│       │   ├── CatalogPortType.java       (SEI)
│       │   ├── CatalogServiceImpl.java    (implementation)
│       │   ├── model/                     (Book, PriceResponse)
│       │   ├── exception/                 (BookNotFoundException)
│       │   └── security/                  (WsSecurityHandler)
│       └── webapp/WEB-INF/
│           ├── sun-jaxws.xml              (JAX-WS endpoint config)
│           ├── web.xml                    (servlet config)
│           └── wsdl/catalog.wsdl         (WSDL contract)
│
├── OrdersService/               ← Spring Boot REST (port 8081)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/globalbooks/orders/
│       ├── OrdersApplication.java
│       ├── controller/OrderController.java
│       ├── service/OrderService.java
│       ├── model/                         (Order, OrderItem, etc.)
│       ├── messaging/OrderEventPublisher.java
│       ├── security/OAuth2ResourceServerConfig.java
│       └── config/RabbitMQConfig.java
│
├── PaymentsService/             ← Spring Boot + RabbitMQ consumer (port 8082)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/globalbooks/payments/
│       └── consumer/PaymentsConsumer.java
│
├── ShippingService/             ← Spring Boot + RabbitMQ consumer (port 8083)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/globalbooks/shipping/
│       └── consumer/ShippingConsumer.java
│
├── bpel/
│   ├── PlaceOrder.bpel           ← BPEL 2.0 orchestration process
│   └── deploy.xml                ← Apache ODE deployment descriptor
│
├── rabbitmq/
│   └── definitions.json          ← Exchange, queue, DLQ, binding definitions
│
├── docs/
│   ├── SOA_Architecture_Design.md
│   ├── UDDI_Registry_Metadata.xml
│   ├── Governance_Policy.md
│   └── openapi.yaml              ← OrdersService OpenAPI 3.0 spec
│
├── testing/
│   ├── soap/                     ← SOAP UI test XML files
│   │   ├── test_get_book_price.xml
│   │   ├── test_get_book_by_id.xml
│   │   └── test_search_books.xml
│   └── rest/
│       └── test_orders_api.bat   ← curl REST test script
│
└── docker-compose.yml            ← Full stack deployment (ILO4)
```

---

## Quick Start

### 1. Start Infrastructure

```cmd
:: Start RabbitMQ
net start RabbitMQ

:: Start Keycloak (first time)
docker run -d --name keycloak -p 9000:8080 ^
  -e KEYCLOAK_ADMIN=admin ^
  -e KEYCLOAK_ADMIN_PASSWORD=admin ^
  quay.io/keycloak/keycloak:latest start-dev

:: Start Tomcat 9 (serves CatalogService, ODE, jUDDI)
cd %CATALINA_HOME%\bin && startup.bat
```

### 2. Build and Deploy BPEL Orchestration Process

**Required:** Tomcat must be running (started in step 1)

```cmd
cd bpel
mvn clean package
```

**What this does:**
- Compiles BPEL specification and creates deployment package
- Packages all artifacts (PlaceOrder.bpel, deploy.xml, WSDLs, schemas) into `PlaceOrder.zip`
- Automatically deploys the ZIP to ODE's deployment directory: `%CATALINA_HOME%\webapps\ode\WEB-INF\processes\`
- ODE deployment poller detects the package and activates the process

**Verify deployment:**

```cmd
REM Check the package was deployed
dir "%CATALINA_HOME%\webapps\ode\WEB-INF\processes"

REM Should see: PlaceOrder.zip
```

Then check the ODE Console:
- Open: http://localhost:8080/ode/
- Navigate to: "Processes" tab
- Verify: `PlaceOrder` appears with status **ACTIVE**

**Troubleshooting:**

If the package doesn't deploy, run the verification script:
```cmd
verify-bpel-deployment.bat
```

For manual deployment if Maven failover:
```cmd
deploy-bpel-manual.bat
```

For ODE health status:
```cmd
check-ode-status.bat
```

**Property Override** (if Tomcat is in a different location):

```cmd
mvn clean package -Dcatalina.home=C:\path\to\tomcat
```

Or set environment variable `CATALINA_HOME` once for all builds.

---

### 3. Build and Deploy CatalogService

```cmd
cd CatalogService
mvn clean package
copy target\CatalogService.war %CATALINA_HOME%\webapps\
```

### 4. Start Spring Boot Services

```cmd
:: Each in its own terminal
cd OrdersService   && mvn spring-boot:run
cd PaymentsService && mvn spring-boot:run
cd ShippingService && mvn spring-boot:run
```

### 5. OR use Docker Compose for OrdersService + PaymentsService + ShippingService

```cmd
docker-compose up --build -d
```

---

## Service Endpoints

| Service | URL |
|---------|-----|
| CatalogService WSDL | http://localhost:8080/CatalogService/catalog?wsdl |
| OrdersService REST  | http://localhost:8081/api/v1/orders |
| PaymentsService     | http://localhost:8082 |
| ShippingService     | http://localhost:8083 |
| Apache ODE          | http://localhost:8080/ode/ |
| jUDDI GUI           | http://localhost:8080/juddi-gui/ |
| RabbitMQ Dashboard  | http://localhost:15672 (guest/guest) |
| Keycloak Admin      | http://localhost:9000 (admin/admin) |

---

## PlaceOrder Orchestration

The synchronous order flow is orchestrated by Apache ODE:

1. `PlaceOrder.wsdl` exposes the BPEL entry operation.
2. `PlaceOrder.bpel` loops through order items and invokes `CatalogService.getBookPrice()` over SOAP.
3. The same BPEL instance then invokes `OrdersService` over HTTP `POST /api/v1/orders` using the HTTP binding described in `bpel/orders.wsdl`.
4. `OrdersService` creates the order and publishes the payment event to RabbitMQ for downstream processing.

The `bpel/` deployment package contains the ODE-facing partner contracts:

- `catalog.wsdl` for the existing SOAP CatalogService partner
- `orders.wsdl` for the REST-style HTTP partner contract used for order creation
- `order-contract.xsd` for the XML payload schema exchanged between ODE and OrdersService
- `orders.endpoint` for the default HTTP headers used by the Orders partner, including the demo bearer token

---

## Assignment Question Coverage

| Question | Answer in... |
|----------|-------------|
| Q1 – SOA principles | `docs/SOA_Architecture_Design.md` §2 |
| Q2 – Service decomposition | `docs/SOA_Architecture_Design.md` §3 |
| Q3 – WSDL design | `CatalogService/src/main/webapp/WEB-INF/wsdl/catalog.wsdl` |
| Q4 – UDDI registry | `docs/UDDI_Registry_Metadata.xml` |
| Q5 – SOAP service config | `CatalogServiceImpl.java`, `sun-jaxws.xml`, `web.xml` |
| Q6 – SOAP operations | `CatalogServiceImpl.java` – getBookById, getBookPrice, searchBooks |
| Q7 – REST API design | `OrderController.java`, `docs/openapi.yaml` |
| Q8 – BPEL orchestration | `bpel/PlaceOrder.bpel`, `bpel/deploy.xml` |
| Q9 – Service testing | `testing/soap/`, `testing/rest/` |
| Q10 – Messaging integration | `OrderEventPublisher.java`, `PaymentsConsumer.java`, `ShippingConsumer.java` |
| Q11 – Error handling | `RabbitMQConfig.java` (DLQ), `application.yml` (retry) |
| Q12 – WS-Security | `WsSecurityHandler.java` |
| Q13 – OAuth2 | `OAuth2ResourceServerConfig.java`, Keycloak |
| Q14 – QoS | `RabbitMQConfig.java` (confirms, TTL), `application.yml` (prefetch) |
| Q15 – Governance | `docs/Governance_Policy.md` |
| ILO4 – Cloud deployment | `docker-compose.yml`, `Dockerfile` (×3) |
