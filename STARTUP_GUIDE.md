# GlobalBooks SOA & Microservices - Complete Startup Guide

**Project**: CCS3341 Coursework | GlobalBooks Inc. SOA & Microservices Platform  
**Last Updated**: March 27, 2026  
**Status**: Production-Ready

---

## 📋 Table of Contents

1. [Prerequisites & System Requirements](#prerequisites--system-requirements)
2. [Project Structure Overview](#project-structure-overview)
3. [Java Environment Setup](#java-environment-setup)
4. [Step-by-Step Startup Instructions](#step-by-step-startup-instructions)
5. [Service Details & File Locations](#service-details--file-locations)
6. [Verification Checklist](#verification-checklist)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites & System Requirements

### Required Software

| Component                | Version             | Purpose                                | Download                                                                    |
| ------------------------ | ------------------- | -------------------------------------- | --------------------------------------------------------------------------- |
| **Java Development Kit** | 17 (or 11 for BPEL) | Spring Boot services, BPEL compilation | [Oracle JDK 17](https://www.oracle.com/java/technologies/downloads/#java17) |
| **Apache Maven**         | 3.8.1+              | Build tool for all services            | [Maven 3.8.1+](https://maven.apache.org/download.cgi)                       |
| **Apache Tomcat**        | 9.0.x               | Runtime for CatalogService, ODE BPEL   | [Tomcat 9.0.116](https://tomcat.apache.org/download-90.cgi)                 |
| **RabbitMQ**             | 3.12.0+             | Message broker for async services      | [RabbitMQ 3.12.0](https://www.rabbitmq.com/download.html)                   |
| **Git**                  | Latest              | Version control                        | [Git](https://git-scm.com/download)                                         |

### Optional (for testing)

| Component            | Purpose                              |
| -------------------- | ------------------------------------ |
| **Keycloak**         | OAuth2 Authorization Server (Docker) |
| **Postman/Insomnia** | REST API testing                     |
| **SoapUI**           | SOAP endpoint testing                |
| **Docker Desktop**   | Containerized deployment             |

### System Requirements

- **OS**: Windows 10/11 (tested) or Linux/macOS
- **RAM**: Minimum 8GB (16GB recommended for all services + Tomcat running)
- **Disk**: 5GB free space
- **Ports Required**:
  - `8000` - AuthServer (OAuth2)
  - `8080` - Tomcat (CatalogService + ODE)
  - `8081` - OrdersService
  - `8082` - PaymentsService
  - `8083` - ShippingService
  - `5672` - RabbitMQ (AMQP)
  - `15672` - RabbitMQ Management Console

---

## Project Structure Overview

```
C:\Projects\SOA & Microservices\
├── AuthServer/                      # OAuth2 Authorization Server (Spring Boot, Java 17)
│   ├── src/main/java/com/globalbooks/auth/
│   ├── src/main/resources/application.yml
│   └── pom.xml
│
├── CatalogService/                  # SOAP Web Service (JAX-WS, Java 1.8)
│   ├── src/main/java/com/globalbooks/catalog/
│   ├── src/main/webapp/WEB-INF/
│   │   ├── sun-jaxws.xml            # JAX-WS endpoint config
│   │   ├── web.xml                  # Servlet configuration
│   │   └── wsdl/catalog.wsdl        # WSDL service contract
│   └── pom.xml
│
├── OrdersService/                   # REST Microservice (Spring Boot, Java 17)
│   ├── src/main/java/com/globalbooks/orders/
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── Dockerfile
│
├── PaymentsService/                 # RabbitMQ Consumer (Spring Boot, Java 17)
│   ├── src/main/java/com/globalbooks/payments/
│   │   └── consumer/PaymentsConsumer.java
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── Dockerfile
│
├── ShippingService/                 # RabbitMQ Consumer (Spring Boot, Java 17)
│   ├── src/main/java/com/globalbooks/shipping/
│   │   └── consumer/ShippingConsumer.java
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── Dockerfile
│
├── bpel/                            # BPEL Orchestration (Apache ODE, Java 11)
│   ├── src/main/resources/
│   │   ├── PlaceOrder.bpel          # BPEL process definition
│   │   ├── deploy.xml               # ODE deployment descriptor
│   │   ├── PlaceOrder.wsdl
│   │   ├── orders.wsdl
│   │   ├── catalog.wsdl
│   │   └── order-contract.xsd
│   ├── src/assembly/ode-package.xml # Maven assembly config
│   └── pom.xml
│
├── rabbitmq/                        # RabbitMQ Configuration
│   └── definitions.json             # Exchanges, queues, bindings
│
├── docs/                            # Documentation
│   ├── SOA_Architecture_Design.md
│   ├── UDDI_Registry_Metadata.xml
│   ├── Governance_Policy.md
│   └── openapi.yaml                 # OpenAPI spec for OrdersService
│
├── testing/                         # Test utilities
│   ├── rest/test_orders_api.bat
│   ├── soap/test_get_book_by_id.xml
│   ├── soap/test_get_book_price.xml
│   └── soap/test_search_books.xml
│
├── README.md                        # Project overview
├── STARTUP_GUIDE.md                 # THIS FILE
└── DEPLOYMENT_TROUBLESHOOTING.md    # Troubleshooting guide
```

---

## Java Environment Setup

### ⚙️ Quick Setup for Windows

#### 1. Download & Install JDK 17

```cmd
:: Download from: https://www.oracle.com/java/technologies/downloads/#java17
:: Install to: C:\Program Files\Java\jdk-17.x.x

:: Verify installation
java -version
javac -version
```

**Expected Output:**

```
java version "17.x.x" 20xx-xx-xx
Java(TM) SE Runtime Environment (build 17.x.x+...)
```

#### 2. Set JAVA_HOME Environment Variable

**For Java 17 (Default for most services):**

```cmd
:: Open Command Prompt as Administrator
setx JAVA_HOME "C:\Program Files\Java\jdk-17.x.x"

:: Verify
echo %JAVA_HOME%
```

**Expected Output:**

```
C:\Program Files\Java\jdk-17.x.x
```

#### 3. Add JAVA_HOME to PATH

```cmd
:: Open Command Prompt as Administrator
setx PATH "%JAVA_HOME%\bin;%PATH%"

:: Verify
java -version
```

#### 4. (Optional) Set Up Java 11 for BPEL Build

Some systems may need Java 11 for BPEL compatibility:

```cmd
:: Download JDK 11 from: https://www.oracle.com/java/technologies/downloads/#java11
:: Install to: C:\Program Files\Java\jdk-11.x.x

:: Create a separate batch file to switch Java versions when building BPEL:
:: File: C:\switch-java.bat

@echo off
if "%1"=="17" (
    setx JAVA_HOME "C:\Program Files\Java\jdk-17.x.x"
    echo Updated JAVA_HOME to Java 17
) else if "%1"=="11" (
    setx JAVA_HOME "C:\Program Files\Java\jdk-11.x.x"
    echo Updated JAVA_HOME to Java 11
) else (
    echo Usage: switch-java.bat [11^|17]
)
```

**Usage:**

```cmd
:: Switch to Java 17 for main services
call C:\switch-java.bat 17

:: Switch to Java 11 for BPEL
call C:\switch-java.bat 11
cd C:\Projects\SOA & Microservices\bpel
mvn clean package
```

#### 5. Install Apache Maven

```cmd
:: Download from: https://maven.apache.org/download.cgi
:: Extract to: C:\Programs\apache-maven-3.8.x

:: Add Maven to PATH
setx PATH "C:\Programs\apache-maven-3.8.x\bin;%PATH%"

:: Verify
mvn -version
```

**Expected Output:**

```
Apache Maven 3.8.x (...)
Maven home: C:\Programs\apache-maven-3.8.x
Java version: 17.x.x
```

#### 6. Set CATALINA_HOME for Tomcat

```cmd
:: Download Tomcat 9 from: https://tomcat.apache.org/download-90.cgi
:: Extract to: C:\Projects\apache-tomcat-9.0.116

:: Set CATALINA_HOME
setx CATALINA_HOME "C:\Projects\apache-tomcat-9.0.116"
setx CATALINA_HOME "C:\Projects\apache-tomcat-9.0.116\apache-tomcat-9.0.116"

:: Verify
echo %CATALINA_HOME%
```

---

## Step-by-Step Startup Instructions

### 🚀 Complete Startup Sequence

#### **Phase 1: Infrastructure Setup** (5-10 minutes)

##### Step 1a: Start RabbitMQ Message Broker

```cmd
:: Windows - RabbitMQ as Service (if installed as service)
net start RabbitMQ

:: Or if using Windows Subsystem for Linux (WSL):
wsl sudo service rabbitmq-server start

:: Verify RabbitMQ is running
:: Open browser: http://localhost:15672
:: Username: guest
:: Password: guest
```

**Expected Output:**

```
RabbitMQ Service is starting.
RabbitMQ Service has been started successfully.
```

**Verify Dashboard:**

- Navigate to: http://localhost:15672
- Login: `guest` / `guest`
- Should see: Queues, Exchanges, and connections

##### Step 1b: Configure RabbitMQ with Definitions

```cmd
:: Navigate to rabbitmq definitions
cd C:\Projects\SOA & Microservices\rabbitmq

:: Import RabbitMQ definitions (exchanges, queues, bindings)
:: Using RabbitMQ Management CLI (from RabbitMQ bin directory)
C:\Program Files\RabbitMQ Server\rabbitmq_server-x.x.x\sbin\rabbitmqctl.bat import_definitions definitions.json

:: Verify
:: Open http://localhost:15672
:: Check that queues exist: payments.queue, shipping.queue
```

**Expected Queues:**

- `payments.queue` (with DLQ: payments.dlq)
- `shipping.queue` (with DLQ: shipping.dlq)

**Expected Exchanges:**

- `order.exchange` (topic type)
- `dlx.exchange` (direct type - Dead Letter Exchange)

---

#### **Phase 2: Start Apache Tomcat** (2-3 minutes)

Tomcat hosts **CatalogService** (SOAP), **ODE** (BPEL orchestration), and **jUDDI** registry.

##### Step 2a: Start Tomcat Server

```cmd
:: Navigate to Tomcat bin directory
cd %CATALINA_HOME%\bin

:: Start Tomcat (non-blocking)
startup.bat

:: Or start with terminal window for debugging
catalina.bat run

:: Verify Tomcat started
:: Wait 5-10 seconds for startup
:: Open browser: http://localhost:8080
:: Should see: Tomcat welcome page
```

**Expected Output:**

```
Using CATALINA_BASE:   "C:\Projects\apache-tomcat-9.0.116"
Using CATALINA_HOME:   "C:\Projects\apache-tomcat-9.0.116"
Using CATALINA_TMPDIR: "C:\Projects\apache-tomcat-9.0.116\temp"
Using JRE_HOME:        "C:\Program Files\Java\jdk-17.x.x"
Using CLASSPATH:       "C:\Projects\apache-tomcat-9.0.116\lib\..."
Tomcat started.
```

##### Step 2b: Verify Tomcat Services Are Available

```cmd
:: Check CatalogService WSDL (requires Tomcat + CatalogService deployed)
:: Browser: http://localhost:8080/CatalogService/catalog?wsdl

:: Check ODE Console
:: Browser: http://localhost:8080/ode

:: Check jUDDI Registry
:: Browser: http://localhost:8080/juddi
```

---

#### **Phase 3: Deploy BPEL Process** (3-5 minutes)

The PlaceOrder BPEL orchestration must be deployed to Apache ODE (runs inside Tomcat).

##### Step 3a: Build & Package BPEL

```cmd
:: Navigate to BPEL directory
cd "C:\Projects\SOA & Microservices\bpel"

:: IMPORTANT: Ensure JAVA_HOME is set to Java 11 (or 17, depending on configuration)
:: If using Java 11:
setx JAVA_HOME "C:\Program Files\Java\jdk-11.x.x"

:: Clean build the BPEL process package
mvn clean package -Dcatalina.home=%CATALINA_HOME%

:: Expected output:
:: [INFO] Building zip: C:\Projects\SOA & Microservices\bpel\target\bpel-processes.zip
:: [INFO] Copying bpel-processes.zip to: C:\Projects\apache-tomcat-9.0.116\webapps\ode\WEB-INF\processes\
```

**Check Build Output:**

```cmd
:: Verify ZIP was created in target/
dir C:\Projects\SOA & Microservices\bpel\target\

:: Should see: bpel-processes.zip (or PlaceOrder.zip)
```

##### Step 3b: Verify BPEL Deployment to ODE

```cmd
:: Verify deployment script
C:\Projects\SOA & Microservices\verify-bpel-deployment.bat

:: Or manually check ODE processes directory
dir "%CATALINA_HOME%\webapps\ode\WEB-INF\processes\"

:: Expected: PlaceOrder.zip should exist
```

**Expected Output:**

```
[✓] PlaceOrder.zip found at C:\Projects\apache-tomcat-9.0.116\webapps\ode\WEB-INF\processes\PlaceOrder.zip
[✓] BPEL process deployed successfully
```

##### Step 3c: Check ODE Console

Open browser: http://localhost:8080/ode

- Should show PlaceOrder process
- Status should be: ACTIVE

---

#### **Phase 4: Build & Start Microservices** (5-10 minutes)

All microservices require Java 17.

##### Step 4a: Set Java to 17

```cmd
:: Ensure JAVA_HOME is set to Java 17
setx JAVA_HOME "C:\Program Files\Java\jdk-20"
set JAVA_HOME=C:\Program Files\Java\jdk-20
set PATH=%JAVA_HOME%\bin;%PATH%

echo %JAVA_HOME%
```

##### Step 4b: Build AuthServer (OAuth2)

```cmd
:: Navigate to AuthServer
cd C:\Projects\SOA & Microservices\AuthServer

:: Clean build
mvn clean package -DskipTests

:: Expected output:
:: [INFO] Building jar: C:\Projects\SOA & Microservices\AuthServer\target\auth-server-1.0.0.jar
```

##### Step 4c: Start AuthServer (OAuth2 Authorization)

```cmd
:: Run AuthServer (starts on port 9000)
cd "C:\Projects\SOA & Microservices\AuthServer"
java -jar target/auth-server-1.0.0.jar

:: Or run with Maven
mvn spring-boot:run

:: Expected output (after ~10 seconds):
:: Started AuthServerApplication in X.XXX seconds
:: Tomcat started on port 9000 (http) with context path ''
```

**Verify AuthServer:**

```cmd
:: Open new terminal/PowerShell
curl http://localhost:9000/.well-known/openid-configuration

:: Or use browser: http://localhost:9000/
```

---

##### Step 4d: Build OrdersService (REST)

```cmd
:: Open NEW terminal/PowerShell

:: Navigate to OrdersService
cd C:\Projects\SOA & Microservices\OrdersService

:: Clean build
mvn clean package -DskipTests

:: Expected output:
:: [INFO] Building jar: C:\Projects\SOA & Microservices\OrdersService\target\orders-service-1.0.0.jar
```

##### Step 4e: Start OrdersService

```cmd
:: Run OrdersService (starts on port 8081)
cd "C:\Projects\SOA & Microservices\OrdersService"
java -jar target/orders-service-1.0.0.jar

:: Or run with Maven
mvn spring-boot:run

:: Expected output (after ~10-15 seconds):
:: Started OrdersApplication in X.XXX seconds
:: Tomcat started on port 8081 (http) with context path ''
```

**Verify OrdersService:**

```cmd
:: Open new terminal
curl http://localhost:8081/orders

:: Or Browser: http://localhost:8081/orders
```

---

##### Step 4f: Build & Start PaymentsService (RabbitMQ Consumer)

```cmd
:: Open NEW terminal/PowerShell

:: Navigate to PaymentsService
cd "C:\Projects\SOA & Microservices\PaymentsService"

:: Clean build
mvn clean package -DskipTests

:: Expected output:
:: [INFO] Built jar: C:\Projects\SOA & Microservices\PaymentsService\target\payments-service-1.0.0.jar
```

```cmd
:: Run PaymentsService (starts on port 8082)
cd "C:\Projects\SOA & Microservices\PaymentsService"

java -jar target/payments-service-1.0.0.jar

:: Or run with Maven
mvn spring-boot:run

:: Expected output (after ~10-15 seconds):
:: Started PaymentsServiceApplication in X.XXX seconds
:: Tomcat started on port 8082 (http) with context path ''
:: PaymentsConsumer listening on payments.queue...
```

---

##### Step 4g: Build & Start ShippingService (RabbitMQ Consumer)

```cmd
:: Open NEW terminal/PowerShell

:: Navigate to ShippingService
cd "C:\Projects\SOA & Microservices\ShippingService"

:: Clean build
mvn clean package -DskipTests

:: Expected output:
:: [INFO] Built jar: C:\Projects\SOA & Microservices\ShippingService\target\shipping-service-1.0.0.jar
```

```cmd
:: Run ShippingService (starts on port 8083)
cd "C:\Projects\SOA & Microservices\ShippingService"
java -jar target/shipping-service-1.0.0.jar

:: Or run with Maven
mvn spring-boot:run

:: Expected output (after ~10-15 seconds):
:: Started ShippingServiceApplication in X.XXX seconds
:: Tomcat started on port 8083 (http) with context path ''
:: ShippingConsumer listening on shipping.queue...
```

---

#### **Phase 5: Deploy CatalogService to Tomcat** (2-3 minutes)

CatalogService is deployed as a WAR file to Tomcat (same instance as ODE).

##### Step 5a: Build CatalogService

```cmd
:: Open NEW terminal/PowerShell

:: Navigate to CatalogService
cd "C:\Projects\SOA & Microservices\CatalogService"

:: IMPORTANT: For CatalogService, Java 1.8 is preferred but Java 17 works
:: If you need strict Java 1.8 compliance:
setx JAVA_HOME "C:\Program Files\Java\jdk1.8.x_xxx"
set JAVA_HOME=C:\Program Files\Java\jdk-1.8.0_482


:: Clean build
mvn clean package -DskipTests

:: Expected output:
:: [INFO] Building war: C:\Projects\SOA & Microservices\CatalogService\target\CatalogService.war
```

##### Step 5b: Deploy CatalogService WAR to Tomcat

```cmd
:: Copy WAR to Tomcat webapps
copy C:\Projects\SOA & Microservices\CatalogService\target\CatalogService.war ^
  %CATALINA_HOME%\webapps\

:: Tomcat will auto-deploy the WAR (takes 5-10 seconds)

:: Verify deployment
dir %CATALINA_HOME%\webapps\CatalogService\

:: Expected: WEB-INF, META-INF directories
```

**Verify CatalogService:**

```cmd
:: Open browser or use curl
curl http://localhost:8080/CatalogService/catalog?wsdl

:: Expected: WSDL XML document
```

---

### 📊 Complete Terminal Setup (All Services Running)

You should have **6+ terminals open**:

| Terminal # | Service         | Port | Command                                | Status  |
| ---------- | --------------- | ---- | -------------------------------------- | ------- |
| 1          | RabbitMQ        | 5672 | `net start RabbitMQ`                   | Running |
| 2          | Tomcat          | 8080 | `%CATALINA_HOME%\bin\startup.bat`      | Running |
| 3          | AuthServer      | 9000 | `java -jar auth-server-1.0.0.jar`      | Running |
| 4          | OrdersService   | 8081 | `java -jar orders-service-1.0.0.jar`   | Running |
| 5          | PaymentsService | 8082 | `java -jar payments-service-1.0.0.jar` | Running |
| 6          | ShippingService | 8083 | `java -jar shipping-service-1.0.0.jar` | Running |

---

## Service Details & File Locations

### 1️⃣ AuthServer (OAuth2 Authorization Server)

**Purpose**: Issues JWT tokens for other services

**Technology**: Spring Boot 3.2.3 + Spring Authorization Server  
**Java Version**: **Java 17**  
**Port**: **9000**

**File Locations:**

```
C:\Projects\SOA & Microservices\AuthServer\
├── pom.xml                          (maven config)
└── src\main\
    ├── java\com\globalbooks\auth\
    │   ├── AuthServerApplication.java
    │   └── config\                  (OAuth2, security config)
    └── resources\
        └── application.yml          (port: 9000)
```

**Configuration:**

```yaml
server.port: 9000
spring.application.name: auth-server
```

**Build Command:**

```cmd
cd C:\Projects\SOA & Microservices\AuthServer
mvn clean package -DskipTests
```

**Run Command:**

```cmd
java -jar target/auth-server-1.0.0.jar
:: or
mvn spring-boot:run
```

**Endpoints:**

- Home: http://localhost:9000
- OAuth2 Config: http://localhost:9000/.well-known/openid-configuration
- Token Endpoint: http://localhost:9000/oauth2/token

**Dependencies** (from pom.xml):

- spring-boot-starter-oauth2-authorization-server
- spring-boot-starter-security
- spring-boot-starter-web

---

### 2️⃣ CatalogService (SOAP Web Service)

**Purpose**: Book catalog lookups via SOAP  
**Technology**: JAX-WS (Metro) + Servlet  
**Java Version**: **Java 1.8** (compatible with 17)  
**Deployment**: **Tomcat 9**  
**URL**: http://localhost:8080/CatalogService

**File Locations:**

```
C:\Projects\SOA & Microservices\CatalogService\
├── pom.xml                          (maven config)
└── src\main\
    ├── java\com\globalbooks\catalog\
    │   ├── CatalogPortType.java     (SEI - Service Endpoint Interface)
    │   ├── CatalogServiceImpl.java   (implementation)
    │   ├── model\                   (Book, PriceResponse entities)
    │   ├── exception\                (BookNotFoundException)
    │   └── security\WsSecurityHandler.java  (SOAP WS-Security)
    └── webapp\WEB-INF\
        ├── sun-jaxws.xml            (JAX-WS endpoint mapping)
        ├── web.xml                  (servlet configuration)
        └── wsdl\
            ├── catalog.wsdl         (service contract)
            └── CatalogService.wsdl  (generated)
```

**Build Command:**

```cmd
cd C:\Projects\SOA & Microservices\CatalogService
mvn clean package -DskipTests
```

**Expected Output:**

```
[INFO] Building war: ...\CatalogService\target\CatalogService.war
```

**Deploy to Tomcat:**

```cmd
copy %CD%\target\CatalogService.war %CATALINA_HOME%\webapps\
```

**WSDL Location:** http://localhost:8080/CatalogService/catalog?wsdl

**SOAP Endpoints:**

- GetBook: http://localhost:8080/CatalogService/catalog (HTTP POST)
- GetBookPrice: http://localhost:8080/CatalogService/catalog (HTTP POST)
- SearchBooks: http://localhost:8080/CatalogService/catalog (HTTP POST)

**Test SOAP Request:**

```xml
<!-- File: C:\Projects\SOA & Microservices\testing\soap\test_get_book_by_id.xml -->
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:glo="http://globalbooks.com/catalog">
   <soapenv:Header/>
   <soapenv:Body>
      <glo:GetBook>
         <bookId>101</bookId>
      </glo:GetBook>
   </soapenv:Body>
</soapenv:Envelope>
```

**Dependencies** (from pom.xml):

- jaxws-rt (Metro JAX-WS implementation)
- javax.servlet-api (provided by Tomcat)

---

### 3️⃣ OrdersService (REST Microservice)

**Purpose**: Order management REST API  
**Technology**: Spring Boot 3.2.3 + REST + RabbitMQ  
**Java Version**: **Java 17**  
**Port**: **8081**

**File Locations:**

```
C:\Projects\SOA & Microservices\OrdersService\
├── pom.xml                          (maven config)
├── Dockerfile                       (docker image)
└── src\main\
    ├── java\com\globalbooks\orders\
    │   ├── OrdersApplication.java
    │   ├── controller\OrderController.java
    │   ├── service\OrderService.java
    │   ├── model\                   (Order, OrderItem, OrderStatusEvent)
    │   ├── messaging\OrderEventPublisher.java  (sends to RabbitMQ)
    │   ├── security\OAuth2ResourceServerConfig.java
    │   └── config\RabbitMQConfig.java
    └── resources\
        └── application.yml          (port: 8081, OAuth2, RabbitMQ config)
```

**Configuration:**

```yaml
server.port: 8081
spring.application.name: orders-service
spring.security.oauth2.resourceserver.jwt.issuer-uri: http://localhost:9000
spring.rabbitmq.host: localhost
spring.rabbitmq.port: 5672
spring.rabbitmq.username: guest
spring.rabbitmq.password: guest
rabbitmq.exchange: order.exchange
rabbitmq.routing-key.payment: order.payment
```

**Build Command:**

```cmd
cd C:\Projects\SOA & Microservices\OrdersService
mvn clean package -DskipTests
```

**Run Command:**

```cmd
java -jar target/orders-service-1.0.0.jar
:: or
mvn spring-boot:run
```

**REST API Endpoints:**

- GET http://localhost:8081/orders - List all orders
- POST http://localhost:8081/orders - Create new order
- GET http://localhost:8081/orders/{id} - Get order details
- PUT http://localhost:8081/orders/{id} - Update order
- DELETE http://localhost:8081/orders/{id} - Delete order

**OpenAPI Spec:**

- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/api-docs
- OpenAPI YAML: [docs/openapi.yaml](docs/openapi.yaml)

**RabbitMQ Integration:**

- Publishes messages to: `order.exchange` with routing key `order.payment`
- Consumed by: PaymentsService

**Dependencies** (from pom.xml):

- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-oauth2-resource-server
- spring-boot-starter-amqp
- jackson-databind

**Example Order Creation:**

```json
{
  "customerId": "CUST123",
  "items": [
    {
      "bookId": "101",
      "quantity": 2,
      "price": 15.99
    }
  ],
  "totalAmount": 31.98,
  "shippingAddress": "123 Main St, City, State"
}
```

---

### 4️⃣ PaymentsService (RabbitMQ Async Consumer)

**Purpose**: Process order payments (async via RabbitMQ)  
**Technology**: Spring Boot 3.2.3 + AMQP + RabbitMQ Consumer  
**Java Version**: **Java 17**  
**Port**: **8082**

**File Locations:**

```
C:\Projects\SOA & Microservices\PaymentsService\
├── pom.xml                          (maven config)
├── Dockerfile                       (docker image)
└── src\main\
    ├── java\com\globalbooks\payments\
    │   ├── PaymentsServiceApplication.java
    │   ├── consumer\PaymentsConsumer.java  (listens to payments.queue)
    │   ├── model\                   (PaymentEvent, PaymentStatus)
    │   └── service\PaymentProcessor.java
    └── resources\
        └── application.yml          (port: 8082, RabbitMQ config)
```

**Configuration:**

```yaml
server.port: 8082
spring.application.name: payments-service
spring.rabbitmq.host: localhost
spring.rabbitmq.port: 5672
spring.rabbitmq.username: guest
spring.rabbitmq.password: guest
rabbitmq.exchange: order.exchange
rabbitmq.routing-key.shipping: order.shipping
```

**Build Command:**

```cmd
cd C:\Projects\SOA & Microservices\PaymentsService
mvn clean package -DskipTests
```

**Run Command:**

```cmd
java -jar target/payments-service-1.0.0.jar
:: or
mvn spring-boot:run
```

**RabbitMQ Queue:**

- Listens to: `payments.queue` (routing key: `order.payment`)
- Publishes to: `order.exchange` with routing key `order.shipping` (for ShippingService)
- Dead Letter Queue: `payments.dlq`

**Message Flow:**

```
OrdersService
  → publishes to order.exchange (routing key: order.payment)
    → routed to payments.queue
      → PaymentsConsumer processes
        → publishes to order.exchange (routing key: order.shipping)
```

**Retry Configuration:**

- Max attempts: 3
- Initial interval: 1000ms
- Exponential backoff multiplier: 2.0
- Max interval: 10000ms
- After max retries: sends to DLQ

**Dependencies** (from pom.xml):

- spring-boot-starter-amqp
- spring-boot-starter-web

---

### 5️⃣ ShippingService (RabbitMQ Async Consumer)

**Purpose**: Process order shipments (async via RabbitMQ)  
**Technology**: Spring Boot 3.2.3 + AMQP + RabbitMQ Consumer  
**Java Version**: **Java 17**  
**Port**: **8083**

**File Locations:**

```
C:\Projects\SOA & Microservices\ShippingService\
├── pom.xml                          (maven config)
├── Dockerfile                       (docker image)
└── src\main\
    ├── java\com\globalbooks\shipping\
    │   ├── ShippingServiceApplication.java
    │   ├── consumer\ShippingConsumer.java  (listens to shipping.queue)
    │   ├── model\                   (ShippingEvent, TrackingInfo)
    │   ├── service\ShipmentProcessor.java
    │   └── carrier\                 (CarrierIntegration)
    └── resources\
        └── application.yml          (port: 8083, RabbitMQ config)
```

**Configuration:**

```yaml
server.port: 8083
spring.application.name: shipping-service
spring.rabbitmq.host: localhost
spring.rabbitmq.port: 5672
spring.rabbitmq.username: guest
spring.rabbitmq.password: guest
```

**Build Command:**

```cmd
cd C:\Projects\SOA & Microservices\ShippingService
mvn clean package -DskipTests
```

**Run Command:**

```cmd
java -jar target/shipping-service-1.0.0.jar
:: or
mvn spring-boot:run
```

**RabbitMQ Queue:**

- Listens to: `shipping.queue` (routing key: `order.shipping`)
- Dead Letter Queue: `shipping.dlq`

**Message Flow:**

```
PaymentsService
  → publishes to order.exchange (routing key: order.shipping)
    → routed to shipping.queue
      → ShippingConsumer processes
        → updates shipment status
```

**Retry Configuration:**

- Max attempts: 3
- Initial interval: 1000ms
- Exponential backoff multiplier: 2.0
- Max interval: 10000ms
- After max retries: sends to DLQ

**Dependencies** (from pom.xml):

- spring-boot-starter-amqp
- spring-boot-starter-web

---

### 6️⃣ BPEL Orchestration Process (PlaceOrder)

**Purpose**: Orchestrates order placement workflow combining SOAP & REST services  
**Technology**: Apache ODE (BPEL 2.0) + WSDL  
**Java Version**: **Java 11** (can use 17)  
**Runtime**: **Tomcat 9** (as ODE WAR)  
**URL**: http://localhost:8080/ode

**File Locations:**

```
C:\Projects\SOA & Microservices\bpel\
├── pom.xml                          (maven config for BPEL build)
├── src\main\resources\
│   ├── PlaceOrder.bpel              (BPEL process definition)
│   ├── deploy.xml                   (ODE deployment descriptor)
│   ├── PlaceOrder.wsdl              (process WSDL interface)
│   ├── orders.wsdl                  (OrdersService WSDL for invocation)
│   ├── orders.endpoint              (HTTP endpoint URL for OrdersService)
│   ├── catalog.wsdl                 (CatalogService WSDL for invocation)
│   ├── order-contract.xsd           (XML schema for order data)
│   └── index.html
└── src\assembly\
    └── ode-package.xml              (Maven assembly descriptor)
```

**Build Command:**

```cmd
cd C:\Projects\SOA & Microservices\bpel

:: Ensure JAVA_HOME is set to Java 11 or 17
setx JAVA_HOME "C:\Program Files\Java\jdk-17.x.x"

:: Clean package the BPEL process
mvn clean package -Dcatalina.home=%CATALINA_HOME%
```

**Expected Build Output:**

```
[INFO] Building zip: C:\Projects\SOA & Microservices\bpel\target\bpel-processes.zip
[INFO] Copying PlaceOrder.zip to ODE processes directory...
[INFO] BUILD SUCCESS
```

**Process Flow (PlaceOrder.bpel):**

```
1. Receive order from OrdersService
   ↓
2. Validate order (invoke CatalogService SOAP for book details)
   ↓
3. Process payment (invoke PaymentsService REST)
   ↓
4. Return order placement result
```

**Deployment:**

- Automatically copies to: `%CATALINA_HOME%\webapps\ode\WEB-INF\processes\PlaceOrder.zip`
- ODE auto-deploys within 10 seconds of Tomcat restart

**ODE Console:** http://localhost:8080/ode

- Shows process status
- History of process instances
- Activity tracking

**Dependencies:**

- BPEL variables, flows, activities
- WSDL service invocations
- XML Schema for data contracts

---

## Verification Checklist

### ✅ Pre-Startup Verification

```cmd
:: 1. Verify Java is installed
java -version
:: Expected: java version "17.x.x" or "11.x.x"

:: 2. Verify Maven is installed
mvn -version
:: Expected: Apache Maven 3.8.x

:: 3. Verify Tomcat path is accessible
dir %CATALINA_HOME%
:: Expected: bin, catalina.sh (or catalina.bat on Windows)

:: 4. Verify RabbitMQ service is available
sc query RabbitMQ
:: Expected: SERVICE_NAME: RabbitMQ
:: STATE           : 4  RUNNING

:: 5. Verify ports are not in use
netstat -ano | findstr "8080 8081 8082 8083 5672 9000"
:: If ports show, existing services are running (stop them first)
```

### ✅ Infrastructure Verification

After starting RabbitMQ:

```cmd
:: Check RabbitMQ is listening
netstat -ano | findstr "5672"
:: Expected: TCP    127.0.0.1:5672    0.0.0.0:0       LISTENING    <PID>

:: Open RabbitMQ Management Console
start http://localhost:15672
:: Login: guest / guest
:: Should see: Connections, Exchanges (order.exchange), Queues
```

After starting Tomcat:

```cmd
:: Check Tomcat is listening
netstat -ano | findstr "8080"
:: Expected: TCP    0.0.0.0:8080     0.0.0.0:0       LISTENING    <PID>

:: Open Tomcat home page
start http://localhost:8080
:: Should see: "Tomcat -> Default Welcome Page"

:: Check CatalogService is deployed (after deployment)
start http://localhost:8080/CatalogService
:: Should see SOAP service information or redirect to WSDL

:: Check ODE is deployed
start http://localhost:8080/ode
:: Should see ODE console with processes list
```

### ✅ Microservices Verification

```cmd
:: Check AuthServer (port 9000)
curl http://localhost:9000
curl http://localhost:9000/.well-known/openid-configuration

:: Check OrdersService (port 8081)
curl http://localhost:8081/orders

:: Check PaymentsService (port 8082)
curl http://localhost:8082/actuator/health

:: Check ShippingService (port 8083)
curl http://localhost:8083/actuator/health
```

### ✅ Complete System Verification

**Terminal Checklist:**

```cmd
:: Terminal 1: RabbitMQ
sc query RabbitMQ | findstr "STATE"
:: Expected: STATE           : 4  RUNNING

:: Terminal 2: Tomcat
tasklist | findstr "java"
:: Expected: java.exe    <PID>    ...

:: Terminal 3: AuthServer
curl -s http://localhost:9000 | findstr "title"
:: Expected: should return 200 OK

:: Terminal 4: OrdersService
curl -s http://localhost:8081/orders | findstr -c "[]" -c "error"
:: Expected: [] or error (depending on auth)

:: Terminal 5: PaymentsService
curl -s http://localhost:8082/actuator/health | findstr "UP"
:: Expected: UP

:: Terminal 6: ShippingService
curl -s http://localhost:8083/actuator/health | findstr "UP"
:: Expected: UP
```

---

## Troubleshooting

### ❌ Problem: "JAVA_HOME is not set"

**Symptom:**

```
'JAVA_HOME' is not recognized as an internal or external command
```

**Solution:**

```cmd
:: Check if JAVA_HOME is set
echo %JAVA_HOME%

:: If empty, set it
setx JAVA_HOME "C:\Program Files\Java\jdk-17.x.x"

:: Verify
java -version
```

---

### ❌ Problem: "Port 8080 is already in use"

**Symptom:**

```
Address already in use: bind
```

**Solution:**

```cmd
:: Find process using port 8080
netstat -ano | findstr ":8080"
:: Output: TCP    0.0.0.0:8080     0.0.0.0:0       LISTENING    1234

:: Kill the process (replace 1234 with actual PID)
taskkill /PID 1234 /F

:: Or change Tomcat port in: %CATALINA_HOME%\conf\server.xml
:: Find: <Connector port="8080" ... />
:: Change to: <Connector port="8081" ... />
```

---

### ❌ Problem: "RabbitMQ: Connection refused"

**Symptom:**

```
ERROR: Connection refused: localhost:5672
```

**Solution:**

```cmd
:: Start RabbitMQ service
net start RabbitMQ

:: Verify RabbitMQ is running
sc query RabbitMQ

:: Check RabbitMQ is listening
netstat -ano | findstr "5672"
```

---

### ❌ Problem: "PlaceOrder.zip not found in ODE directory"

**Symptom:**

```
[✗] FAILED: PlaceOrder.zip NOT FOUND at C:\...\webapps\ode\WEB-INF\processes\
```

**Solution:**

See [DEPLOYMENT_TROUBLESHOOTING.md](DEPLOYMENT_TROUBLESHOOTING.md) for detailed diagnostics.

```cmd
:: 1. Rebuild BPEL
cd C:\Projects\SOA & Microservices\bpel
mvn clean package -Dcatalina.home=%CATALINA_HOME%

:: 2. Check build output
dir target\

:: 3. Verify Tomcat path
echo %CATALINA_HOME%
dir %CATALINA_HOME%\webapps\ode\WEB-INF\processes\

:: 4. Manually copy if needed
copy C:\Projects\SOA & Microservices\bpel\target\bpel-processes.zip ^
  %CATALINA_HOME%\webapps\ode\WEB-INF\processes\PlaceOrder.zip

:: 5. Restart Tomcat
%CATALINA_HOME%\bin\shutdown.bat
timeout /t 5
%CATALINA_HOME%\bin\startup.bat
```

---

### ❌ Problem: "Tomcat service fails to start"

**Symptom:**

```
Tomcat service failed to start
```

**Solution:**

```cmd
:: 1. Check Tomcat logs
type %CATALINA_HOME%\logs\catalina.out

:: 2. Run Tomcat in console mode to see errors
cd %CATALINA_HOME%\bin
catalina.bat run

:: 3. Check Java is correctly installed
java -version

:: 4. Check CATALINA_HOME is correct
echo %CATALINA_HOME%
```

---

### ❌ Problem: "Spring Boot service won't start - port conflicts"

**Symptom:**

```
Address already in use: Port 8081 (OrdersService)
```

**Solution:**

```cmd
:: Check which service is using the port
netstat -ano | findstr "8081"

:: Kill conflicting process
taskkill /PID <PID> /F

:: Or run services on different ports (modify application.yml):
::   OrdersService: port 8181
::   PaymentsService: port 8182
::   ShippingService: port 8183
```

---

### ❌ Problem: "OAuth2 token validation fails"

**Symptom:**

```
Error: invalid_token
```

**Troubleshooting:**

```cmd
:: 1. Verify AuthServer is running on port 9000
curl http://localhost:9000

:: 2. Verify issuer-uri is correct in OrdersService application.yml
:: Should be: http://localhost:9000

:: 3. Get a new token from AuthServer
curl -X POST http://localhost:9000/oauth2/token ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "grant_type=client_credentials&client_id=<id>&client_secret=<secret>"

:: 4. Use token to access OrdersService
curl -H "Authorization: Bearer <token>" http://localhost:8081/orders
```

---

### ❌ Problem: "RabbitMQ messages not being consumed"

**Symptom:**

```
Messages piling up in payments.queue or shipping.queue
```

**Troubleshooting:**

```cmd
:: 1. Verify consumers are listening
:: PaymentsService and ShippingService logs should show:
::   "Listening on payments.queue"
::   "Listening on shipping.queue"

:: 2. Check RabbitMQ Management Console
start http://localhost:15672
:: Login: guest/guest
:: Navigate to Queues
:: Should show: payments.queue, shipping.queue with 0 messages (if consuming)

:: 3. Check consumers tab
:: Should show PaymentsService and ShippingService consumers

:: 4. Restart consumers
:: Kill PaymentsService and ShippingService Java processes
tasklist | findstr "java"
taskkill /PID <PaymentsService_PID> /F
taskkill /PID <ShippingService_PID> /F

:: 5. Restart services
:: In new terminals:
java -jar C:\...\PaymentsService\target\payments-service-1.0.0.jar
java -jar C:\...\ShippingService\target\shipping-service-1.0.0.jar
```

---

### ❌ Problem: "CatalogService WSDL binding mismatch with BPEL"

**Symptom:**

```
BPEL invokes CatalogService but returns "Invalid binding"
```

**Solution:**

```cmd
:: 1. Check actual CatalogService WSDL
curl http://localhost:8080/CatalogService/catalog?wsdl > actual-catalog.wsdl

:: 2. Compare with BPEL's version
diff actual-catalog.wsdl C:\...\bpel\src\main\resources\catalog.wsdl

:: 3. Update BPEL's catalog.wsdl if needed
copy actual-catalog.wsdl C:\...\bpel\src\main\resources\catalog.wsdl

:: 4. Rebuild BPEL
cd C:\...\bpel
mvn clean package -Dcatalina.home=%CATALINA_HOME%

:: 5. Restart Tomcat
%CATALINA_HOME%\bin\shutdown.bat
timeout /t 5
%CATALINA_HOME%\bin\startup.bat
```

---

## 📚 Additional Resources

- **README.md**: Project overview and architecture
- **DEPLOYMENT_TROUBLESHOOTING.md**: Detailed BPEL deployment diagnostics
- **docs/SOA_Architecture_Design.md**: Architecture diagrams and design patterns
- **docs/UDDI_Registry_Metadata.xml**: Service registry metadata
- **docs/Governance_Policy.md**: SOA governance rules
- **docs/openapi.yaml**: OpenAPI 3.0 specification for OrdersService

---

## 💡 Quick Reference Commands

```cmd
:: Build all services
cd C:\Projects\SOA & Microservices
for /d %s in (AuthServer OrdersService PaymentsService ShippingService CatalogService) do (
    cd %s
    mvn clean package -DskipTests
    cd ..
)

:: Start all services (requires 6+ terminals)
:: Terminal 1: net start RabbitMQ
:: Terminal 2: %CATALINA_HOME%\bin\startup.bat
:: Terminal 3: cd AuthServer && java -jar target/auth-server-1.0.0.jar
:: Terminal 4: cd OrdersService && java -jar target/orders-service-1.0.0.jar
:: Terminal 5: cd PaymentsService && java -jar target/payments-service-1.0.0.jar
:: Terminal 6: cd ShippingService && java -jar target/shipping-service-1.0.0.jar

:: Full system health check
curl http://localhost:9000/.well-known/openid-configuration && ^
curl http://localhost:8080 && ^
curl http://localhost:8081/orders && ^
curl http://localhost:8082/actuator/health && ^
curl http://localhost:8083/actuator/health

:: Deploy CatalogService to Tomcat
cd CatalogService && ^
mvn clean package -DskipTests && ^
copy target\CatalogService.war %CATALINA_HOME%\webapps\

:: Clean all build artifacts
for /d %s in (AuthServer OrdersService PaymentsService ShippingService CatalogService bpel) do (
    cd %s
    mvn clean
    cd ..
)
```

---

**Last Updated**: March 27, 2026  
**Version**: 1.0  
**Author**: GlobalBooks Development Team
