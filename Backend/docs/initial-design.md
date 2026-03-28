# 초기 설계 메모

## 1. 도메인 흐름

### 주문
- 사용자는 `POST /api/orders`로 매수 주문을 요청한다.
- 요청에는 `accountId`, `instrumentCode`, `quantity`, `price`, `idempotencyKey`, `mode`가 들어간다.
- `idempotencyKey`는 GUID를 전제로 하며, 같은 키가 재호출되면 기존 주문을 그대로 반환한다.

### 원장
- 주문 접수 시 현금은 먼저 reserve 된다.
- 원장 엔트리는 아래 유형을 사용한다.
  - `RESERVE_CASH`
  - `EXECUTE_BUY`
  - `RELEASE_CASH`
  - `APPLY_COMPENSATION`

### 외부 체결 시뮬레이션
- 실제 거래소 연동은 하지 않는다.
- 모드별 결과는 다음처럼 정의한다.
  - `NORMAL`: 체결 성공
  - `REJECT`: 체결 거절, reserve 해제
  - `TIMEOUT`: 응답 없음, 보상처리
  - `DUPLICATE_CALLBACK`: 정상 체결 후 같은 체결 콜백이 한 번 더 도착
  - `DELAYED_CALLBACK`: 타임아웃 보상처리 후 늦은 체결 콜백이 도착

## 2. 상태 전이

### 정상 흐름
- `CREATED -> RESERVED -> EXECUTING -> EXECUTED`

### 거절 흐름
- `CREATED -> RESERVED -> EXECUTING -> CANCELLED`

### 타임아웃 보상 흐름
- `CREATED -> RESERVED -> EXECUTING -> COMPENSATED`

### 늦은 콜백 흐름
- `CREATED -> RESERVED -> EXECUTING -> COMPENSATED -> RECONCILE_REQUIRED`

## 3. 대사 기준

### EXECUTED
- reserve 원장 1건 존재
- execute 원장 1건 존재
- release / compensation 원장 없음
- 중복 콜백이 있어도 execute 원장은 1건만 존재

### CANCELLED
- reserve 원장 1건 존재
- release 원장 1건 존재
- execute 원장 없음

### COMPENSATED
- reserve 원장 1건 존재
- compensation 원장 1건 존재
- execute 원장 없음

### RECONCILE_REQUIRED
- 늦은 콜백 또는 정합성 충돌이 남은 주문
- 배치 대사에서 unresolved 항목으로 집계

## 4. try / repeat 규칙

- `tryCount`: 한 반복에서 실행할 주문 수
- `repeatCount`: 동일 mode 실험을 몇 번 반복할지
- 총 실행 건수는 `tryCount * repeatCount`
- 각 실행 결과는 DB에 저장해서 나중에 비교 가능해야 한다.

## 5. 운영 추적

- 주문 상태 변화는 `order_event`로 남긴다.
- 운영 관점 기록은 `audit_log`로 남긴다.
- 배치 대사 결과는 `reconciliation_job`에 남긴다.
- 반복 실험 결과는 `experiment_run`에 남긴다.
