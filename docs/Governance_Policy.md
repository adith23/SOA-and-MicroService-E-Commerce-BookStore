# GlobalBooks SOA Governance Policy

**Document ID:** GOV-001  
**Version:** 1.0  
**System:** GlobalBooks SOA Platform  
**Applies To:** CatalogService, OrdersService, PaymentsService, ShippingService, PlaceOrder BPEL Process

## 1. Purpose
This policy defines how GlobalBooks services are versioned, secured, monitored, changed, and retired. Since the system is split into multiple independent services, governance is needed to avoid breaking integrations and to keep service evolution controlled.

## 2. Scope
This policy applies to:
- `CatalogService` SOAP service
- `OrdersService` REST API
- `PaymentsService` and `ShippingService` RabbitMQ consumers
- `PlaceOrder` BPEL orchestration
- related contracts, registry metadata, and security policies

## 3. Versioning Strategy

### 3.1 SOAP Services
SOAP versions must be embedded in the WSDL and XML namespace.

```xml
targetNamespace="http://catalog.globalbooks.com/v1"
targetNamespace="http://catalog.globalbooks.com/v2"
```

Rules:
- breaking changes require a new namespace such as `/v2`
- non-breaking changes may stay in the same version if backward compatibility is preserved
- old clients using `v1` must continue to work until retirement

### 3.2 REST Services
REST versions must be exposed in the URL path.

```text
/api/v1/orders
/api/v2/orders
```

Rules:
- breaking changes require a new major path such as `/api/v2/`
- non-breaking changes can stay in the same version
- each public REST version must have matching OpenAPI documentation

### 3.3 Messaging Contracts
For RabbitMQ integrations, exchange names and routing keys must remain stable unless a breaking event change is introduced. If the payload changes in a breaking way, a new event version or routing key must be created.

## 4. SLA Targets

### 4.1 Availability
| Service | Availability Target |
|---------|---------------------|
| CatalogService | 99.9% |
| OrdersService | 99.95% |
| PaymentsService | 99.99% |
| ShippingService | 99.9% |

### 4.2 Response Time
| Service | Target |
|---------|--------|
| CatalogService | p95 under 200 ms |
| OrdersService | p95 under 300 ms |
| PaymentsService | Asynchronous |
| ShippingService | Asynchronous |

### 4.3 Operational Limits
| Component | Limit |
|-----------|-------|
| CatalogService timeout | 5 s |
| OrdersService timeout | 10 s |
| Queue message TTL | 30,000 ms |

## 5. Deprecation Strategy
The deprecation lifecycle is:

```text
Active -> Deprecated -> Sunset Window -> Retired
```

Rules:
- every retiring version must receive at least **6 months notice**
- a replacement version must be published before the old one is retired
- deprecated SOAP contracts remain available during the notice period
- deprecated REST APIs should be documented and marked with deprecation/sunset headers
- registry metadata must be updated when a service becomes deprecated or retired

Retirement process:
1. Submit and approve a change request.
2. Publish the replacement version.
3. Notify consumers and allow a 6-month migration window.
4. Mark the old version as deprecated.
5. Retire the old version after the sunset date.

## 6. Security and QoS Governance
| Area | Policy |
|------|--------|
| CatalogService | Secured with WS-Security UsernameToken |
| OrdersService | Secured with OAuth2 JWT bearer tokens |
| Messaging QoS | Use durable queues, publisher confirms, retry with backoff, DLQs, and message TTL |

## 7. Registry and Change Governance
- SOAP services must publish WSDL metadata and be discoverable through the service registry
- service metadata must include owner, version, endpoint, contract location, and lifecycle status
- contract or endpoint changes must be reflected in registry records within 5 business days
- all public contract changes must be reviewed, and breaking changes must create a new version

## 8. Roles and Responsibilities
| Role | Responsibility |
|------|---------------|
| Solution Architect | Approves service boundaries and major version changes |
| Service Developer | Implements and tests services according to policy |
| Integration Developer | Maintains BPEL flows and message routing |
| Security Engineer | Reviews WS-Security and OAuth2 controls |
| Operations/Governance Reviewer | Monitors SLAs, lifecycle status, and retirement readiness |

## 9. Summary
This governance policy ensures that GlobalBooks services can evolve safely without disrupting consumers. SOAP services are versioned through namespaces, REST services through URL paths, and messaging contracts through controlled event evolution. SLA targets define expected reliability, and the deprecation strategy ensures that service retirement happens in a predictable and well-managed way.
