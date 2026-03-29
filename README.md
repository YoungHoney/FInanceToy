# FInanceToy

금융 코어 토이프로젝트 저장소입니다.
루트 안에서 백엔드와 프론트를 분리해 관리합니다.

## 구조

- `Backend/`: Java 21 + Spring Boot 3.x + PostgreSQL 기반 백엔드
- `Frontend/`: Vue 3 + Vite 기반 작업 콘솔
- `context/`: 프로젝트 목표와 도메인 리서치 메모

## 실행

백엔드:

```bash
cd Backend
docker compose up -d
./gradlew bootRun
```

프론트엔드:

```bash
cd Frontend
npm install
npm run dev
```

기본 개발 기준:

- Backend API: `http://localhost:8080`
- Backend DB: `localhost:5430`
- Frontend: `http://localhost:5173`

프론트 개발 서버는 Vite proxy를 통해 백엔드 `/api`를 호출합니다.
자세한 백엔드 설명은 [Backend/README.md](/mnt/c/Users/SSAFY/Desktop/FInanceToy/Backend/README.md)를 보면 됩니다.

## 테스트 DB

통합 테스트는 H2가 아니라 PostgreSQL 기준으로 실행합니다.

- 컨테이너 기동: `cd Backend && docker compose up -d`
- 테스트 연결 DB: `financetoy`
- 테스트 전용 schema: `financetoy_test`

즉, 테스트는 기존 PostgreSQL 컨테이너를 그대로 사용하고, 별도 schema를 분리해서 수행합니다.
