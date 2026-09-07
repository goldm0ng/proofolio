import type { ApplicationStatus, EssayStatus, EvidenceType, RequirementKind, SectionType, Visibility } from "./types";

export const APPLICATION_STATUSES: ApplicationStatus[] = [
  "INTERESTED", "PLANNED", "WRITING", "SUBMITTED", "DOCUMENT_PASSED", "TEST", "INTERVIEW", "ACCEPTED", "REJECTED", "WITHDRAWN",
];
export const CLOSED_STATUSES: ApplicationStatus[] = ["ACCEPTED", "REJECTED", "WITHDRAWN"];
export const OPEN_STATUSES: ApplicationStatus[] = APPLICATION_STATUSES.filter((s) => !CLOSED_STATUSES.includes(s));

export const STATUS_LABEL: Record<ApplicationStatus, string> = {
  INTERESTED: "관심", PLANNED: "지원 예정", WRITING: "작성 중", SUBMITTED: "제출", DOCUMENT_PASSED: "서류 통과",
  TEST: "필기·코테", INTERVIEW: "면접", ACCEPTED: "최종 합격", REJECTED: "불합격", WITHDRAWN: "지원 철회",
};
export const REQUIREMENT_KINDS: RequirementKind[] = ["RESUME", "ESSAY", "TRANSCRIPT", "CERTIFICATE", "LANGUAGE", "PORTFOLIO", "EXPERIENCE_DESC", "OTHER"];
export const REQUIREMENT_KIND_LABEL: Record<RequirementKind, string> = {
  RESUME: "이력서", ESSAY: "자기소개서", TRANSCRIPT: "성적증명서", CERTIFICATE: "자격증", LANGUAGE: "어학",
  PORTFOLIO: "포트폴리오", EXPERIENCE_DESC: "경험기술서", OTHER: "기타",
};
export const ESSAY_STATUSES: EssayStatus[] = ["EMPTY", "DRAFT", "DONE"];
export const ESSAY_STATUS_LABEL: Record<EssayStatus, string> = { EMPTY: "미작성", DRAFT: "초안", DONE: "완료" };
export const SECTION_TYPES: SectionType[] = ["OVERVIEW", "FEATURES", "ARCHITECTURE", "TROUBLESHOOTING", "IMPROVEMENTS", "LINKS"];
export const SECTION_LABEL: Record<SectionType, string> = {
  OVERVIEW: "개요", FEATURES: "기능", ARCHITECTURE: "아키텍처", TROUBLESHOOTING: "트러블슈팅", IMPROVEMENTS: "개선점", LINKS: "링크",
};
export const EVIDENCE_TYPES: EvidenceType[] = ["PR", "COMMIT", "NOTE", "URL"];
export const EVIDENCE_LABEL: Record<EvidenceType, string> = { PR: "PR", COMMIT: "커밋", NOTE: "노트", URL: "링크" };
export const VISIBILITY_LABEL: Record<Visibility, string> = { PRIVATE: "비공개", PUBLIC: "공개" };
export const TECH_CATEGORIES = ["LANGUAGE", "FRAMEWORK", "DATABASE", "INFRA", "TOOL"] as const;
export const TECH_CATEGORY_LABEL: Record<(typeof TECH_CATEGORIES)[number], string> = {
  LANGUAGE: "언어", FRAMEWORK: "프레임워크", DATABASE: "데이터베이스", INFRA: "인프라", TOOL: "도구",
};
