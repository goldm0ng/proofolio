"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { useLoadOnMount } from "@/lib/hooks";
import type { Metric, ProjectDetail, ProjectUpsert, Section, SectionType, TechStackItem } from "@/lib/types";
import { SECTION_LABEL, SECTION_TYPES, TECH_CATEGORIES, TECH_CATEGORY_LABEL, VISIBILITY_LABEL } from "@/lib/labels";
import { fmtDate } from "@/lib/format";
import { Button, Card, Chip, ConfirmDialog, ErrorBox, Input, LinkButton, Loading, Select, Textarea, useToast } from "@/components/ui";
import { ProjectFormFields, cleanProject } from "@/components/project-form";

function toUpsert(p: ProjectDetail): ProjectUpsert {
  return { name: p.name, tagline: p.tagline ?? "", startedAt: p.startedAt ?? "", endedAt: p.endedAt ?? "", teamSize: p.teamSize ?? undefined, role: p.role ?? "", repoUrl: p.repoUrl ?? "", deployUrl: p.deployUrl ?? "", visibility: p.visibility };
}

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const toast = useToast();
  const [p, setP] = useState<ProjectDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [editHeader, setEditHeader] = useState(false);
  const [form, setForm] = useState<ProjectUpsert | null>(null);
  const [sections, setSections] = useState<Record<SectionType, string>>({ OVERVIEW: "", FEATURES: "", ARCHITECTURE: "", TROUBLESHOOTING: "", IMPROVEMENTS: "", LINKS: "" });
  const [stack, setStack] = useState<TechStackItem[]>([]);
  const [metrics, setMetrics] = useState<Metric[]>([]);
  const [busy, setBusy] = useState<string | null>(null);
  const [confirm, setConfirm] = useState(false);

  const apply = (d: ProjectDetail) => {
    setP(d); setForm(toUpsert(d));
    const s = { OVERVIEW: "", FEATURES: "", ARCHITECTURE: "", TROUBLESHOOTING: "", IMPROVEMENTS: "", LINKS: "" } as Record<SectionType, string>;
    d.sections.forEach((x) => { s[x.type] = x.body; });
    setSections(s); setStack(d.techStackItems); setMetrics(d.metrics);
  };
  const load = useCallback(async () => {
    try { apply(await api.projects.get(id)); setError(null); } catch (e) { setError(errorMessage(e)); }
  }, [id]);
  useLoadOnMount(load);

  const run = async (key: string, fn: () => Promise<ProjectDetail>, msg = "저장했습니다.") => {
    setBusy(key);
    try { apply(await fn()); toast.push(msg, "ok"); } catch (e) { toast.push(errorMessage(e), "error"); } finally { setBusy(null); }
  };
  const saveSections = () => run("sections", () => api.projects.setSections(id, SECTION_TYPES.map((t) => ({ type: t, body: sections[t] })).filter((s) => s.body.trim()) as Section[]));
  const saveStack = () => run("stack", () => api.projects.setTechStack(id, stack.filter((s) => s.name.trim())));
  const saveMetrics = () => run("metrics", () => api.projects.setMetrics(id, metrics.filter((m) => m.label.trim())));

  const [newTechCat, setNewTechCat] = useState<string>("LANGUAGE"); const [newTech, setNewTech] = useState("");
  const addTech = () => { if (!newTech.trim()) return; setStack([...stack, { category: newTechCat, name: newTech.trim() }]); setNewTech(""); };

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!p || !form) return <Loading />;

  const sectionsDirty = SECTION_TYPES.some((t) => (p.sections.find((s) => s.type === t)?.body ?? "") !== sections[t]);
  const stackDirty = JSON.stringify(stack) !== JSON.stringify(p.techStackItems);
  const metricsDirty = JSON.stringify(metrics) !== JSON.stringify(p.metrics);

  return (
    <>
      <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="text-xs text-muted"><Link href="/projects" className="hover:underline">프로젝트</Link> / {p.name}</div>
          <h1 className="mt-0.5 flex items-center gap-2 text-xl font-semibold tracking-tight">{p.name} <Chip tone={p.visibility === "PUBLIC" ? "ok" : "neutral"}>{VISIBILITY_LABEL[p.visibility]}</Chip></h1>
          {p.tagline && <p className="text-sm text-ink-2">{p.tagline}</p>}
          <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted">
            <span className="tabular">{fmtDate(p.startedAt)} ~ {p.endedAt ? fmtDate(p.endedAt) : "진행 중"}</span>
            {p.teamSize && <span>{p.teamSize}명</span>}
            {p.role && <span>{p.role}</span>}
            {p.repoUrl && <a className="text-accent-ink hover:underline" href={p.repoUrl} target="_blank" rel="noreferrer">저장소</a>}
            {p.deployUrl && <a className="text-accent-ink hover:underline" href={p.deployUrl} target="_blank" rel="noreferrer">배포</a>}
          </div>
        </div>
        <div className="flex gap-2">
          <Button onClick={() => setEditHeader((v) => !v)}>{editHeader ? "닫기" : "기본 정보 편집"}</Button>
          <Button variant="danger" onClick={() => setConfirm(true)}>삭제</Button>
        </div>
      </div>

      {editHeader && (
        <Card className="mb-4" title="기본 정보">
          <ProjectFormFields form={form} onChange={setForm} />
          <div className="mt-3 flex justify-end gap-2">
            <Button onClick={() => { setForm(toUpsert(p)); setEditHeader(false); }}>취소</Button>
            <Button variant="primary" loading={busy === "header"} onClick={() => run("header", () => api.projects.update(id, cleanProject(form))).then(() => setEditHeader(false))}>저장</Button>
          </div>
        </Card>
      )}

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="flex flex-col gap-4 lg:col-span-2">
          <Card title="섹션" actions={<Button size="sm" variant="primary" onClick={saveSections} loading={busy === "sections"} disabled={!sectionsDirty}>저장</Button>}>
            <div className="flex flex-col gap-4">
              {SECTION_TYPES.map((t) => (
                <div key={t}>
                  <div className="mb-1 text-xs font-medium text-ink-2">{SECTION_LABEL[t]}</div>
                  <Textarea rows={t === "OVERVIEW" || t === "LINKS" ? 3 : 5} value={sections[t]} onChange={(e) => setSections({ ...sections, [t]: e.target.value })} placeholder={placeholderFor(t)} />
                </div>
              ))}
            </div>
          </Card>
          <Card title={`경험 (${p.experiences.length})`} actions={<LinkButton size="sm" href={`/experiences/new?projectId=${p.id}`}>경험 추가</LinkButton>}>
            {p.experiences.length === 0 ? <p className="text-sm text-muted">이 프로젝트에서 뽑은 경험이 없습니다. STAR 단위로 5~8개가 적당합니다.</p> : (
              <ul className="divide-y divide-line">
                {p.experiences.map((e) => (
                  <li key={e.id} className="flex items-center gap-2 py-2">
                    <Link href={`/experiences/${e.id}`} className="min-w-0 flex-1 text-sm font-medium hover:underline">{e.title}</Link>
                    <div className="flex flex-wrap gap-1">{e.tags.map((t) => <Chip key={t} tone="accent">{t}</Chip>)}</div>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>

        <div className="flex flex-col gap-4">
          <Card title="기술 스택" actions={<Button size="sm" variant="primary" onClick={saveStack} loading={busy === "stack"} disabled={!stackDirty}>저장</Button>}>
            <div className="flex flex-col gap-3">
              {TECH_CATEGORIES.map((cat) => {
                const items = stack.filter((s) => s.category === cat);
                if (items.length === 0) return null;
                return (
                  <div key={cat}>
                    <div className="mb-1 text-[11px] text-muted">{TECH_CATEGORY_LABEL[cat]}</div>
                    <div className="flex flex-wrap gap-1">
                      {items.map((s, i) => (
                        <span key={`${s.name}-${i}`} className="inline-flex items-center gap-1 rounded-sm border border-line bg-surface px-1.5 py-0.5 text-xs">
                          {s.name}<button type="button" className="opacity-60 hover:opacity-100" aria-label="제거" onClick={() => setStack(stack.filter((x) => x !== s))}>×</button>
                        </span>
                      ))}
                    </div>
                  </div>
                );
              })}
              {stack.some((s) => !TECH_CATEGORIES.includes(s.category as (typeof TECH_CATEGORIES)[number])) && (
                <div className="flex flex-wrap gap-1">{stack.filter((s) => !TECH_CATEGORIES.includes(s.category as (typeof TECH_CATEGORIES)[number])).map((s, i) => <Chip key={i}>{s.category}: {s.name}</Chip>)}</div>
              )}
              <div className="flex gap-1.5 border-t border-line pt-3">
                <Select className="!w-28" value={newTechCat} onChange={(e) => setNewTechCat(e.target.value)}>{TECH_CATEGORIES.map((c) => <option key={c} value={c}>{TECH_CATEGORY_LABEL[c]}</option>)}</Select>
                <Input value={newTech} placeholder="예: Spring Boot" onChange={(e) => setNewTech(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") addTech(); }} />
                <Button size="sm" onClick={addTech} disabled={!newTech.trim()}>추가</Button>
              </div>
            </div>
          </Card>
          <Card title="성과 지표" actions={<Button size="sm" variant="primary" onClick={saveMetrics} loading={busy === "metrics"} disabled={!metricsDirty}>저장</Button>}>
            <div className="flex flex-col gap-2">
              {metrics.map((m, i) => (
                <div key={i} className="flex gap-1.5">
                  <Input value={m.label} placeholder="지표" onChange={(e) => setMetrics(metrics.map((x, j) => (j === i ? { ...x, label: e.target.value } : x)))} />
                  <Input className="!w-32" value={m.value} placeholder="값" onChange={(e) => setMetrics(metrics.map((x, j) => (j === i ? { ...x, value: e.target.value } : x)))} />
                  <Button variant="danger" size="sm" onClick={() => setMetrics(metrics.filter((_, j) => j !== i))}>삭제</Button>
                </div>
              ))}
              <Button size="sm" onClick={() => setMetrics([...metrics, { label: "", value: "" }])}>지표 추가</Button>
            </div>
          </Card>
        </div>
      </div>
      <ConfirmDialog open={confirm} title="프로젝트를 삭제할까요?" description="연결된 경험은 남고 프로젝트 연결만 풀립니다." onConfirm={async () => { try { await api.projects.remove(id); toast.push("삭제했습니다.", "ok"); router.push("/projects"); } catch (e) { toast.push(errorMessage(e), "error"); setConfirm(false); } }} onCancel={() => setConfirm(false)} />
    </>
  );
}

function placeholderFor(t: SectionType): string {
  switch (t) {
    case "OVERVIEW": return "무엇을 하는 서비스인지, 왜 만들었는지 한두 문단";
    case "FEATURES": return "기능 목록. 한 줄에 하나";
    case "ARCHITECTURE": return "구성 요소와 흐름. 도식 링크가 있으면 함께";
    case "TROUBLESHOOTING": return "상황 / 원인 / 해결 / 배운 점";
    case "IMPROVEMENTS": return "개선점, 미해결 과제";
    case "LINKS": return "저장소, 배포, PR, 문서 링크";
  }
}
