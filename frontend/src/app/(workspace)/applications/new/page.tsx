"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { ApiRequestError, api, errorMessage } from "@/lib/api";
import type { ApplicationCreate, JobPostingExtraction, RequirementKind } from "@/lib/types";
import { REQUIREMENT_KINDS, REQUIREMENT_KIND_LABEL } from "@/lib/labels";
import { fromLocalInput, toLocalInput } from "@/lib/format";
import { Button, Card, Chip, ErrorBox, Field, Input, PageHeader, Select, TagInput, Textarea, useToast } from "@/components/ui";

type Mode = "extract" | "manual";

function looksLikeUrl(s: string) { return /^https?:\/\/\S+$/i.test(s.trim()); }

function ExtractMode() {
  const router = useRouter();
  const toast = useToast();
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ext, setExt] = useState<JobPostingExtraction | null>(null);
  const [saving, setSaving] = useState(false);

  const extract = async () => {
    setError(null); setLoading(true);
    try {
      const body = looksLikeUrl(input) ? { url: input.trim() } : { text: input };
      const res = await api.ai.extractPosting(body);
      setExt(res.extraction);
    } catch (e) {
      const msg = errorMessage(e);
      if (e instanceof ApiRequestError && (e.status === 422 || e.status === 503)) setError(`${msg} 공고 본문을 직접 붙여넣거나 '직접 입력'으로 전환하세요.`);
      else setError(msg);
    } finally { setLoading(false); }
  };

  const save = async () => {
    if (!ext) return;
    setSaving(true);
    try {
      const created = await api.applications.fromExtraction(ext);
      toast.push("지원 카드를 만들었습니다.", "ok");
      router.push(`/applications/${created.id}`);
    } catch (e) { toast.push(errorMessage(e), "error"); setSaving(false); }
  };

  const set = <K extends keyof JobPostingExtraction>(k: K, v: JobPostingExtraction[K]) => setExt((s) => (s ? { ...s, [k]: v } : s));

  if (!ext) {
    return (
      <Card title="공고 URL 또는 본문">
        <Textarea rows={8} value={input} onChange={(e) => setInput(e.target.value)} placeholder="채용 공고 URL 하나를 붙여넣거나, 공고 본문 전체를 붙여넣으세요." />
        {error && <div className="mt-3"><ErrorBox message={error} /></div>}
        <div className="mt-3 flex items-center justify-between">
          <p className="text-xs text-muted">{looksLikeUrl(input) ? "URL로 인식했습니다. 서버가 페이지를 받아 추출합니다." : "본문 텍스트로 추출합니다."}</p>
          <Button variant="primary" onClick={extract} loading={loading} disabled={!input.trim()}>추출</Button>
        </div>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <Card title="추출 결과 검토" actions={<Button size="sm" onClick={() => setExt(null)}>다시 추출</Button>}>
        {ext.summary && <p className="mb-4 rounded bg-surface-2 p-3 text-sm text-ink-2">{ext.summary}</p>}
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="회사"><Input value={ext.companyName} onChange={(e) => set("companyName", e.target.value)} /></Field>
          <Field label="직무"><Input value={ext.positionTitle} onChange={(e) => set("positionTitle", e.target.value)} /></Field>
          <Field label="고용 형태"><Input value={ext.employmentType ?? ""} onChange={(e) => set("employmentType", e.target.value || null)} /></Field>
          <Field label="근무지"><Input value={ext.location ?? ""} onChange={(e) => set("location", e.target.value || null)} /></Field>
          <Field label="마감"><Input type="datetime-local" value={toLocalInput(ext.deadlineAt)} onChange={(e) => set("deadlineAt", fromLocalInput(e.target.value))} /></Field>
          <Field label="출처 URL"><Input value={ext.sourceUrl ?? ""} onChange={(e) => set("sourceUrl", e.target.value || null)} /></Field>
          <Field label="필수 역량" className="sm:col-span-2"><TagInput value={ext.requiredSkills} onChange={(v) => set("requiredSkills", v)} /></Field>
          <Field label="우대 역량" className="sm:col-span-2"><TagInput value={ext.preferredSkills} onChange={(v) => set("preferredSkills", v)} /></Field>
          <Field label="전형 단계" hint="순서대로" className="sm:col-span-2"><TagInput value={ext.hiringStages} onChange={(v) => set("hiringStages", v)} /></Field>
        </div>
      </Card>

      <Card title={`필요 서류 (${ext.requiredDocuments.length})`} actions={<Button size="sm" onClick={() => set("requiredDocuments", [...ext.requiredDocuments, { title: "", kind: "OTHER" }])}>추가</Button>}>
        {ext.requiredDocuments.length === 0 ? <p className="text-sm text-muted">없음</p> : (
          <ul className="flex flex-col gap-2">
            {ext.requiredDocuments.map((d, i) => (
              <li key={i} className="flex gap-2">
                <Input value={d.title} placeholder="서류 이름" onChange={(e) => set("requiredDocuments", ext.requiredDocuments.map((x, j) => (j === i ? { ...x, title: e.target.value } : x)))} />
                <Select className="!w-40" value={d.kind} onChange={(e) => set("requiredDocuments", ext.requiredDocuments.map((x, j) => (j === i ? { ...x, kind: e.target.value as RequirementKind } : x)))}>
                  {REQUIREMENT_KINDS.map((k) => <option key={k} value={k}>{REQUIREMENT_KIND_LABEL[k]}</option>)}
                </Select>
                <Button variant="danger" size="sm" onClick={() => set("requiredDocuments", ext.requiredDocuments.filter((_, j) => j !== i))}>삭제</Button>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card title={`자소서 문항 (${ext.essayQuestions.length})`} actions={<Button size="sm" onClick={() => set("essayQuestions", [...ext.essayQuestions, { question: "", maxLength: null }])}>추가</Button>}>
        {ext.essayQuestions.length === 0 ? <p className="text-sm text-muted">없음</p> : (
          <ul className="flex flex-col gap-2">
            {ext.essayQuestions.map((qq, i) => (
              <li key={i} className="flex items-start gap-2">
                <Textarea rows={2} value={qq.question} placeholder="문항" onChange={(e) => set("essayQuestions", ext.essayQuestions.map((x, j) => (j === i ? { ...x, question: e.target.value } : x)))} />
                <Input type="number" className="!w-28" placeholder="글자수" value={qq.maxLength ?? ""} onChange={(e) => set("essayQuestions", ext.essayQuestions.map((x, j) => (j === i ? { ...x, maxLength: e.target.value ? Number(e.target.value) : null } : x)))} />
                <Button variant="danger" size="sm" onClick={() => set("essayQuestions", ext.essayQuestions.filter((_, j) => j !== i))}>삭제</Button>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <div className="flex justify-end gap-2">
        <Button onClick={() => setExt(null)}>취소</Button>
        <Button variant="primary" onClick={save} loading={saving} disabled={!ext.companyName.trim() || !ext.positionTitle.trim()}>지원 카드 만들기</Button>
      </div>
    </div>
  );
}

function ManualMode() {
  const router = useRouter();
  const toast = useToast();
  const [form, setForm] = useState<ApplicationCreate>({ companyName: "", positionTitle: "" });
  const [deadline, setDeadline] = useState("");
  const [saving, setSaving] = useState(false);
  const set = <K extends keyof ApplicationCreate>(k: K, v: ApplicationCreate[K]) => setForm((s) => ({ ...s, [k]: v }));

  const save = async () => {
    setSaving(true);
    try {
      const created = await api.applications.create({ ...form, deadlineAt: fromLocalInput(deadline) ?? undefined });
      toast.push("지원 카드를 만들었습니다.", "ok");
      router.push(`/applications/${created.id}`);
    } catch (e) { toast.push(errorMessage(e), "error"); setSaving(false); }
  };

  return (
    <Card title="직접 입력">
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="회사" hint="같은 이름의 기업이 있으면 재사용합니다"><Input value={form.companyName ?? ""} onChange={(e) => set("companyName", e.target.value)} /></Field>
        <Field label="직무"><Input value={form.positionTitle} onChange={(e) => set("positionTitle", e.target.value)} /></Field>
        <Field label="공고 URL"><Input value={form.postingUrl ?? ""} onChange={(e) => set("postingUrl", e.target.value)} /></Field>
        <Field label="마감"><Input type="datetime-local" value={deadline} onChange={(e) => setDeadline(e.target.value)} /></Field>
        <Field label="고용 형태"><Input value={form.employmentType ?? ""} onChange={(e) => set("employmentType", e.target.value)} /></Field>
        <Field label="근무지"><Input value={form.location ?? ""} onChange={(e) => set("location", e.target.value)} /></Field>
        <Field label="메모" className="sm:col-span-2"><Textarea value={form.notes ?? ""} onChange={(e) => set("notes", e.target.value)} /></Field>
      </div>
      <div className="mt-4 flex justify-end">
        <Button variant="primary" onClick={save} loading={saving} disabled={!form.companyName?.trim() || !form.positionTitle.trim()}>지원 카드 만들기</Button>
      </div>
    </Card>
  );
}

export default function NewApplicationPage() {
  const [mode, setMode] = useState<Mode>("extract");
  return (
    <>
      <PageHeader title="공고 등록" description="URL이나 본문에서 추출하거나 직접 입력합니다" />
      <div className="mb-4 flex gap-2">
        <Chip tone={mode === "extract" ? "accent" : "neutral"} onClick={() => setMode("extract")} className="!px-3 !py-1 !text-xs">공고 URL/본문으로 추출</Chip>
        <Chip tone={mode === "manual" ? "accent" : "neutral"} onClick={() => setMode("manual")} className="!px-3 !py-1 !text-xs">직접 입력</Chip>
      </div>
      {mode === "extract" ? <ExtractMode /> : <ManualMode />}
    </>
  );
}
