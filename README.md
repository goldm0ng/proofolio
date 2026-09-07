# proofolio

취업 준비 컨텍스트를 한 곳에 쌓고, 그 위에서 지원·학습·면접·증빙을 처리하는 개인 커리어 OS.
기획안: `docs/plan.md`. API 계약: `docs/api.md`.

## 구조

```
backend/   Spring Boot 3 · Java 21 · PostgreSQL · Flyway · Anthropic Java SDK
frontend/  Next.js (App Router) · TypeScript · Tailwind
docs/      기획안, API 계약, ADR
```

백엔드 패키지는 도메인 모듈 단위로 나뉜다(Phase 0 기준).

```
com.proofolio
├─ common        공통 엔티티/예외/보안 필터/Visibility
├─ company       기업 프로필
├─ application   지원 파이프라인(칸반, 체크리스트, 자소서 문항)
├─ project       프로젝트 카드(섹션, 스택, 지표)
├─ experience    경험 뱅크(STAR, 태그, 근거 링크)
├─ ai            AI 게이트웨이(공고 추출, 호출 로그)
└─ importer      노션 CSV 임포트
```

## 실행

```bash
cp .env.example .env            # 값 채우기
docker compose up -d db          # PostgreSQL (기본 5433 포트)

cd backend && ./gradlew bootRun  # http://localhost:8080
cd frontend && npm install && npm run dev   # http://localhost:3000
```

인증은 단일 사용자 Bearer 토큰(`APP_API_TOKEN`). 브라우저는 토큰을 모르고, Next.js 서버의 프록시 라우트가 붙인다.

Phase 0는 실사용 단일 사용자다. 다만 스키마는 처음부터 멀티테넌트 대응이다. `users` 테이블에 사용자 한 명(`me`)이 시드되고,
모든 루트 테이블에 `owner_id`가 있으며, 서비스는 항상 현재 사용자로 범위를 제한한다. 로그인·OAuth·Spring Security는 없다(Phase 1 이후).

## 테스트

```bash
cd backend && ./gradlew test     # Testcontainers로 PostgreSQL 기동, Docker 필요
cd frontend && npm run build     # 타입 체크 포함
```
