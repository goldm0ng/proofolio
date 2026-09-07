"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { Evidence, EvidenceType, ExperienceUpsert, ProjectSummary } from "@/lib/types";
import { EVIDENCE_LABEL, EVIDENCE_TYPES, VISIBILITY_LABEL } from "@/lib/labels";
import { Button, Field, Input, Select, TagInput, Textarea } from "@/components/ui";

export function ExperienceForm({ form, onChange }: { form: ExperienceUpsert; onChange: (f: ExperienceUpsert) => void }) {
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  useEffect(() => { api.projects.list().then(setProjects).catch(() => setProjects([])); }, []);
  const set = <K extends keyof ExperienceUpsert>(k: K, v: ExperienceUpsert[K]) => onChange({ ...form, [k]: v });
  const evidence = form.evidence ?? [];
  const setEv = (i: number, patch: Partial<Evidence>) => set("evidence", evidence.map((e, j) => (j === i ? { ...e, ...patch } : e)));

  return (
    <div className="flex flex-col gap-3">
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="제목" className="sm:col-span-2"><Input value={form.title} onChange={(e) => set("title", e.target.value)} placeholder="예: 조회수 집계 API 응답 시간 70% 단축" /></Field>
        <Field label="프로젝트">
          <Select value={form.projectId ?? ""} onChange={(e) => set("projectId", e.target.value || null)}>
            <option value="">없음</option>
            {projects.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </Select>
        </Field>
        <Field label="공개 범위">
          <Select value={form.visibility ?? "PRIVATE"} onChange={(e) => set("visibility", e.target.value as ExperienceUpsert["visibility"])}>
            <option value="PRIVATE">{VISIBILITY_LABEL.PRIVATE}</option><option value="PUBLIC">{VISIBILITY_LABEL.PUBLIC}</option>
          </Select>
        </Field>
      </div>
      <Field label="Situation · 상황" hint="어떤 맥락, 어떤 제약이 있었나"><Textarea value={form.situation} onChange={(e) => set("situation", e.target.value)} /></Field>
      <Field label="Task · 과제" hint="내가 맡은 목표"><Textarea value={form.task} onChange={(e) => set("task", e.target.value)} /></Field>
      <Field label="Action · 행동" hint="구체적으로 무엇을 했나. 기술 선택의 이유"><Textarea rows={6} value={form.action} onChange={(e) => set("action", e.target.value)} /></Field>
      <Field label="Result · 결과" hint="수치가 있으면 수치로"><Textarea value={form.result} onChange={(e) => set("result", e.target.value)} /></Field>
      <Field label="역량 태그" hint="예: 문제해결, 성능, 장애대응, 협업, 설계"><TagInput value={form.tags ?? []} onChange={(v) => set("tags", v)} /></Field>
      <div>
        <div className="mb-1 flex items-center justify-between"><span className="text-xs font-medium text-ink-2">근거 링크</span><Button size="sm" onClick={() => set("evidence", [...evidence, { type: "PR", url: "", label: "" }])}>추가</Button></div>
        {evidence.length === 0 ? <p className="text-xs text-muted">PR, 커밋, 노트 링크를 붙이면 면접에서 바로 꺼낼 수 있습니다.</p> : (
          <div className="flex flex-col gap-1.5">
            {evidence.map((e, i) => (
              <div key={i} className="flex gap-1.5">
                <Select className="!w-24" value={e.type} onChange={(ev) => setEv(i, { type: ev.target.value as EvidenceType })}>{EVIDENCE_TYPES.map((t) => <option key={t} value={t}>{EVIDENCE_LABEL[t]}</option>)}</Select>
                <Input value={e.url} placeholder="URL" onChange={(ev) => setEv(i, { url: ev.target.value })} />
                <Input className="!w-40" value={e.label ?? ""} placeholder="설명" onChange={(ev) => setEv(i, { label: ev.target.value })} />
                <Button variant="danger" size="sm" onClick={() => set("evidence", evidence.filter((_, j) => j !== i))}>삭제</Button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export function cleanExperience(f: ExperienceUpsert): ExperienceUpsert {
  return {
    projectId: f.projectId || null, title: f.title.trim(), situation: f.situation.trim(), task: f.task.trim(), action: f.action.trim(), result: f.result.trim(),
    tags: (f.tags ?? []).map((t) => t.trim()).filter(Boolean),
    evidence: (f.evidence ?? []).filter((e) => e.url.trim()).map((e) => ({ type: e.type, url: e.url.trim(), label: e.label?.trim() || null })),
    visibility: f.visibility ?? "PRIVATE",
  };
}
export const EMPTY_EXPERIENCE: ExperienceUpsert = { projectId: null, title: "", situation: "", task: "", action: "", result: "", tags: [], evidence: [], visibility: "PRIVATE" };
