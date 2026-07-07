# eai-shipment-api

ERP에서 WMS로 출고 지시가 전달되는 상황을 가정한 Spring Boot 기반 EAI 출고 지시 처리 API 프로젝트입니다.

ERP로부터 출고 지시 데이터를 수신하고, DB에 저장한 뒤 조회, 상태 관리, 실패 처리, 재처리, Kafka 기반 비동기 전송, 스케줄러 기반 자동 dispatch까지 처리하는 흐름을 구현했습니다.

현재 MVP에서는 실제 ERP와 WMS 서버를 별도로 구현하지 않습니다. 하나의 Spring Boot 애플리케이션 안에서 API 서버, Kafka Producer, Kafka Consumer를 함께 두고 EAI 처리 흐름을 시뮬레이션합니다.

## 기술 스택

- Java 21
- Spring Boot 4.1.0
- Gradle
- Spring Web
- Spring Data JPA
- H2 File Database
- Bean Validation
- springdoc-openapi / Swagger UI
- Spring Kafka
- Docker Compose
- Apache Kafka
- Kafka UI
- Logback
- Static HTML/CSS/JavaScript UI
- JUnit 5 / Spring Boot Test

## 프로젝트 목적

실무 EAI 운영 업무에서 자주 볼 수 있는 출고 지시 연동 흐름을 Spring Boot 백엔드 프로젝트로 재구성하는 것이 목표입니다.

기본 흐름은 다음과 같습니다.

```text
ERP 출고 지시 생성
-> EAI API 수신
-> DB 적재
-> 전송 대상 선별
-> WMS 전송 요청
-> 성공/실패 상태 반영
-> 실패 건 재처리
```

기존 업무에서 사용하는 상태 기반 배치 처리 방식을 참고하되, 별도 전송 flag 컬럼을 추가하기보다 `ShipmentStatus` 상태값을 중심으로 흐름을 단순화했습니다.

## 전체 처리 흐름

```text
[ERP]
  |
  | POST /api/shipments
  v
[Spring Boot API]
  |
  | DB insert
  v
[shipment_request]
  |
  | status = RECEIVED
  v
[Scheduler or Manual Dispatch]
  |
  | status = PROCESSING
  | publish message
  v
[Kafka Topic: shipment-dispatch]
  |
  | consume message
  v
[ShipmentDispatchConsumer]
  |
  | WMS 처리 시뮬레이션
  v
[DB Status Update]
  |
  | SUCCESS or FAILED
  v
[조회 / 재처리 / 운영 확인]
```

현재는 WMS 서버가 없으므로 `ShipmentDispatchConsumer`가 같은 Spring Boot 애플리케이션 안에서 WMS 처리 결과를 시뮬레이션합니다. 실제 운영 구조에서는 EAI 서버가 Producer, WMS 또는 WMS 연계 서버가 Consumer가 되는 형태로 분리될 수 있습니다.

## 주요 기능

- ERP 출고 지시 수신 API
- 출고 지시 DB 저장
- 출고 지시 목록 조회
- 출고 지시 상세 조회
- 상태별 출고 지시 조회
- 출고 지시 상태 변경
- 실패 건 재처리
- 수동 dispatch
- 스케줄러 기반 자동 dispatch
- Kafka Producer 메시지 발행
- Kafka Consumer 메시지 수신
- 처리 성공/실패 상태 반영
- 장기 PROCESSING 상태 timeout 처리
- 공통 API 응답 포맷
- 전역 예외 처리
- Swagger API 문서
- H2 Console
- Kafka UI
- 애플리케이션 로그 / Hibernate SQL 로그 분리
- 서비스 책임 분리 리팩토링
- 서비스/컨트롤러 테스트

## 도메인 모델

출고 지시는 `ShipmentRequest` 엔티티를 중심으로 관리합니다.

DB는 MVP 기준으로 `shipment_request` 단일 테이블을 사용합니다. Java 코드에서는 역할별 값 객체를 `@Embeddable` / `@Embedded`로 분리했습니다.

```text
ShipmentRequest
├── ShipmentRequestInfo      출고 지시 기본 정보
├── WarehouseInfo            창고 정보
├── CustomerInfo             고객 정보
├── ShipmentItemInfo         품목 정보
├── ShipmentProcessingInfo   처리 상태 정보
└── AuditInfo                생성/수정 시간
```

| 값 객체 | 주요 필드 |
| --- | --- |
| ShipmentRequestInfo | shipmentNo, orderNo, requestedAt |
| WarehouseInfo | warehouseCode |
| CustomerInfo | customerCode, customerName |
| ShipmentItemInfo | materialCode, materialName, quantity, unit |
| ShipmentProcessingInfo | status, retryCount, message, errorPayload, dispatchBatchId |
| AuditInfo | createdAt, updatedAt |

## 상태값

| 상태 | 의미 |
| --- | --- |
| RECEIVED | ERP 출고 지시 수신 완료, WMS 전송 대기 |
| PROCESSING | WMS 전송 요청 또는 처리 중 |
| SUCCESS | 처리 성공 |
| FAILED | 처리 실패 |

기본 성공 흐름은 다음과 같습니다.

```text
RECEIVED -> PROCESSING -> SUCCESS
```

실패 흐름은 다음과 같습니다.

```text
RECEIVED -> PROCESSING -> FAILED -> retry -> SUCCESS
```

`FAILED` 상태가 되면 실패 사유를 `message`에 저장하고, 필요한 경우 실패 당시 payload를 `errorPayload`에 저장합니다. `FAILED`가 아닌 상태로 변경되면 실패 메시지와 payload는 정리합니다.

## 서비스 책임 분리

초기에는 `ShipmentRequestService` 하나에 등록, 조회, dispatch, Consumer 결과 처리, timeout 처리 로직이 함께 있었습니다.

Kafka와 Scheduler 기능이 추가되면서 다음과 같이 책임을 분리했습니다.

```text
shipment/service
├── ShipmentRequestService
├── ShipmentDispatchService
├── ShipmentDispatchResultService
└── ShipmentTimeoutService
```

| 서비스 | 책임 |
| --- | --- |
| ShipmentRequestService | 출고 지시 등록, 조회, 상태 변경, 재처리 |
| ShipmentDispatchService | RECEIVED 대상 dispatch, PROCESSING 변경, Kafka 메시지 발행 요청 |
| ShipmentDispatchResultService | Kafka Consumer 처리 결과를 SUCCESS/FAILED로 반영 |
| ShipmentTimeoutService | 오래된 PROCESSING 건을 FAILED로 전환 |

Producer와 Consumer는 Kafka 입출력에 집중하고, 실제 업무 판단과 DB 상태 변경은 Service 계층에서 처리합니다.

```text
Controller / Scheduler
-> Service
-> Repository / Producer
```

```text
Kafka Consumer
-> DispatchResultService
-> Repository
```

## Kafka 처리 구조

### Topic

```text
shipment-dispatch
```

### Message

```json
{
  "shipmentId": 1,
  "shipmentNo": "SHP-20260703-001",
  "dispatchBatchId": "DISPATCH-..."
}
```

### Producer

```text
ShipmentDispatchProducer
```

역할:

```text
ShipmentDispatchMessage를 Kafka topic에 발행
Kafka 발행 성공/실패를 whenComplete 콜백으로 로그 기록
```

### Consumer

```text
ShipmentDispatchConsumer
```

역할:

```text
Kafka topic에서 메시지 수신
수신 payload를 필요 시 문자열로 보관
ShipmentDispatchResultService 호출
DB 상태를 SUCCESS 또는 FAILED로 변경
```

현재 WMS 시뮬레이션 규칙은 다음과 같습니다.

```text
shipmentNo에 FAIL 포함 -> FAILED
그 외 -> SUCCESS
```

## Scheduler

`@EnableScheduling`과 `@Scheduled`를 사용해 주기적으로 출고 지시를 처리합니다.

기본 cron 설정:

```yaml
shipment:
  dispatch:
    scheduler:
      cron:
        default: "0 */5 * * * *"
    timeout-minutes: 10
```

즉, 서버 시작 시간을 기준으로 5분마다 도는 `fixedRate` 방식이 아니라 서버 시간 기준 매 5분 정각에 실행됩니다.

스케줄러는 다음 작업을 수행합니다.

```text
1. RECEIVED 상태 출고 지시 자동 dispatch
2. 오래된 PROCESSING 상태 timeout 처리
```

### 자동 dispatch

```text
RECEIVED 조회
-> dispatchBatchId 생성
-> 각 출고 지시를 PROCESSING으로 변경
-> Kafka topic에 메시지 발행
```

여러 건 처리 중 일부 건에서 예외가 발생해도 전체 배치를 중단하지 않고 다음 건을 계속 처리합니다.

### PROCESSING timeout

`PROCESSING` 상태가 오래 유지되는 건은 자동 재전송하지 않습니다.

EAI 서버가 WMS에 실제로 보냈는지, WMS가 처리 중인지, Kafka 처리 중 문제가 생겼는지 서버가 확정할 수 없기 때문입니다. 자동 재전송은 중복 출고 위험이 있으므로 timeout 대상은 `FAILED`로 전환하고 운영자 또는 사용자가 확인 후 재처리하도록 설계했습니다.

```text
PROCESSING 상태가 10분 이상 지속
-> FAILED로 전환
-> message = Dispatch timeout
-> 운영자 확인 후 재처리
```

## 스케줄러 제어 API

스케줄러 dispatch는 서버 재시작 없이 enable / disable 할 수 있습니다.

| Method | URL | 설명 |
| --- | --- | --- |
| GET | `/api/shipments/scheduler` | dispatch scheduler 상태 조회 |
| PATCH | `/api/shipments/scheduler/enable` | dispatch scheduler 활성화 |
| PATCH | `/api/shipments/scheduler/disable` | dispatch scheduler 비활성화 |

현재는 `AtomicBoolean` 기반 인메모리 플래그로 관리합니다. 서버 재시작 시 기본값으로 초기화됩니다.

추후 운영 환경에서는 DB 또는 설정 저장소 기반 제어로 확장할 수 있습니다.

## API 목록

| Method | URL | 설명 |
| --- | --- | --- |
| POST | `/api/shipments` | 출고 지시 등록 |
| GET | `/api/shipments` | 출고 지시 목록 조회 |
| GET | `/api/shipments/{id}` | 출고 지시 상세 조회 |
| GET | `/api/shipments/status/{status}` | 상태별 출고 지시 조회 |
| PATCH | `/api/shipments/{id}/status` | 출고 지시 상태 변경 |
| POST | `/api/shipments/{id}/retry` | FAILED 출고 지시 재처리 |
| POST | `/api/shipments/{id}/dispatch` | 수동 dispatch |
| GET | `/api/shipments/scheduler` | 스케줄러 상태 조회 |
| PATCH | `/api/shipments/scheduler/enable` | 스케줄러 활성화 |
| PATCH | `/api/shipments/scheduler/disable` | 스케줄러 비활성화 |

## 요청 예시

### 출고 지시 등록

```http
POST /api/shipments
Content-Type: application/json
```

```json
{
  "shipmentNo": "SHP-20260703-001",
  "orderNo": "ORD-20260703-001",
  "requestedAt": "2026-07-03T09:00:00",
  "warehouseCode": "WH-SEOUL-01",
  "customerCode": "CUST-001",
  "customerName": "Seoul Distribution",
  "materialCode": "MAT-001",
  "materialName": "Water 500ml",
  "quantity": 100,
  "unit": "EA"
}
```

### 상태 변경

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

### 재처리

```http
POST /api/shipments/1/retry
```

### 수동 dispatch

```http
POST /api/shipments/1/dispatch
```

## 공통 응답 포맷

성공 응답:

```json
{
  "resultCode": "S",
  "message": "Shipment list returned",
  "data": {}
}
```

실패 응답:

```json
{
  "resultCode": "E",
  "message": "Error message",
  "data": null
}
```

공통 응답은 `ApiResponse<T>`를 사용합니다.

## 예외 처리

전역 예외 처리는 `GlobalExceptionHandler`에서 담당합니다.

처리 대상 예시:

- 비즈니스 예외
- Validation 실패
- 잘못된 enum 값
- 잘못된 path variable 타입
- 존재하지 않는 출고 지시 조회
- 기타 예상하지 못한 서버 오류

비즈니스 규칙 위반은 `BusinessException`을 사용합니다.

## 로그 관리

Logback 설정 파일:

```text
src/main/resources/logback-spring.xml
```

로그 파일:

```text
logs/app/eai-shipment-api.log
logs/sql/hibernate-sql.log
```

로그 정책:

```text
애플리케이션 로그와 Hibernate SQL 로그 분리
파일 크기 10MB 기준 rolling
최대 10개 파일 보관
WARN 이상 로그는 콘솔에도 출력
```

주요 로그에는 `dispatchBatchId`를 함께 남겨 스케줄러 실행 단위와 Kafka 메시지 흐름을 추적할 수 있도록 했습니다.

예시:

```text
Shipment dispatch requested. shipmentId=1, shipmentNo=SHP-001, dispatchBatchId=DISPATCH-...
Kafka publish succeeded. topic=shipment-dispatch, partition=0, offset=10, ...
Shipment dispatch completed. shipmentId=1, shipmentNo=SHP-001, dispatchBatchId=DISPATCH-...
Stale PROCESSING shipment marked as FAILED. shipmentId=1, ...
```

## 패키지 구조

```text
com.eaishipment
├── config
├── global
│   ├── exception
│   └── response
└── shipment
    ├── consumer
    ├── controller
    ├── dto
    ├── entity
    ├── event
    ├── mapper
    ├── producer
    ├── repository
    ├── scheduler
    └── service
```

## 실행 방법

### 1. Kafka 실행

```powershell
docker compose up -d
```

### 2. Spring Boot 실행

```powershell
.\gradlew bootRun
```

`bootRun`은 서버를 계속 띄워두는 명령이므로 실행 중에는 Gradle 작업이 종료되지 않습니다. 중지하려면 터미널에서 `Ctrl + C`를 누릅니다.

### 3. 빌드

```powershell
.\gradlew clean build
```

### 4. 테스트

```powershell
.\gradlew test
```

## 접속 URL

| 구분 | URL |
| --- | --- |
| 애플리케이션 UI | `http://localhost:8080/index.html` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| H2 Console | `http://localhost:8080/h2-console` |
| Kafka UI | `http://localhost:8081` |
| Kafka Broker | `localhost:9092` |

H2 접속 정보:

```text
JDBC URL: jdbc:h2:file:D:/h2/eai-shipment-api/shipmentdb
User Name: sa
Password:
```

## 테스트 구조

서비스 책임 분리 이후 테스트도 역할별로 분리했습니다.

```text
src/test/java/com/eaishipment/shipment/service
├── ShipmentRequestServiceTest
├── ShipmentDispatchServiceTest
├── ShipmentDispatchResultServiceTest
└── ShipmentTimeoutServiceTest
```

```text
src/test/java/com/eaishipment/shipment/controller
└── ShipmentRequestControllerTest
```

테스트 범위:

- 출고 지시 등록/조회/상태 변경/재처리
- dispatch 시 RECEIVED -> PROCESSING 변경
- Kafka Producer 호출 확인
- Consumer 처리 결과 SUCCESS/FAILED 반영
- 실패 payload 저장 확인
- 오래된 PROCESSING timeout 처리
- Controller 응답 검증

## 로컬 테스트 시나리오

### 정상 처리 케이스

1. Kafka 실행
2. Spring Boot 실행
3. 출고 지시 등록
4. 수동 dispatch 또는 scheduler 실행
5. Kafka Consumer 처리
6. 상태가 `SUCCESS`로 변경되는지 확인

```text
RECEIVED -> PROCESSING -> SUCCESS
```

### 실패 처리 케이스

`shipmentNo`에 `FAIL`이 포함된 출고 지시를 등록합니다.

```text
SHP-FAIL-20260703-001
```

dispatch 후 Consumer가 실패로 처리합니다.

```text
RECEIVED -> PROCESSING -> FAILED
message = WMS transmission failed
errorPayload = Kafka dispatch message JSON
```

### 장기 PROCESSING 처리 케이스

`PROCESSING` 상태가 10분 이상 지속되면 timeout scheduler가 실패로 전환합니다.

```text
PROCESSING -> FAILED
message = Dispatch timeout
```

## 설계 포인트

- DB는 MVP 기준 `shipment_request` 단일 테이블로 단순화했습니다.
- Java 코드는 `@Embedded` 값 객체로 의미 단위를 분리했습니다.
- 엔티티를 API 응답으로 직접 노출하지 않고 DTO로 변환합니다.
- 상태 변경은 Transaction 내부에서 JPA dirty checking을 활용합니다.
- Kafka 발행은 비동기이므로 dispatch API 응답은 최종 성공이 아니라 `PROCESSING` 상태를 의미합니다.
- Kafka 최종 처리 결과는 Consumer가 별도로 DB에 반영합니다.
- 오래된 PROCESSING 건은 자동 재전송하지 않고 FAILED로 전환해 중복 출고 위험을 줄입니다.
- 스케줄러는 cron 기반으로 매 5분 정각 실행되도록 설정했습니다.
- 운영성 API는 추후 Spring Security로 인가 처리를 적용할 예정입니다.

## 향후 개선 로드맵

- Spring Security 도입
- 스케줄러 제어 API 권한 제한
- 수동 dispatch / 상태 변경 / 재처리 API 권한 제한
- Kafka retry 정책 적용
- Dead Letter Topic 도입
- Kafka consumer offset / consumer group 운영 관리 문서화
- 중복 처리 방지를 위한 idempotency 설계
- H2에서 MySQL 또는 PostgreSQL로 전환
- 운영 이력 테이블 추가
- Actuator 기반 health check 추가
- 서비스별 테스트 커버리지 보강
- GitHub Actions 기반 CI 구성
- 아키텍처 다이어그램 이미지 추가

## 현재 구현 상태 요약

```text
출고 지시 수신 API 구현 완료
JPA Entity / Embedded Value Object 설계 완료
조회 / 상태 변경 / 재처리 API 구현 완료
Kafka Producer / Consumer 구현 완료
스케줄러 기반 자동 dispatch 구현 완료
장기 PROCESSING timeout 처리 구현 완료
파일 로그 분리 및 rolling 설정 완료
서비스 책임 분리 리팩토링 완료
서비스 / 컨트롤러 테스트 작성 및 통과 확인
Spring Boot 4.1.0 업그레이드 완료
```