# eai-shipment-api

Spring Boot backend portfolio project that models an EAI shipment request flow between ERP and WMS.

The application receives shipment requests from an ERP-like sender, stores them in a single API database, exposes search/status management APIs, and dispatches shipment work asynchronously through Kafka.

## Tech Stack

- Java 21
- Spring Boot 3.x
- Gradle
- Spring Web
- Spring Data JPA
- H2 Database
- Bean Validation
- springdoc-openapi / Swagger UI
- Spring Kafka
- Docker Compose for local Kafka
- Logback file logging
- Static HTML/CSS/JavaScript UI
- JUnit 5 / Spring Boot Test

## Main Features

- Create shipment request
- Get shipment list
- Get shipment detail
- Filter shipments by status
- Update shipment status
- Retry failed shipment
- Dispatch shipment through Kafka
- Store failure message and error payload
- Common API response format
- Global exception handling
- File-based application and SQL logs
- Simple operation UI

## Business Flow

```text
ERP
 -> EAI API
 -> shipment_request DB insert
 -> status = RECEIVED

Operator/UI dispatch
 -> EAI API
 -> status = PROCESSING
 -> Kafka topic: shipment-dispatch

Kafka Consumer
 -> receives dispatch message
 -> updates shipment status
 -> SUCCESS or FAILED
```

In the current MVP, WMS communication is simulated. If `shipmentNo` contains `FAIL`, the dispatch result becomes `FAILED`; otherwise it becomes `SUCCESS`.

## Shipment Status

| Status | Meaning |
| --- | --- |
| RECEIVED | Shipment request received and waiting for dispatch |
| PROCESSING | Dispatch requested and processing through Kafka |
| SUCCESS | Dispatch processed successfully |
| FAILED | Dispatch or processing failed |

When status changes to `FAILED`, `message` stores the failure reason and `errorPayload` stores the failed Kafka payload. When status changes to a non-FAILED status, message and payload are cleared.

## Kafka Flow

Topic:

```text
shipment-dispatch
```

Message:

```json
{
  "shipmentId": 1,
  "shipmentNo": "SHP-KAFKA-001"
}
```

Producer:

```text
ShipmentDispatchProducer
```

Consumer:

```text
ShipmentDispatchConsumer
```

Dispatch behavior:

```text
POST /api/shipments/{id}/dispatch
 -> validate status is RECEIVED
 -> update status to PROCESSING
 -> publish ShipmentDispatchMessage to Kafka
 -> return PROCESSING response

Kafka consumer
 -> consume ShipmentDispatchMessage
 -> call completeDispatch(id, payload)
 -> update status to SUCCESS or FAILED
```

## Common Response Format

Success:

```json
{
  "resultCode": "S",
  "message": "Shipment list returned",
  "data": {}
}
```

Failure:

```json
{
  "resultCode": "E",
  "message": "Error message",
  "data": null
}
```

## API List

| Method | URL | Description |
| --- | --- | --- |
| POST | `/api/shipments` | Create shipment request |
| GET | `/api/shipments` | Get all shipment requests |
| GET | `/api/shipments/{id}` | Get shipment detail |
| GET | `/api/shipments/status/{status}` | Get shipments by status |
| PATCH | `/api/shipments/{id}/status` | Update shipment status |
| POST | `/api/shipments/{id}/retry` | Retry failed shipment |
| POST | `/api/shipments/{id}/dispatch` | Dispatch shipment through Kafka |

## Request Examples

Create shipment:

```http
POST /api/shipments
Content-Type: application/json
```

```json
{
  "shipmentNo": "SHP-20260611-001",
  "orderNo": "ORD-20260611-001",
  "requestedAt": "2026-06-11T09:00:00",
  "warehouseCode": "WH-SEOUL-01",
  "customerCode": "CUST-001",
  "customerName": "Seoul Distribution",
  "materialCode": "MAT-001",
  "materialName": "Water 500ml",
  "quantity": 100,
  "unit": "EA"
}
```

Update status:

```http
PATCH /api/shipments/1/status
Content-Type: application/json
```

```json
{
  "status": "FAILED",
  "message": "WMS stock shortage"
}
```

Retry failed shipment:

```http
POST /api/shipments/1/retry
```

Dispatch shipment:

```http
POST /api/shipments/1/dispatch
```

## Run

Start Kafka and Kafka UI:

```powershell
docker compose up -d
```

Run Spring Boot:

```powershell
.\gradlew bootRun
```

Build:

```powershell
.\gradlew clean build
```

Run tests:

```powershell
.\gradlew test
```

## URLs

- UI: `http://localhost:8080/index.html`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console`
- Kafka UI: `http://localhost:8081`
- Kafka broker: `localhost:9092`

H2 connection:

```text
JDBC URL: jdbc:h2:file:D:/h2/eai-shipment-api/shipmentdb
User Name: sa
Password:
```

## Logging

Logback is configured through:

```text
src/main/resources/logback-spring.xml
```

Log files:

```text
logs/app/eai-shipment-api.log
logs/sql/hibernate-sql.log
```

Rolling policy:

```text
Max file size: 10MB
Max backup files: 10
```

Log separation:

```text
Application/Kafka business logs
 -> logs/app/eai-shipment-api.log

Hibernate SQL logs
 -> logs/sql/hibernate-sql.log
```

Default production-like level:

```text
com.eaishipment: INFO
org.apache.kafka: WARN
org.hibernate.SQL: DEBUG
org.hibernate.orm.jdbc.bind: WARN
```

For troubleshooting, a specific package can temporarily be changed to `DEBUG` or `TRACE`.

## Package Structure

```text
com.eaishipment
|-- config
|-- global
|   |-- exception
|   `-- response
`-- shipment
    |-- consumer
    |-- controller
    |-- dto
    |-- entity
    |-- event
    |-- mapper
    |-- producer
    |-- repository
    `-- service
```

## Design Notes

- The MVP keeps one main table: `shipment_request`.
- Java domain classes use `@Embeddable` and `@Embedded` value objects.
- API responses use the shared `ApiResponse<T>` format.
- Entities are not exposed directly to API responses.
- DTO mapping is handled through `ShipmentRequestMapper`.
- Status updates rely on JPA dirty checking inside transactions.
- Kafka dispatch returns `PROCESSING` first, then the consumer updates the final status.
- Failed dispatch stores both a human-readable message and the failed Kafka payload.
- Application logs and SQL logs are separated into different rolling files.

## Local Test Scenario

1. Start Kafka:

```powershell
docker compose up -d
```

2. Start Spring Boot:

```powershell
.\gradlew bootRun
```

3. Create a normal shipment:

```text
shipmentNo = SHP-KAFKA-001
```

4. Create a failure shipment:

```text
shipmentNo = SHP-FAIL-KAFKA-001
```

5. Dispatch both shipments:

```text
POST /api/shipments/{id}/dispatch
```

Expected result:

```text
Normal shipment:
PROCESSING -> SUCCESS

Failure shipment:
PROCESSING -> FAILED
message = WMS transmission failed
errorPayload = Kafka dispatch message JSON
```

## Roadmap

- Add Kafka producer send callback handling
- Add scheduler-based dispatch for RECEIVED shipments
- Add Spring Boot Actuator for health and metrics
- Improve operational error history tables
- Add authentication and authorization
- Move from H2 to an operation-grade database
