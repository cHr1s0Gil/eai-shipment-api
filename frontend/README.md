# EAI Shipment Frontend

출고 지시의 처리 상태를 조회하고 FAILED 건을 재처리하는 React + TypeScript 관리 화면입니다.

## 주요 기능

- 출고 지시 목록 및 상세 조회
- RECEIVED, PROCESSING, SUCCESS, FAILED 상태 표시
- FAILED 출고 재처리
- PROCESSING 상태 polling 및 목록 동기화
- dispatchBatchId, 재처리 횟수, 오류 메시지 확인
- x-api-key 기반 Backend API 호출

## 실행 조건

- Node.js 및 npm
- `http://localhost:8080`에서 실행 중인 Spring Boot Backend

## 개발 서버 실행

```bash
npm install
npm run dev
```

개발 서버는 기본적으로 `http://localhost:5173`에서 실행됩니다.

## 환경변수

개발 환경의 Backend URL은 `.env.development`에서 관리합니다.

```env
VITE_API_BASE_URL=http://localhost:8080
```

production 빌드는 `/api` 상대 경로를 사용합니다. 로컬 `preview` 실행 시 Vite가 `http://localhost:8080`으로 프록시하며, 실제 배포 환경에서는 Web Server 또는 Reverse Proxy가 `/api`를 Backend로 전달해야 합니다.

API Key는 환경변수나 번들에 저장하지 않고 화면에서 입력합니다. `VITE_` 환경변수는 브라우저 번들에 포함되므로 비밀값을 저장하면 안 됩니다.

## 검증

```bash
npm run lint
npm run build
npm run preview
```
