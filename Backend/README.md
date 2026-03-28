# FinanceToy Backend

금융 코어 문제를 작은 범위에서 정직하게 시뮬레이션하는 Java 21 기반 백엔드입니다.
실제 증권사나 캐피탈사의 운영 시스템을 재현했다고 주장하지 않고, 아래 문제를 작동 가능한 형태로 보여주는 데 목적이 있습니다.

- GUID 기반 idempotency
- 주문 후 외부 체결 연계 실패 대응
- 원장 중심 정합성 관리
- 자동취소 또는 보상처리
- 배치 대사
- 감사로그 및 운영 추적
- mode / try / repeat 기반 반복 실험

## 왜 이런 구조인가

### Modular Monolith
- 금융 코어 문제는 트랜잭션 정합성과 상태 일관성이 우선입니다.
- 작은 토이프로젝트에서 무리하게 MSA로 쪼개면 도메인 설명보다 인프라 설명이 커질 수 있습니다.
- 그래서 v1은 단일 Spring Boot 애플리케이션 안에서 `order`, `execution`, `ledger`, `reconciliation`, `audit`, `experiment` 패키지로 분리한 modular monolith로 시작합니다.

### PostgreSQL
- 거래 기록, 원장 엔트리, 감사로그, 실험 결과를 관계형으로 명확하게 관리하기 좋습니다.
- 정합성 검증과 대사 기준을 SQL 관점에서 설명하기 쉽습니다.
- 추후 프론트 조회 화면이 붙더라도 기록성 테이블 확장이 자연스럽습니다.

## 핵심 시나리오

사용자는 매수 주문을 생성하고, 서버는 주문 금액을 먼저 `reserved` 처리합니다.
이후 외부 체결 연계 시뮬레이터가 응답하고, 결과에 따라 원장과 주문 상태가 달라집니다.

1. 주문 생성
2. 현금 reserve
3. 외부 체결 시도
4. 성공이면 체결 확정과 원장 반영
5. 실패이면 자동취소 또는 보상처리
6. 늦은 콜백이나 중복 콜백은 감사로그와 대사 대상으로 남김

## 실패 Mode

- `NORMAL`: 정상 체결
- `TIMEOUT`: 외부 응답 타임아웃, 보상처리
- `REJECT`: 외부 거절, 자동취소
- `DUPLICATE_CALLBACK`: 정상 체결 뒤 중복 콜백 수신
- `DELAYED_CALLBACK`: 타임아웃으로 보상처리된 뒤 늦은 체결 콜백 수신

## 상태 전이

- `CREATED`
- `RESERVED`
- `EXECUTING`
- `EXECUTED`
- `CANCELLED`
- `COMPENSATED`
- `RECONCILE_REQUIRED`

자세한 규칙은 [initial-design.md](/mnt/c/Users/SSAFY/Desktop/FInanceToy/Backend/docs/initial-design.md)에 정리했습니다.

## API

- `POST /api/orders`
- `GET /api/orders/{orderId}`
- `POST /api/experiments/run`
- `GET /api/experiments/{runId}`
- `POST /api/reconciliations/run`
- `GET /api/reconciliations/{jobId}`

Swagger UI는 `/swagger-ui/index.html`, OpenAPI 문서는 `/v3/api-docs`를 기준으로 둡니다.

## 로컬 개발 기준

- WSL Ubuntu
- Java 21
- Docker PostgreSQL
- Gradle Wrapper

## DB 실행

백엔드 DB는 Docker로 실행합니다.

```bash
docker compose up -d
```

기본 포트 매핑은 `5430 -> 5432`입니다.

환경 변수 기본값:

- `DB_HOST=localhost`
- `DB_PORT=5430`
- `DB_NAME=financetoy`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=postgres`

실행:

```bash
./gradlew bootRun
```

테스트:

```bash
./gradlew test
```

## 비과장 원칙

- 실제 거래소, 결제기관, 증권사 운영 시스템을 구현했다고 말하지 않습니다.
- 실제 금융권 운영 경험처럼 포장하지 않습니다.
- 대신 금융 코어 문제를 작은 범위에서 어떻게 구조화하고 검증했는지를 보여줍니다.
