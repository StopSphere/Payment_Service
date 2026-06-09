# Payment Service

A Spring Boot microservice that processes payments for orders in the ShopSphere e-commerce platform. It consumes inventory reservation events, processes payments, and publishes payment status events to orchestrate the distributed order workflow using event-driven architecture.

## Service Overview

The Payment Service handles payment processing for reserved inventory. Upon receiving an `InventoryReservedEvent`, it creates a payment transaction with PENDING status, simulates payment processing, and publishes either a `PaymentSuccessEvent` (allowing order fulfillment) or `PaymentFailedEvent` with inventory release (triggering order cancellation). The service ensures transactional consistency in the Saga pattern-based distributed transaction flow.

---

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 4.0.6 |
| **Data Access** | Spring Data JPA | - |
| **Messaging** | Apache Kafka | - |
| **Service Registry** | Netflix Eureka | - |
| **Database** | MySQL | 8 |
| **Utilities** | Lombok | - |
| **Testing** | JUnit 5, Mockito | - |

---

## Kafka Event Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                  PAYMENT SERVICE KAFKA TOPOLOGY                     │
└─────────────────────────────────────────────────────────────────────┘

INVENTORY RESERVED (from Inventory Service):
  InventoryReservedEvent {orderId, productId, quantity, amount}
    ↓
  ┌──────────────────────────────────┐
  │  InventoryReservedConsumer       │
  │  ├─ Topic: "inventory-reserved"  │
  │  ├─ Consumer Group: payment-service-group
  │  └─ Triggers: processPayment()   │
  └──────────┬───────────────────────┘
             ↓
  ┌──────────────────────────────────────────────────┐
  │  PaymentServiceImpl.processPayment()              │
  │  ├─ Create Payment (status: PENDING)             │
  │  ├─ Save to Database                            │
  │  ├─ Simulate payment processing (80% success)   │
  │  └─ Branch on result...                         │
  └──┬────────────────────────────────────────┬──────┘
     │                                        │
     │ SUCCESS (80% chance)                   │ FAILED (20% chance)
     │                                        │
     ▼                                        ▼
  ┌──────────────────┐         ┌──────────────────────────┐
  │ UPDATE Payment   │         │ UPDATE Payment           │
  │ status: SUCCESS  │         │ status: FAILED           │
  │                  │         │                          │
  │ PUBLISH:         │         │ PUBLISH:                 │
  │ payment-success  │         │ ├─ payment-failed        │
  │ (Order Service   │         │ │ (Order Service         │
  │  continues to    │         │ │  cancels order)        │
  │  fulfillment)    │         │ └─ inventory-release     │
  └──────────────────┘         │    (Inventory Service    │
                               │     releases stock)      │
                               └──────────────────────────┘

EVENT DETAILS:

INPUT:  InventoryReservedEvent
  - orderId (UUID)
  - productId (UUID)
  - quantity (Integer)
  - amount (BigDecimal)

OUTPUT on Success: PaymentSuccessEvent
  - orderId (UUID)
  - amount (BigDecimal)
  - paymentId (UUID)

OUTPUT on Failure: PaymentFailedEvent
  - orderId (UUID)
  - reason (String)
  - amount (BigDecimal)

OUTPUT on Failure: InventoryReleaseEvent
  - orderId (UUID)
  - productId (UUID)
  - quantity (Integer)
```

---

## Key Design Patterns

| Pattern | Description |
|---------|-------------|
| **Saga Pattern** | Part of distributed transaction orchestration. Payment service responds to inventory events and triggers subsequent actions (order fulfillment or inventory release) via event publishing. |
| **Event-Driven Architecture** | Core interaction model; the service is triggered by Kafka events and communicates state changes through published events rather than synchronous calls. |

---

## Payment Processing

The Payment Service simulates payment gateway processing. When an `InventoryReservedEvent` is received:

1. **Create Payment Record** — A new Payment entity is created with status `PENDING` and persisted to the database
2. **Process Payment** — Payment is simulated with an 80% success rate using random number generation
3. **Publish Outcome** — Based on the result:
   - **Success**: Publishes `PaymentSuccessEvent` (order proceeds to fulfillment)
   - **Failure**: Publishes `PaymentFailedEvent` AND `InventoryReleaseEvent` (order is cancelled and inventory is released)

**Payment Statuses:**
- `PENDING` — Payment transaction created, awaiting processing
- `SUCCESS` — Payment processed successfully
- `FAILED` — Payment processing failed

---

## API Endpoints

Currently, the Payment Service operates purely through **event-driven Kafka messaging**. There are no public REST API endpoints exposed. All interactions occur via Kafka topics.

**TODO:** REST endpoints for payment queries (e.g., retrieve payment by order ID) may be added in future iterations.

---

## How to Run

### Prerequisites
- **Java 21** installed
- **Docker** (for dependencies)
- **MySQL 8** on `localhost:3307` (database: `payment_db`)
- **Kafka** on `localhost:9092`
- **Eureka Discovery Server** on `localhost:8761`

### Start the Service

```powershell
# Start Docker services
docker compose up -d

# Build and compile
.\gradlew.bat clean build

# Run the service
.\gradlew.bat bootRun
```

The service will start on **http://localhost:8085** and register with Eureka as `payment-service`.

### Configuration

Key configuration in `src/main/resources/application.yml`:
- **Database**: MySQL at `localhost:3307` (database: `payment_db`)
- **Kafka**: Broker at `localhost:9092`
  - Consumer Group: `payment-service-group`
  - Consumer Topic: `inventory-reserved`
  - Producer Topics: `payment-success`, `payment-failed`, `inventory-release`
- **Eureka**: Registry at `http://localhost:8761/eureka`
- **Server Port**: `8085`

---

## Testing

The service includes unit tests using **JUnit 5** and **Mockito**.

### Test Coverage
- ✓ Payment creation with valid `InventoryReservedEvent`
- ✓ Payment status transitions (PENDING → SUCCESS or FAILED)
- ✓ Kafka event publishing (success and failure scenarios)
- ✓ Database persistence and retrieval
- ✓ Error handling and exception scenarios

### Run Tests

```powershell
# Using Gradle
.\gradlew.bat test

# View test report
# Reports generated in: build/reports/tests/test/index.html
```

---

## Project Structure

```
src/main/java/com/shopsphere/payment_Services/
├── PaymentServicesApplication.java      # Entry point
├── Controller/
│   └── PaymentController.java            # REST endpoints (reserved for future use)
├── Service/
│   ├── PaymentService.java               # Business logic interface
│   └── Impl/
│       └── PaymentServiceImpl.java        # Payment processing implementation
├── Repository/
│   └── PaymentRepository.java            # Data access layer
├── Entity/
│   ├── Payment.java                      # JPA entity
│   └── PaymentStatus.java                # Status enum
├── Kafka/
│   ├── PaymentEventProducer.java         # Publish payment events
│   ├── InventoryReservedEvent.java       # Incoming event
│   ├── PaymentSuccessEvent.java          # Outgoing event (success)
│   ├── PaymentFailedEvent.java           # Outgoing event (failure)
│   ├── InventoryReleaseEvent.java        # Outgoing event (release inventory)
│   └── Consumer/
│       └── InventoryReservedConsumer.java # Consume inventory events
└── Config/
    └── KafkaConfig.java                  # Kafka configuration

src/main/resources/
└── application.yml                       # Configuration
```

---

## Data Model

### Payment Entity

| Field | Type | Description |
|-------|------|-------------|
| `paymentId` | UUID | Primary key, auto-generated |
| `orderId` | UUID | Reference to the order |
| `amount` | BigDecimal | Payment amount |
| `status` | Enum | Payment status (PENDING, SUCCESS, FAILED) |
| `createdAt` | LocalDateTime | Creation timestamp |

---

## Performance Notes

- **Payment Processing**: Near real-time event consumption and processing
- **Message Ordering**: Preserved per order using `orderId` as Kafka partition key
- **Event Publishing**: Non-blocking async publication to Kafka
- **Database**: Connection pooling and indexed UUID queries optimize data retrieval
- **Transactional Consistency**: Payment state transitions are atomic database operations

---

## Error Handling

| Scenario | Behavior |
|----------|----------|
| Kafka consumer unavailable | Service logs error; message remains in queue for retry |
| Database connection failure | Spring Data JPA exception handling; transaction rollback |
| Event deserialization error | Kafka deserializer exception captured by consumer |
| Payment processing exception | Service logs error; creates FAILED payment record |

---

## Integration Points

```
┌────────────────────────────────────────────────────────────┐
│                      PAYMENT SERVICE INTEGRATION           │
└────────────────────────────────────────────────────────────┘

Upstream:
  Inventory Service → [inventory-reserved] → Payment Service
                                              (Consumes)

Downstream:
  Payment Service → [payment-success] → Order Service
                 → [payment-failed] → Order Service
                 → [inventory-release] → Inventory Service

Service Registry:
  Payment Service ↔ Eureka Discovery (localhost:8761)
  
Database:
  Payment Service ↔ MySQL (localhost:3307/payment_db)

Message Broker:
  Payment Service ↔ Kafka (localhost:9092)
```

---

## Status

✅ Event-driven payment processing via Kafka  
✅ Payment creation and status tracking  
✅ Success and failure path publishing  
✅ Inventory release on payment failure  
✅ Database persistence (MySQL)  
✅ Eureka service registration and discovery  
✅ Unit test coverage with JUnit & Mockito  
⚠️ REST endpoints — Reserved for future implementation  

---

**Built for ShopSphere E-commerce Platform**

