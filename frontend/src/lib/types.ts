export type Visibility = "PRIVATE" | "PUBLIC";
export type ApplicationStatus =
  | "INTERESTED" | "PLANNED" | "WRITING" | "SUBMITTED" | "DOCUMENT_PASSED"
  | "TEST" | "INTERVIEW" | "ACCEPTED" | "REJECTED" | "WITHDRAWN";
export type RequirementKind =
  | "RESUME" | "ESSAY" | "TRANSCRIPT" | "CERTIFICATE" | "LANGUAGE"
  | "PORTFOLIO" | "EXPERIENCE_DESC" | "OTHER";
export type EssayStatus = "EMPTY" | "DRAFT" | "DONE";
export type SectionType = "OVERVIEW" | "FEATURES" | "ARCHITECTURE" | "TROUBLESHOOTING" | "IMPROVEMENTS" | "LINKS";
export type EvidenceType = "PR" | "COMMIT" | "NOTE" | "URL";

export interface ApiError { code: string; message: string; details?: Record<string, unknown> }

export interface Company {
  id: string; name: string; industry?: string | null; website?: string | null;
  talentProfile?: string | null; coreValues: string[]; techStack: string[];
  hiringProcess?: string | null; notes?: string | null; createdAt: string; updatedAt: string;
}
export type CompanyUpsert = Omit<Company, "id" | "createdAt" | "updatedAt">;
export interface CompanyDetail extends Company { applications: ApplicationCard[] }

export interface ApplicationCard {
  id: string; companyId: string; companyName: string; positionTitle: string; status: ApplicationStatus;
  deadlineAt?: string | null; postingUrl?: string | null;
  requirementsDone: number; requirementsTotal: number; essayDone: number; essayTotal: number;
  updatedAt: string;
}
export interface Requirement { id: string; title: string; kind: RequirementKind; done: boolean; note?: string | null; sortOrder: number }
export interface EssayQuestion { id: string; question: string; maxLength?: number | null; draft?: string | null; status: EssayStatus; sortOrder: number; updatedAt: string }
export interface StatusHistoryItem { status: ApplicationStatus; note?: string | null; changedAt: string }
export interface ApplicationDetail extends ApplicationCard {
  employmentType?: string | null; location?: string | null; notes?: string | null;
  result?: string | null; retrospective?: string | null;
  company: Company; requirements: Requirement[]; essayQuestions: EssayQuestion[];
  statusHistory: StatusHistoryItem[]; createdAt: string;
}
export interface ApplicationCreate {
  companyId?: string; companyName?: string; positionTitle: string; postingUrl?: string; deadlineAt?: string;
  employmentType?: string; location?: string; notes?: string; status?: ApplicationStatus;
  requirements?: { title: string; kind: RequirementKind }[];
  essayQuestions?: { question: string; maxLength?: number }[];
}
export interface ApplicationUpdate {
  positionTitle?: string; postingUrl?: string | null; deadlineAt?: string | null; employmentType?: string | null;
  location?: string | null; notes?: string | null; result?: string | null; retrospective?: string | null;
}
export interface Board { columns: { status: ApplicationStatus; items: ApplicationCard[] }[] }

export interface TechStackItem { category: string; name: string }
export interface Metric { label: string; value: string }
export interface Section { type: SectionType; body: string }
export interface ProjectSummary {
  id: string; name: string; tagline?: string | null; startedAt?: string | null; endedAt?: string | null;
  teamSize?: number | null; role?: string | null; repoUrl?: string | null; deployUrl?: string | null;
  visibility: Visibility; experienceCount: number; techStack: string[]; updatedAt: string;
}
export interface ProjectDetail extends ProjectSummary {
  sections: Section[]; techStackItems: TechStackItem[]; metrics: Metric[]; experiences: ExperienceSummary[]; createdAt: string;
}
export interface ProjectUpsert {
  name: string; tagline?: string; startedAt?: string; endedAt?: string; teamSize?: number;
  role?: string; repoUrl?: string; deployUrl?: string; visibility?: Visibility;
}

export interface Evidence { type: EvidenceType; url: string; label?: string | null }
export interface ExperienceSummary {
  id: string; projectId?: string | null; projectName?: string | null; title: string; tags: string[];
  visibility: Visibility; updatedAt: string;
}
export interface Experience extends ExperienceSummary {
  situation: string; task: string; action: string; result: string; evidence: Evidence[]; createdAt: string;
}
export interface ExperienceUpsert {
  projectId?: string | null; title: string; situation: string; task: string; action: string; result: string;
  tags?: string[]; evidence?: Evidence[]; visibility?: Visibility;
}
export interface TagCount { tag: string; count: number }

export interface JobPostingExtraction {
  companyName: string; positionTitle: string; employmentType?: string | null; location?: string | null;
  deadlineAt?: string | null; requiredSkills: string[]; preferredSkills: string[]; hiringStages: string[];
  requiredDocuments: { title: string; kind: RequirementKind }[];
  essayQuestions: { question: string; maxLength?: number | null }[];
  summary: string; sourceUrl?: string | null;
}
export interface ExtractResponse { extraction: JobPostingExtraction; sourceText: string; callId: string }
export interface AiCall {
  id: string; task: string; model: string; status: "OK" | "ERROR"; inputTokens: number; outputTokens: number;
  cacheReadTokens: number; costUsd: number; durationMs: number; errorMessage?: string | null; createdAt: string;
}

export interface NotionMapping {
  companyName: string; positionTitle?: string; deadlineAt?: string; status?: string; postingUrl?: string;
  notes?: string[]; statusValues?: Record<string, ApplicationStatus>;
}
export interface ImportPreview { columns: string[]; sampleRows: string[][]; suggestedMapping: NotionMapping; rowCount: number }
export interface ImportResult { created: number; updated: number; skipped: number; errors: { row: number; message: string }[] }
