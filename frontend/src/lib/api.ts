import type {
  AiCall, ApiError, ApplicationCard, ApplicationCreate, ApplicationDetail, ApplicationStatus, ApplicationUpdate, Board,
  Company, CompanyDetail, CompanyUpsert, EssayQuestion, EssayStatus, Experience, ExperienceSummary, ExperienceUpsert,
  ExtractResponse, ImportPreview, ImportResult, JobPostingExtraction, Metric, NotionMapping, ProjectDetail,
  ProjectSummary, ProjectUpsert, Requirement, RequirementKind, Section, TagCount, TechStackItem,
} from "./types";

export class ApiRequestError extends Error {
  code: string;
  status: number;
  details?: Record<string, unknown>;
  constructor(status: number, body: ApiError) {
    super(body.message);
    this.code = body.code;
    this.status = status;
    this.details = body.details;
  }
}

const BASE = "/api/backend";

async function request<T>(method: string, path: string, body?: unknown, raw?: FormData): Promise<T> {
  const init: RequestInit = { method, headers: {} };
  if (raw) init.body = raw;
  else if (body !== undefined) {
    init.headers = { "content-type": "application/json" };
    init.body = JSON.stringify(body);
  }
  const res = await fetch(`${BASE}${path}`, init);
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  let parsed: unknown = null;
  try { parsed = text ? JSON.parse(text) : null; } catch { parsed = null; }
  if (!res.ok) {
    const err = (parsed && typeof parsed === "object" && "message" in parsed)
      ? (parsed as ApiError)
      : { code: `HTTP_${res.status}`, message: text || `요청 실패 (${res.status})` };
    throw new ApiRequestError(res.status, err);
  }
  return parsed as T;
}

const q = (params: Record<string, string | number | undefined | null>) => {
  const sp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v !== undefined && v !== null && v !== "") sp.set(k, String(v));
  const s = sp.toString();
  return s ? `?${s}` : "";
};

export const api = {
  companies: {
    list: (search?: string) => request<Company[]>("GET", `/companies${q({ q: search })}`),
    create: (b: CompanyUpsert) => request<Company>("POST", "/companies", b),
    get: (id: string) => request<CompanyDetail>("GET", `/companies/${id}`),
    update: (id: string, b: CompanyUpsert) => request<Company>("PUT", `/companies/${id}`, b),
    remove: (id: string) => request<void>("DELETE", `/companies/${id}`),
  },
  applications: {
    list: (p: { status?: ApplicationStatus; companyId?: string } = {}) => request<ApplicationCard[]>("GET", `/applications${q(p)}`),
    board: () => request<Board>("GET", "/applications/board"),
    upcoming: (days = 14) => request<ApplicationCard[]>("GET", `/applications/upcoming${q({ days })}`),
    create: (b: ApplicationCreate) => request<ApplicationDetail>("POST", "/applications", b),
    get: (id: string) => request<ApplicationDetail>("GET", `/applications/${id}`),
    update: (id: string, b: ApplicationUpdate) => request<ApplicationDetail>("PUT", `/applications/${id}`, b),
    setStatus: (id: string, status: ApplicationStatus, note?: string) => request<ApplicationDetail>("PATCH", `/applications/${id}/status`, { status, note }),
    remove: (id: string) => request<void>("DELETE", `/applications/${id}`),
    addRequirement: (id: string, b: { title: string; kind: RequirementKind; note?: string }) => request<Requirement>("POST", `/applications/${id}/requirements`, b),
    updateRequirement: (id: string, rid: string, b: Partial<Pick<Requirement, "title" | "kind" | "done" | "note">>) => request<Requirement>("PATCH", `/applications/${id}/requirements/${rid}`, b),
    removeRequirement: (id: string, rid: string) => request<void>("DELETE", `/applications/${id}/requirements/${rid}`),
    addEssay: (id: string, b: { question: string; maxLength?: number; sortOrder?: number }) => request<EssayQuestion>("POST", `/applications/${id}/essay-questions`, b),
    updateEssay: (id: string, qid: string, b: { question?: string; maxLength?: number | null; draft?: string; status?: EssayStatus; sortOrder?: number }) => request<EssayQuestion>("PATCH", `/applications/${id}/essay-questions/${qid}`, b),
    removeEssay: (id: string, qid: string) => request<void>("DELETE", `/applications/${id}/essay-questions/${qid}`),
    fromExtraction: (extraction: JobPostingExtraction, companyId?: string) => request<ApplicationDetail>("POST", "/applications/from-extraction", { extraction, companyId }),
  },
  projects: {
    list: () => request<ProjectSummary[]>("GET", "/projects"),
    create: (b: ProjectUpsert) => request<ProjectDetail>("POST", "/projects", b),
    get: (id: string) => request<ProjectDetail>("GET", `/projects/${id}`),
    update: (id: string, b: ProjectUpsert) => request<ProjectDetail>("PUT", `/projects/${id}`, b),
    setSections: (id: string, sections: Section[]) => request<ProjectDetail>("PUT", `/projects/${id}/sections`, { sections }),
    setTechStack: (id: string, items: TechStackItem[]) => request<ProjectDetail>("PUT", `/projects/${id}/tech-stack`, { items }),
    setMetrics: (id: string, items: Metric[]) => request<ProjectDetail>("PUT", `/projects/${id}/metrics`, { items }),
    remove: (id: string) => request<void>("DELETE", `/projects/${id}`),
  },
  experiences: {
    list: (p: { projectId?: string; tag?: string; q?: string } = {}) => request<ExperienceSummary[]>("GET", `/experiences${q(p)}`),
    tags: () => request<TagCount[]>("GET", "/experiences/tags"),
    create: (b: ExperienceUpsert) => request<Experience>("POST", "/experiences", b),
    get: (id: string) => request<Experience>("GET", `/experiences/${id}`),
    update: (id: string, b: ExperienceUpsert) => request<Experience>("PUT", `/experiences/${id}`, b),
    remove: (id: string) => request<void>("DELETE", `/experiences/${id}`),
  },
  ai: {
    extractPosting: (b: { url?: string; text?: string }) => request<ExtractResponse>("POST", "/ai/extract-posting", b),
    calls: (limit = 50) => request<AiCall[]>("GET", `/ai/calls${q({ limit })}`),
  },
  importer: {
    preview: (file: File) => {
      const fd = new FormData();
      fd.append("file", file);
      return request<ImportPreview>("POST", "/import/notion/preview", undefined, fd);
    },
    run: (file: File, mapping: NotionMapping) => {
      const fd = new FormData();
      fd.append("file", file);
      fd.append("mapping", new Blob([JSON.stringify(mapping)], { type: "application/json" }));
      return request<ImportResult>("POST", "/import/notion", undefined, fd);
    },
  },
};

export function errorMessage(e: unknown): string {
  if (e instanceof ApiRequestError) return e.message;
  if (e instanceof Error) return e.message;
  return "알 수 없는 오류가 발생했습니다.";
}
