# eai-shipment-api

ERP에서 전달된 출고 지시를 수신하여 상태를 추적하고, Kafka 기반 WMS 전송, 실패·재처리, 장기 처리 timeout 및 OpenAI 기반 장애 분석을 제공하는 Spring Boot·React 기반 EAI 포트폴리오 프로젝트입니다.

현재 프로젝트는 실무에서 경험한 ERP/EAI 연계와 운영 장애 대응 흐름을 재구성한 포트폴리오 목적의 MVP입니다. 실제 ERP/WMS 서버를 별도로 구현하지 않고, 하나의 Spring Boot 애플리케이션 안에서 API 서버, Kafka Producer, Kafka Consumer를 함께 실행하여 EAI 처리 흐름을 시뮬레이션합니다. AI 분석 결과는 실제 장애 원인을 확정하지 않으며 운영자의 확인을 돕는 참고 정보로만 사용합니다.

## 기술 스택

- Java 21
- Spring Boot 4.1.1
- Gradle
- Spring Web
- Spring Data JPA
- Spring Security
- H2 Database
- Bean Validation
- Spring Kafka
- Apache Kafka
- Docker Compose
- Kafka UI
- springdoc-openapi / Swagger UI
- Logback
- React 19 / TypeScript 6 / Vite 8
- OpenAI Java SDK 4.52.0
- JUnit 5 / Spring Boot Test

## 주요 기능

- ERP 출고 지시 수신 API
- 출고 지시 등록, 목록 조회, 상세 조회
- 상태별 출고 지시 조회
- Dispatch, Consumer 결과, Timeout, Retry 기반 상태 전이 관리
- 실패 건 재처리
- 수동 dispatch
- 스케줄러 기반 자동 dispatch
- Kafka Producer 메시지 발행
- Kafka Consumer 메시지 수신 및 처리 결과 반영
- 장기 PROCESSING 상태 timeout 처리
- x-api-key 기반 API 보호
- 공통 API 응답 포맷
- 전역 예외 처리
- 애플리케이션 로그 / Hibernate SQL 로그 분리
- React + TypeScript 기반 출고 관리 UI
- 출고 목록·상세 조회, FAILED 재처리 및 PROCESSING 상태 polling
- FAILED 출고 대상 OpenAI 장애 분석 요청
- 가능한 원인, 확인 항목, 재처리 권고를 구조화하여 표시
- 출고·dispatch 단위별 장애 분석 결과 저장 및 기존 결과 재사용
- 환경변수 기반 AI 분석 활성화/비활성화 및 모델 설정
- 컨트롤러 / 서비스 테스트

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
  | WMS 처리 결과 시뮬레이션
  v
[DB Status Update]
  |
  | SUCCESS or FAILED
  v
[조회 / 재처리 / 운영 확인]
```

현재는 WMS 서버가 없기 때문에 `ShipmentDispatchConsumer`가 같은 Spring Boot 애플리케이션 안에서 WMS 처리 결과를 시뮬레이션합니다.

FAILED 출고의 장애 분석 흐름:

```text
[FAILED Shipment]
  |
  | 운영자가 AI 오류 분석 실행
  v
[FailureAnalysisService]
  |
  | FAILED 상태 / 기능 플래그 / dispatchBatchId 검증
  | PENDING 분석 이력 저장
  v
[FailureAnalyzer]
  |
  | OpenAI Responses API 호출
  v
[shipment_failure_analysis]
  |
  | COMPLETE 또는 FAILED
  v
[React Analysis Result Panel]
```

실제 구조에서는 다음처럼 분리될 수 있습니다.

```text
EAI API Server -> Kafka Producer -> Kafka Broker -> WMS Consumer
```

## 상태 흐름

| 상태 | 의미 |
| --- | --- |
| RECEIVED | ERP 출고 지시 수신 완료, WMS 전송 대기 |
| PROCESSING | WMS 전송 요청 또는 처리 중 |
| SUCCESS | 처리 성공 |
| FAILED | 처리 실패 |

정상 흐름:

```text
RECEIVED -> PROCESSING -> SUCCESS
```

실패 및 재처리 흐름:

```text
RECEIVED -> PROCESSING -> FAILED -> retry -> PROCESSING -> SUCCESS or FAILED
```

`FAILED` 상태는 실패 사유가 필요하며, `message` 필드에 실패 원인을 저장합니다. 필요 시 실패 당시 payload는 `errorPayload`에 저장할 수 있습니다.

## 도메인 모델

출고 지시는 `ShipmentRequest` 엔티티를 중심으로 관리합니다.

출고 정보는 `shipment_request`, AI 장애 분석 이력은 `shipment_failure_analysis` 테이블에 저장합니다. Java 코드에서는 출고 정보의 의미 단위를 `@Embeddable` / `@Embedded` 값 객체로 분리했습니다.

```text
ShipmentRequest
├─ ShipmentRequestInfo      출고 지시 기본 정보
├─ WarehouseInfo            창고 정보
├─ CustomerInfo             고객 정보
├─ ShipmentItemInfo         품목 정보
├─ ShipmentProcessingInfo   처리 상태 정보
└─ AuditInfo                생성/수정 시간
```

| 값 객체 | 주요 필드 |
| --- | --- |
| ShipmentRequestInfo | shipmentNo, orderNo, requestedAt |
| WarehouseInfo | warehouseCode |
| CustomerInfo | customerCode, customerName |
| ShipmentItemInfo | materialCode, materialName, quantity, unit |
| ShipmentProcessingInfo | status, retryCount, message, errorPayload, dispatchBatchId |
| AuditInfo | createdAt, updatedAt |

### 장애 분석 모델

`ShipmentFailureAnalysis`는 특정 출고의 특정 dispatch 시점에 발생한 장애 분석 결과를 보관합니다.

```text
ShipmentRequest 1 ---- N ShipmentFailureAnalysis
```

| 필드 | 용도 |
| --- | --- |
| shipmentRequest | 분석 대상 출고 지시 |
| dispatchBatchId | 실패가 발생한 dispatch 실행 단위 |
| retryCount | 분석 당시 재처리 횟수 스냅샷 |
| failureMessage | 분석 당시 실패 메시지 |
| errorPayloadSnapshot | 분석 당시 오류 payload |
| status | PENDING / COMPLETE / FAILED |
| analysisResult | LLM이 반환한 구조화된 JSON 문자열 |
| analyzerName | 분석기 구현체 이름 |
| analysisErrorMessage | AI 분석 자체가 실패한 사유 |

`shipment_id + dispatch_batch_id`에는 유일성 제약을 두어 동일한 실패 건에 대한 중복 분석 저장을 방지합니다.

## 서비스 클래스 구조

```text
shipment/service
├─ ShipmentRequestService
├─ ShipmentDispatchService
├─ ShipmentDispatchResultService
└─ ShipmentTimeoutService
```

| 서비스 | 책임 |
| --- | --- |
| ShipmentRequestService | 출고 지시 등록 및 조회 |
| ShipmentDispatchService | RECEIVED 건 dispatch, PROCESSING 변경, Kafka 발행 요청 |
| ShipmentDispatchResultService | Kafka Consumer 처리 결과를 SUCCESS/FAILED로 반영 |
| ShipmentTimeoutService | 오래된 PROCESSING 건을 FAILED로 전환 |

Producer와 Consumer는 Kafka 입출력에 집중하고, DB 상태 변경과 업무 판단은 Service 계층에서 처리합니다.

## Kafka 처리 구조

### Topic

```text
shipment-dispatch
```

### Message 예시

```json
{
  "shipmentId": 1,
  "shipmentNo": "SHP-20260714-001",
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

현재 WMS 시뮬레이션 규칙:

```text
shipmentNo에 FAIL 포함 -> FAILED
그 외 -> SUCCESS
```

## Scheduler

`@EnableScheduling`과 `@Scheduled`를 사용하여 주기적으로 출고 지시를 처리합니다.

기본 cron 설정:

```yaml
shipment:
  dispatch:
    scheduler:
      cron:
        default: "0 */5 * * * *"
    timeout-minutes: 10
```

즉 서버 시간 기준 매 5분 정각에 실행됩니다.

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

오래된 `PROCESSING` 건은 자동 재전송하지 않습니다.

```text
PROCESSING 상태가 10분 이상 지속
-> FAILED 전환
-> message = Dispatch timeout
```

## Scheduler 제어 API

스케줄러 dispatch는 서버 재시작 없이 enable / disable 할 수 있습니다.

| Method | URL | 설명 |
| --- | --- | --- |
| GET | `/api/shipments/scheduler` | dispatch scheduler 상태 조회 |
| PATCH | `/api/shipments/scheduler/enable` | dispatch scheduler 활성화 |
| PATCH | `/api/shipments/scheduler/disable` | dispatch scheduler 비활성화 |

현재는 `AtomicBoolean` 기반 인메모리 플래그로 관리합니다. 서버 재시작 시 기본값(true)으로 초기화됩니다.

## AI 장애 분석

운영자는 `FAILED` 상태의 출고 상세 화면에서 **AI 오류 분석**을 실행할 수 있습니다. 분석 기능은 기존 출고 처리 흐름과 분리되어 있으므로 OpenAI API 호출이 실패해도 출고 상태나 Kafka 처리에는 영향을 주지 않습니다.

처리 순서:

```text
1. 출고 지시 존재 여부와 FAILED 상태 검증
2. OpenAI 장애 분석 기능 활성화 여부 확인
3. dispatchBatchId 존재 여부 확인
4. shipmentId + dispatchBatchId 기준 기존 분석 조회
5. 기존 결과가 없으면 PENDING 분석 이력 저장
6. FailureAnalyzer 인터페이스를 통해 OpenAI 호출
7. 정상 응답은 COMPLETE, 호출 오류나 빈 응답은 FAILED로 저장
8. React 화면에서 분석 결과 또는 분석 오류 표시
```

OpenAI에는 출고번호, 실패 메시지, 재처리 횟수, `dispatchBatchId`, 마지막 수정 시각과 오류 payload를 전달합니다. 오류 payload는 프롬프트 크기를 제한하기 위해 최대 2,000자까지만 사용합니다.

분석 응답 형식:

```json
{
  "summary": "장애 요약",
  "possibleCauses": ["가능성 있는 원인"],
  "checks": ["운영자가 확인할 항목"],
  "retryRecommendation": {
    "decision": "RETRY | CHECK_REQUIRED | DO_NOT_RETRY",
    "reason": "재처리 판단 근거"
  }
}
```

`FailureAnalyzer` 인터페이스와 `OpenAIFailureAnalyzer` 구현체를 분리하여 테스트에서는 실제 API를 호출하지 않고 mock으로 대체할 수 있습니다.

## API 목록

모든 `/api/**` 요청은 `x-api-key` 헤더가 필요합니다.

| Method | URL | 설명 |
| --- | --- | --- |
| POST | `/api/shipments` | 출고 지시 등록 |
| GET | `/api/shipments` | 출고 지시 목록 조회 |
| GET | `/api/shipments/{id}` | 출고 지시 상세 조회 |
| GET | `/api/shipments/status/{status}` | 상태별 출고 지시 조회 |
| POST | `/api/shipments/{id}/retry` | FAILED 출고 지시 재처리 |
| POST | `/api/shipments/{id}/dispatch` | 수동 dispatch |
| GET | `/api/shipments/scheduler` | 스케줄러 상태 조회 |
| PATCH | `/api/shipments/scheduler/enable` | 스케줄러 활성화 |
| PATCH | `/api/shipments/scheduler/disable` | 스케줄러 비활성화 |
| POST | `/api/analyses/shipments/{shipmentId}/failure` | FAILED 출고 AI 장애 분석 |

## API Key 인증

Spring Security와 커스텀 `OncePerRequestFilter`를 사용하여 `/api/**` 요청을 보호합니다.

인증 헤더:

```http
x-api-key: your-api-key
```

보안 관련 클래스:

```text
com.eaishipment.config.security
├─ ApiKeyProperties
├─ ApiKeyAuthenticationFilter
└─ SecurityConfig
```

| 클래스 | 역할 |
| --- | --- |
| ApiKeyProperties | `security.api-key` 설정 바인딩 |
| ApiKeyAuthenticationFilter | 요청 헤더의 x-api-key 검증 |
| SecurityConfig | SecurityFilterChain 구성 및 URL 접근 정책 정의 |

인증 실패 응답:

```json
{
  "resultCode": "E",
  "message": "Invalid API key",
  "data": null
}
```

## 환경 설정

프로젝트 루트에 `.env` 파일을 생성하고 API Key를 설정합니다.

```properties
EAI_API_KEY=your-api-key
OPENAI_API_KEY=your-openai-api-key
OPENAI_ANALYSIS_ENABLED=false
OPENAI_MODEL=your-available-model-id
```

`OPENAI_ANALYSIS_ENABLED=false`이면 장애 분석 API는 OpenAI를 호출하거나 `PENDING` 분석 이력을 생성하지 않고 요청을 거부합니다. 실제 호출 테스트가 필요할 때만 `true`로 변경하는 것을 권장합니다. `.env`는 Git에 커밋하지 않습니다.

`application.yml`은 `.env`를 import합니다.

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

API Key 설정:

```yaml
security:
  api-key:
    header-name: x-api-key
    value: ${EAI_API_KEY:local-dev-api-key}
```

AI 장애 분석 설정:

```yaml
analysis:
  openai:
    enabled: ${OPENAI_ANALYSIS_ENABLED:true}
    model: ${OPENAI_MODEL:gpt-5.6-luna}
```

`OPENAI_MODEL`에는 현재 OpenAI 프로젝트에서 실제로 사용할 수 있는 모델 ID를 지정해야 합니다.

## 요청 예시

### 출고 지시 등록

```http
POST /api/shipments
Content-Type: application/json
x-api-key: your-api-key
```

```json
{
  "shipmentNo": "SHP-20260714-001",
  "orderNo": "ORD-20260714-001",
  "requestedAt": "2026-07-14T09:00:00",
  "warehouseCode": "WH-SEOUL-01",
  "customerCode": "CUST-001",
  "customerName": "Seoul Distribution",
  "materialCode": "MAT-001",
  "materialName": "Water 500ml",
  "quantity": 100,
  "unit": "EA"
}
```

### 상태 전이 정책

수동 상태 변경 API는 상태 전이 무결성을 위해 비활성화되어 있습니다.
상태는 dispatch 요청, Kafka Consumer 처리 결과, PROCESSING timeout, FAILED 재처리 흐름에서만 변경됩니다.

### 재처리

```http
POST /api/shipments/1/retry
x-api-key: your-api-key
```

### 수동 dispatch

```http
POST /api/shipments/1/dispatch
x-api-key: your-api-key
```

### AI 장애 분석

```http
POST /api/analyses/shipments/1/failure
x-api-key: your-api-key
```

분석 대상은 `FAILED` 상태이며 `dispatchBatchId`가 존재해야 합니다. 같은 `shipmentId + dispatchBatchId`로 다시 요청하면 OpenAI를 중복 호출하지 않고 저장된 분석 결과를 반환합니다.

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
- 예상하지 못한 서버 오류

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

주요 로그에는 `dispatchBatchId`를 포함하여 스케줄러 실행 단위와 Kafka 메시지 흐름을 추적할 수 있도록 했습니다.

## 패키지 구조

```text
com.eaishipment
├─ config
│  ├─ security
│  └─ swagger
├─ failureanalysis
│  ├─ analyzer
│  ├─ config
│  ├─ controller
│  ├─ dto
│  ├─ entity
│  ├─ mapper
│  ├─ repository
│  └─ service
├─ global
│  ├─ exception
│  └─ response
└─ shipment
   ├─ consumer
   ├─ controller
   ├─ dto
   ├─ entity
   ├─ event
   ├─ mapper
   ├─ producer
   ├─ repository
   ├─ scheduler
   └─ service

frontend/src
├─ api          # Backend API 호출
├─ components   # 목록, 상세, 재처리 및 AI 분석 UI
├─ types        # 출고 및 장애 분석 응답 TypeScript 타입
├─ utils        # 장애 분석 JSON 변환 등 화면 보조 로직
├─ App.tsx      # 조회, polling, 재처리 및 AI 분석 상태 관리
└─ main.tsx     # React 애플리케이션 진입점
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

`bootRun`은 서버를 계속 실행하는 명령입니다. 종료하려면 터미널에서 `Ctrl + C`를 누릅니다.

### 3. React 개발 서버 실행

새 터미널에서 프론트엔드 디렉터리로 이동한 뒤 의존성을 설치하고 개발 서버를 실행합니다.

```powershell
cd frontend
npm install
npm run dev
```

개발 환경에서는 `frontend/.env.development`의 `VITE_API_BASE_URL`을 통해 Spring Boot API 주소를 설정합니다.

### 4. 빌드

```powershell
.\gradlew clean build
cd frontend
npm run build
```

### 5. 테스트

```powershell
.\gradlew test
```

특정 테스트 클래스만 실행:

```powershell
.\gradlew test --tests "com.eaishipment.shipment.controller.ShipmentRequestControllerTest"
.\gradlew test --tests "com.eaishipment.failureanalysis.service.FailureAnalysisServiceTest"
```

## 접속 URL

| 구분 | URL |
| --- | --- |
| React 관리 UI | `http://localhost:5173` |
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

테스트는 `test` 프로필을 사용합니다.

```java
@ActiveProfiles("test")
```

테스트 설정 파일:

```text
src/test/resources/application-test.yml
```

테스트 환경에서는 운영/로컬 `.env`의 실제 API Key를 사용하지 않고 테스트 전용 키를 사용합니다.

```yaml
security:
  api-key:
    value: api-key-test
```

테스트 코드에서는 다음 값을 사용합니다.

```java
private static final String API_KEY_HEADER = "x-api-key";
private static final String API_KEY_VALUE = "api-key-test";
```

테스트 범위:

- 출고 지시 등록/조회/재처리
- dispatch 시 RECEIVED -> PROCESSING 변경
- Kafka Producer 호출 확인
- Consumer 처리 결과 SUCCESS/FAILED 반영
- 실패 payload 저장 확인
- 오래된 PROCESSING timeout 처리
- Controller 응답 검증
- API Key 없음/오류 키 요청에 대한 401 응답 검증
- AI 분석 대상의 FAILED 상태 및 dispatchBatchId 검증
- AI 분석 비활성화 시 저장·호출 차단 검증
- 기존 분석 결과 재사용 및 중복 OpenAI 호출 방지 검증
- 분석 성공 시 COMPLETE, 예외·빈 응답 시 FAILED 저장 검증
- AI 장애 분석 API 성공/실패 및 API Key 보호 검증

장애 분석 테스트에서는 `FailureAnalyzer`를 mock 처리하므로 실제 OpenAI API 비용이 발생하지 않습니다. 현재 전체 백엔드 테스트는 36개이며 모두 통과합니다.

## 로컬 테스트 시나리오

### 정상 처리 케이스

1. Kafka 실행
2. Spring Boot 실행
3. API Key 설정
4. 출고 지시 등록
5. 수동 dispatch 또는 scheduler 실행
6. Kafka Consumer 처리
7. 상태가 `SUCCESS`로 변경되는지 확인

```text
RECEIVED -> PROCESSING -> SUCCESS
```

### 실패 처리 케이스

`shipmentNo`에 `FAIL`을 포함한 출고 지시를 등록합니다.

```text
SHP-FAIL-20260714-001
```

dispatch 후 Consumer가 실패로 처리합니다.

```text
RECEIVED -> PROCESSING -> FAILED
```

### 장기 PROCESSING 처리 케이스

`PROCESSING` 상태가 설정 시간 이상 유지되면 timeout scheduler가 실패로 전환합니다.

```text
PROCESSING -> FAILED
message = Dispatch timeout
```

### AI 장애 분석 케이스

1. `OPENAI_API_KEY`, `OPENAI_MODEL`을 설정하고 분석 기능을 활성화합니다.
2. `FAILED`이며 `dispatchBatchId`가 존재하는 출고를 선택합니다.
3. React 상세 화면에서 **AI 오류 분석**을 실행합니다.
4. 요약, 가능한 원인, 확인 항목과 재처리 권고가 표시되는지 확인합니다.
5. 같은 실패 건을 다시 분석했을 때 저장된 결과가 반환되는지 확인합니다.

## 설계 포인트

- Java 코드에서는 `@Embedded` 값 객체로 의미 단위를 분리했습니다.
- Entity를 API 응답으로 직접 노출하지 않고 DTO로 변환합니다.
- 상태 전이는 트랜잭션 내부에서 JPA dirty checking을 사용합니다.
- Kafka 발행은 비동기이므로 dispatch API 응답은 최종 성공이 아니라 `PROCESSING` 상태를 의미합니다.
- Kafka 최종 처리 결과는 Consumer가 별도로 DB에 반영합니다.
- 오래된 PROCESSING 건은 자동 재전송하지 않고 FAILED로 전환하여 중복 출고 위험을 줄입니다.
- 모든 `/api/**` 요청은 x-api-key로 보호합니다.
- 테스트 환경은 `.env`와 분리된 테스트 전용 API Key를 사용합니다.
- AI 분석은 출고 처리 트랜잭션과 분리하여 외부 API 장애가 Kafka 처리와 상태 전이에 영향을 주지 않도록 했습니다.
- 동일한 출고·dispatch 조합은 기존 분석 결과를 재사용하여 중복 호출과 비용 발생을 줄입니다.
- LLM 결과는 가능한 원인과 확인 항목을 제공하는 운영 보조 정보이며 실제 장애 원인을 확정하지 않습니다.

## 현재 구현 범위와 한계

- H2를 로컬 실행 DB로 사용하며 PostgreSQL 전환은 아직 구현하지 않았습니다.
- Kafka Consumer는 실제 WMS가 아니라 동일한 Spring Boot 애플리케이션 안에서 처리 결과를 시뮬레이션합니다.
- Kafka Retry Topic, DLQ와 별도 WMS Result Topic은 아직 구현하지 않았습니다.
- 스케줄러 enable/disable 상태는 인메모리이므로 서버 재시작 후 유지되지 않습니다.
- React 단위 테스트 도구는 아직 구성하지 않았으며 프론트엔드는 TypeScript 빌드로 검증합니다.
- AI 분석 결과는 문자열 JSON으로 저장하고 프론트엔드에서 구조화된 타입으로 변환합니다.
