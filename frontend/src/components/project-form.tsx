"use client";

import type { ProjectUpsert } from "@/lib/types";
import { VISIBILITY_LABEL } from "@/lib/labels";
import { Field, Input, Select } from "@/components/ui";

export function ProjectFormFields({ form, onChange }: { form: ProjectUpsert; onChange: (f: ProjectUpsert) => void }) {
  const set = <K extends keyof ProjectUpsert>(k: K, v: ProjectUpsert[K]) => onChange({ ...form, [k]: v });
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      <Field label="이름"><Input value={form.name} onChange={(e) => set("name", e.target.value)} /></Field>
      <Field label="한 줄 소개"><Input value={form.tagline ?? ""} onChange={(e) => set("tagline", e.target.value)} /></Field>
      <Field label="시작"><Input type="date" value={form.startedAt ?? ""} onChange={(e) => set("startedAt", e.target.value)} /></Field>
      <Field label="종료" hint="진행 중이면 비워두기"><Input type="date" value={form.endedAt ?? ""} onChange={(e) => set("endedAt", e.target.value)} /></Field>
      <Field label="팀 규모"><Input type="number" min={1} value={form.teamSize ?? ""} onChange={(e) => set("teamSize", e.target.value ? Number(e.target.value) : undefined)} /></Field>
      <Field label="내 역할"><Input value={form.role ?? ""} onChange={(e) => set("role", e.target.value)} placeholder="예: 백엔드 · API 설계, 배포" /></Field>
      <Field label="저장소 URL"><Input value={form.repoUrl ?? ""} onChange={(e) => set("repoUrl", e.target.value)} /></Field>
      <Field label="배포 URL"><Input value={form.deployUrl ?? ""} onChange={(e) => set("deployUrl", e.target.value)} /></Field>
      <Field label="공개 범위">
        <Select value={form.visibility ?? "PRIVATE"} onChange={(e) => set("visibility", e.target.value as ProjectUpsert["visibility"])}>
          <option value="PRIVATE">{VISIBILITY_LABEL.PRIVATE}</option><option value="PUBLIC">{VISIBILITY_LABEL.PUBLIC}</option>
        </Select>
      </Field>
    </div>
  );
}

export function cleanProject(f: ProjectUpsert): ProjectUpsert {
  const s = (v?: string) => (v && v.trim() ? v.trim() : undefined);
  return { name: f.name.trim(), tagline: s(f.tagline), startedAt: s(f.startedAt), endedAt: s(f.endedAt), teamSize: f.teamSize || undefined, role: s(f.role), repoUrl: s(f.repoUrl), deployUrl: s(f.deployUrl), visibility: f.visibility ?? "PRIVATE" };
}
