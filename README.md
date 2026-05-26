# eai-shipment-api

Spring Boot backend portfolio project that models an EAI shipment request flow between ERP and WMS.

The first MVP does not implement real ERP, WMS, Kafka, Redis, JWT, or Docker integration. It focuses on one Spring Boot API server that can receive, store, search, update, fail, and retry shipment requests.

## Tech Stack

- Java 21
- Spring Boot 3.x
- Gradle
- Spring Web
- Spring Data JPA
- H2 Database
- Bean Validation
- springdoc-openapi / Swagger UI
- Static HTML/CSS/JavaScript UI

## Main Features

- Create shipment request
- Get shipment list
- Get shipment detail
- Filter shipments by status
- Update shipment status
- Retry failed shipment
- Common API response format
- Global exception handling
- Simple operation UI

## Business Flow

```text
ERP -> EAI API -> DB
          |
          +-- status = RECEIVED

Operator/WMS simulation -> EAI API -> status update
          |
          +-- PROCESSING / SUCCESS / FAILED
          +-- retry only FAILED shipments
```

Current MVP simulates WMS-side status changes through Swagger or the static UI.

## Shipment Status

| Status | Meaning |
| --- | --- |
| RECEIVED | Shipment request received |
| PROCESSING | Processing |
| SUCCESS | Processed successfully |
| FAILED | Processing failed |

When status is changed to `FAILED`, `message` is used as the failure reason. When status changes to a non-FAILED status, the message is cleared.

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

## Request Examples

Create shipment:

```http
POST /api/shipments
Content-Type: application/json
```

```json
{
  "shipmentNo": "SHP-20260522-001",
  "orderNo": "ORD-20260522-001",
  "requestedAt": "2026-05-22T09:00:00",
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

## Run

```powershell
.\gradlew bootRun
```

Build:

```powershell
.\gradlew clean build
```

## URLs

- UI: `http://localhost:8080/index.html`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console`

H2 connection:

```text
JDBC URL: jdbc:h2:file:D:/h2/eai-shipment-api/shipmentdb
User Name: sa
Password:
```

## Package Structure

```text
com.eaishipment
├── config
├── global
│   ├── exception
│   └── response
└── shipment
    ├── controller
    ├── dto
    ├── entity
    ├── mapper
    ├── repository
    └── service
```

## Design Notes

- The database keeps a single `shipment_request` table for the MVP.
- Java domain classes use `@Embeddable` and `@Embedded` value objects.
- API responses use the shared `ApiResponse<T>` format.
- Entities are not exposed directly to the API response.
- DTO mapping is handled through `ShipmentRequestMapper`.
- Status updates rely on JPA dirty checking inside a transaction.
- Retry is currently simulated as `FAILED -> SUCCESS`; later stages can replace this with real WMS communication or message queue processing.

## Roadmap

- Add focused unit and integration tests
- Improve Swagger examples and schema documentation
- Split frontend files when UI grows further
- Add asynchronous messaging flow with Kafka or another queue
- Add authentication and authorization
- Add Docker-based local runtime
- Move from H2 to an operation-grade database
