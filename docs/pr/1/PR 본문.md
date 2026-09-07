close #1

## 작업 배경

기존 취업 준비는 **노션 데이터베이스에 공고마다 페이지를 수기로 만들고, 마감·기업 정보·자소서 문항·필요 서류를 각 페이지에 따로 입력한다**는 가정 위에서 돌아가고 있었습니다.

하지만 실제 준비 과정을 확인한 결과, **같은 정보를 여러 곳에 다시 적어야 하는 반례**가 반복됐습니다.

반례의 규모는 노션 CSV 임포트 후 실제 건수로 측정할 예정이며, 현재 확인된 패턴은 다음과 같습니다.

EX) 프로젝트 A의 트러블슈팅을 노션 프로젝트 페이지에 적고, 자소서 문항에 맞춰 다시 요약해 붙여넣고, 면접 준비 문서에 또 옮겨 적는다. 기업 B의 인재상은 지원 페이지마다 복사된다.

이로 인해 AI에게 자소서 초안을 맡길 때마다 이력서·프로젝트·기업 정보를 프롬프트에 수동으로 다시 붙여 넣어야 했고, 어떤 경험을 어느 자소서에 썼는지 추적할 수 없었습니다.

## 해결 방법

**프로젝트·경험·기업·지원 기록을 한 저장소(PostgreSQL)에 구조화해 쌓고, 그 위에 지원 칸반·프로젝트 카드·경험 뱅크·AI 공고 추출·노션 임포트를 얹은 Phase 0 골격**을 구현했습니다.

**노션을 원본으로 두고 Notion API 위에 AI 레이어만 얹는 대안**을 사용할 경우, 노션 페이지가 자유 형식이라 경험을 STAR 단위로 검색하거나, 어떤 경험을 어느 자소서에 썼는지 추적하거나, 항목별 공개 범위를 제어할 수 없습니다. 또한 백엔드 포트폴리오로서의 가치가 없습니다.

**직접 스키마를 갖는 방법**이 적절한 이유는, 이후 Phase(이력서 버전관리, 모의면접, 공개 프로필)가 모두 "경험 단위 참조"와 "항목별 visibility"를 전제로 하기 때문입니다. 다인 확장에 대비해 `users` 테이블과 모든 루트 테이블의 `owner_id`를 처음부터 두되, 지금은 단일 토큰으로 한 사용자만 씁니다.

## 작업 내용

- `backend/build.gradle`, `settings.gradle`, `application.yml`
  - Spring Boot 3.4 · Java 21 타깃, PostgreSQL · Flyway · Anthropic Java SDK 의존성, 환경변수 기반 설정
- `common/` (`BaseEntity`, `User`, `CurrentUser`, `ApiTokenFilter`, `GlobalExceptionHandler`), `db/migration/V1__init.sql`
  - Bearer 토큰 인증 필터, 시드 사용자, owner_id 스코핑 기반 클래스, 전체 스키마
- `company/`, `application/`
  - 기업 프로필 CRUD, 지원 카드·칸반·상태 이력·준비물 체크리스트·자소서 문항, 추출 결과로 카드 생성
- `ai/` (`AnthropicClaudeGateway`, `PostingExtractionService`, `AiCallLogger`, `PageTextFetcher`), `prompts/extract-posting.txt`
  - 공고 URL/본문 → Claude 구조화 출력으로 `JobPostingExtraction` 추출, 호출별 토큰·비용 로그
- `project/`, `experience/`
  - 프로젝트 카드(섹션·기술 스택·지표), STAR 경험·태그·근거 링크
- `importer/` (`NotionImportService`, `NotionDateParser`)
  - 노션 CSV 미리보기·컬럼 매핑 제안·멱등 임포트, 한글·영문 날짜 파싱
- `frontend/src/app/api/backend/[...path]/route.ts`, `lib/`, `components/`
  - 토큰을 서버에서만 붙이는 프록시, api.md 기반 타입·클라이언트, 공통 UI
- `frontend/src/app/(workspace)/**`, `app/u/[username]`
  - 대시보드, 지원 칸반(드래그 이동), 공고 등록(AI 추출/직접 입력), 지원 상세, 기업·프로젝트·경험·임포트·AI 로그, 공개 프로필 자리
- `AuthFilterTest`, `ApplicationFlowTest`, `ProjectExperienceTest`, `NotionImportTest`, `AiExtractionTest`
  - 401, 카드 생성→칸반→상태 이력→카운터, 섹션 교체·경험 태그, 임포트 멱등성·BOM·KST 날짜, AI 400/503 경로

## 검증

Testcontainers(PostgreSQL 16)로 백엔드 통합 테스트를 돌리고, 실제 DB·백엔드·프론트를 함께 띄워 브라우저에서 사용자 흐름을 확인했습니다.

- 수정 전: 노션에서 공고·기업·문항을 수기 입력, AI 호출 시 컨텍스트를 매번 복사
- 수정 후: `./gradlew test` 14개 통과. 브라우저에서 지원 카드 생성 → 준비물·자소서 문항 추가 → 상태 변경 → 칸반 반영 → 프로젝트 생성 → 노션 CSV 미리보기(한글 헤더 자동 매핑) → 대시보드 마감 표시까지 정상 동작, 콘솔 오류 없음

## 리뷰 포인트

DB를 PostgreSQL로 유지할지, MySQL로 바꿀지. 현재 PostgreSQL 전용 기능은 태그·핵심가치·기술스택의 `text[]` 컬럼과 GIN 인덱스이며, 이후 Phase에서 이력서 블록 트리(JSONB)와 경험 검색(pgvector)을 계획하고 있습니다. 바꾼다면 데이터가 쌓이기 전인 지금이 가장 쌉니다.

## 참고

- 백엔드 모듈 간 순환 참조(`company`↔`application`, `application`↔`ai`, `project`↔`experience`)가 있으나, 현재는 DTO·enum 참조 수준이라 컴파일·동작에 문제가 없습니다. 모듈 분리가 필요해지는 시점(MCP 어댑터 추가)에 인터페이스로 끊을 예정입니다.
- JDK 22로 Java 21 타깃을 빌드·실행하는 구성은 문제가 없음을 확인했습니다. 툴체인 자동 다운로드를 피하기 위한 선택입니다.
- create-next-app이 만든 `frontend/.gitignore`의 `.env*` 규칙이 `.env.local.example`까지 제외하고 있어 예외를 추가했습니다.
- 실제 노션 데이터는 아직 임포트하지 않았습니다. 작업 배경의 수치는 임포트 후 채웁니다.
