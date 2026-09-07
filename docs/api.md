# API 계약 (Phase 0)

- Base URL: `http://localhost:8080/api/v1`
- 인증: 모든 `/api/**` 요청에 `Authorization: Bearer ${APP_API_TOKEN}`. 실패 시 401. `/actuator/health`는 공개.
- 응답: JSON. 시각은 ISO-8601(`2026-09-30T23:59:00+09:00`), 날짜만 있으면 `2026-09-30`.
- 오류: `{ "code": "NOT_FOUND" | "VALIDATION" | "CONFLICT" | "UNAUTHORIZED" | "AI_ERROR" | "IMPORT_ERROR", "message": "...", "details": {...}? }`
- ID: UUID 문자열.
- 목록은 배열 그대로 반환(Phase 0에서는 페이지네이션 없음).

## 공통 enum

```
Visibility        PRIVATE | PUBLIC
ApplicationStatus INTERESTED | PLANNED | WRITING | SUBMITTED | DOCUMENT_PASSED | TEST | INTERVIEW | ACCEPTED | REJECTED | WITHDRAWN
RequirementKind   RESUME | ESSAY | TRANSCRIPT | CERTIFICATE | LANGUAGE | PORTFOLIO | EXPERIENCE_DESC | OTHER
EssayStatus       EMPTY | DRAFT | DONE
SectionType       OVERVIEW | FEATURES | ARCHITECTURE | TROUBLESHOOTING | IMPROVEMENTS | LINKS
EvidenceType      PR | COMMIT | NOTE | URL
```

칸반 컬럼 순서는 ApplicationStatus 순서와 같다. ACCEPTED/REJECTED/WITHDRAWN는 "종료" 컬럼으로 묶어 표시해도 된다.

## Company

```
GET    /companies?q=            -> Company[]
POST   /companies               CompanyUpsert -> Company (201)
GET    /companies/{id}          -> CompanyDetail
PUT    /companies/{id}          CompanyUpsert -> Company
DELETE /companies/{id}          -> 204 (지원이 있으면 409)
```

```ts
Company {
  id, name, industry?, website?, talentProfile?, coreValues: string[], techStack: string[],
  hiringProcess?, notes?, createdAt, updatedAt
}
CompanyUpsert = Omit<Company, "id"|"createdAt"|"updatedAt">
CompanyDetail = Company & { applications: ApplicationCard[] }
```

## Application

```
GET    /applications?status=&companyId=   -> ApplicationCard[]  (deadline 오름차순, null은 뒤)
GET    /applications/board                -> { columns: { status, items: ApplicationCard[] }[] }
GET    /applications/upcoming?days=14     -> ApplicationCard[] (마감이 오늘~N일 내, 종료 상태 제외)
POST   /applications                      ApplicationCreate -> ApplicationDetail (201)
GET    /applications/{id}                 -> ApplicationDetail
PUT    /applications/{id}                 ApplicationUpdate -> ApplicationDetail
PATCH  /applications/{id}/status          { status, note? } -> ApplicationDetail (이력 기록)
DELETE /applications/{id}                 -> 204

POST   /applications/{id}/requirements    { title, kind, note? } -> Requirement (201)
PATCH  /applications/{id}/requirements/{rid}  { title?, kind?, done?, note? } -> Requirement
DELETE /applications/{id}/requirements/{rid}  -> 204

POST   /applications/{id}/essay-questions       { question, maxLength?, sortOrder? } -> EssayQuestion (201)
PATCH  /applications/{id}/essay-questions/{qid} { question?, maxLength?, draft?, status?, sortOrder? } -> EssayQuestion
DELETE /applications/{id}/essay-questions/{qid} -> 204

POST   /applications/from-extraction      { extraction: JobPostingExtraction, companyId? } -> ApplicationDetail (201)
```

```ts
ApplicationCard {
  id, companyId, companyName, positionTitle, status, deadlineAt?, postingUrl?,
  requirementsDone: number, requirementsTotal: number, essayDone: number, essayTotal: number,
  updatedAt
}
ApplicationDetail = ApplicationCard & {
  employmentType?, location?, notes?, result?, retrospective?,
  company: Company,
  requirements: Requirement[],
  essayQuestions: EssayQuestion[],
  statusHistory: { status, note?, changedAt }[],
  createdAt
}
ApplicationCreate {
  companyId? , companyName?      // 둘 중 하나. companyName만 오면 같은 이름 회사가 있으면 재사용, 없으면 생성
  positionTitle, postingUrl?, deadlineAt?, employmentType?, location?, notes?, status? (기본 INTERESTED)
  requirements?: { title, kind }[]
  essayQuestions?: { question, maxLength? }[]
}
ApplicationUpdate { positionTitle?, postingUrl?, deadlineAt?, employmentType?, location?, notes?, result?, retrospective? }
Requirement   { id, title, kind, done, note?, sortOrder }
EssayQuestion { id, question, maxLength?, draft?, status, sortOrder, updatedAt }
```

from-extraction: 추출 결과에서 companyName으로 회사 찾기/생성(companyId가 오면 그것 사용), 지원 카드 생성, requiredDocuments → requirements, essayQuestions → essay-questions. hiringStages는 notes에 줄바꿈으로 기록.

## Project

```
GET    /projects                 -> ProjectSummary[]
POST   /projects                 ProjectUpsert -> ProjectDetail (201)
GET    /projects/{id}            -> ProjectDetail
PUT    /projects/{id}            ProjectUpsert -> ProjectDetail
PUT    /projects/{id}/sections   { sections: { type, body }[] } -> ProjectDetail   (전체 교체)
PUT    /projects/{id}/tech-stack { items: { category, name }[] } -> ProjectDetail  (전체 교체)
PUT    /projects/{id}/metrics    { items: { label, value }[] } -> ProjectDetail    (전체 교체)
DELETE /projects/{id}            -> 204 (경험은 projectId가 null로 남는다)
```

```ts
ProjectSummary { id, name, tagline?, startedAt?, endedAt?, teamSize?, role?, repoUrl?, deployUrl?, visibility, experienceCount, techStack: string[] , updatedAt }
ProjectDetail = ProjectSummary & {
  sections: { type, body }[],
  techStackItems: { category, name }[],   // category 예: LANGUAGE, FRAMEWORK, DATABASE, INFRA, TOOL
  metrics: { label, value }[],
  experiences: ExperienceSummary[],
  createdAt
}
ProjectUpsert { name, tagline?, startedAt?, endedAt?, teamSize?, role?, repoUrl?, deployUrl?, visibility? }
```

## Experience

```
GET    /experiences?projectId=&tag=&q=   -> ExperienceSummary[]
GET    /experiences/tags                 -> { tag, count }[]
POST   /experiences                      ExperienceUpsert -> Experience (201)
GET    /experiences/{id}                 -> Experience
PUT    /experiences/{id}                 ExperienceUpsert -> Experience
DELETE /experiences/{id}                 -> 204
```

```ts
ExperienceSummary { id, projectId?, projectName?, title, tags: string[], visibility, updatedAt }
Experience = ExperienceSummary & {
  situation, task, action, result,
  evidence: { type, url, label? }[],
  createdAt
}
ExperienceUpsert { projectId?, title, situation, task, action, result, tags?: string[], evidence?: { type, url, label? }[], visibility? }
```

## AI

```
POST /ai/extract-posting   { url?, text? }  -> { extraction: JobPostingExtraction, sourceText: string, callId }
GET  /ai/calls?limit=50    -> AiCall[]
```

- url이 오면 서버가 페이지를 받아 본문 텍스트를 뽑고, 실패하면 `AI_ERROR`에 "본문을 붙여넣어 주세요"를 담아 422.
- text가 오면 그대로 사용. 둘 다 없으면 400.

```ts
JobPostingExtraction {
  companyName, positionTitle, employmentType?, location?, deadlineAt?,   // deadlineAt은 ISO-8601 또는 null
  requiredSkills: string[], preferredSkills: string[],
  hiringStages: string[],                // 예: ["서류", "코딩테스트", "1차 면접", "2차 면접"]
  requiredDocuments: { title, kind: RequirementKind }[],
  essayQuestions: { question, maxLength?: number }[],
  summary: string,
  sourceUrl?: string
}
AiCall { id, task, model, status: "OK"|"ERROR", inputTokens, outputTokens, cacheReadTokens, costUsd, durationMs, errorMessage?, createdAt }
```

## Import (Notion CSV)

```
POST /import/notion/preview   multipart file=csv           -> { columns: string[], sampleRows: string[][], suggestedMapping: NotionMapping, rowCount }
POST /import/notion           multipart file=csv, mapping=json(NotionMapping) -> { created, updated, skipped, errors: { row, message }[] }
```

```ts
NotionMapping {
  companyName: string,          // 필수. 컬럼 이름
  positionTitle?: string,
  deadlineAt?: string,
  status?: string,
  postingUrl?: string,
  notes?: string[],             // 여러 컬럼을 notes로 합침
  statusValues?: Record<string, ApplicationStatus>   // 노션 상태값 -> enum
}
```

- 멱등: (companyName, positionTitle) 조합으로 기존 지원을 찾아 업데이트한다.
- suggestedMapping은 컬럼 이름에 회사/기업/이름/마감/직무/상태/링크/URL/메모 같은 키워드가 있으면 채운다.

## Public profile (Phase 1, placeholder)

인증 없이 읽기 전용. Phase 0에는 엔드포인트가 없고 경로만 예약한다. 스키마는 이미 `users.username`을 가지고 있으므로 아래 경로가 그대로 붙는다.

```
GET /public/u/{username}                -> 프로필 요약 (소개, 기술 스택, 링크)
GET /public/u/{username}/projects       -> visibility=PUBLIC 프로젝트 카드
GET /public/u/{username}/experiences    -> visibility=PUBLIC 경험
GET /public/u/{username}/certifications -> 취득 자격증 (Phase 3)
GET /public/u/{username}/resume         -> 공개로 지정한 이력서 버전 (Phase 1)
```

지원 현황, 기업 분석, 면접 기록, 자소서 초안은 공개 경로에 존재하지 않는다(엔티티에 공개 옵션 자체가 없음).

## 인증·소유권 메모 (Phase 0)

- 단일 토큰이 시드된 사용자(`username=me`)로 해석된다. 모든 루트 테이블(companies, applications, projects, experiences, ai_calls)에 `owner_id`가 있고, 모든 조회·생성은 현재 사용자로 범위가 제한된다.
- 다른 사용자의 행을 요청하면 403이 아니라 404를 돌려준다.
- 회사 이름 유일성은 사용자 안에서만 적용된다 `(owner_id, name)`.
