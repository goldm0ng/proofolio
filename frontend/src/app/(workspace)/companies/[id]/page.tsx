"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { useLoadOnMount } from "@/lib/hooks";
import type { CompanyDetail, CompanyUpsert } from "@/lib/types";
import { STATUS_LABEL } from "@/lib/labels";
import { dday, fmtDate } from "@/lib/format";
import { Button, Card, Chip, ConfirmDialog, ErrorBox, Field, Input, Loading, PageHeader, TagInput, Textarea, useToast } from "@/components/ui";

export default function CompanyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const toast = useToast();
  const [c, setC] = useState<CompanyDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<CompanyUpsert | null>(null);
  const [saving, setSaving] = useState(false);
  const [confirm, setConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    try {
      const d = await api.companies.get(id);
      setC(d);
      setForm({ name: d.name, industry: d.industry ?? "", website: d.website ?? "", talentProfile: d.talentProfile ?? "", coreValues: d.coreValues, techStack: d.techStack, hiringProcess: d.hiringProcess ?? "", notes: d.notes ?? "" });
    } catch (e) { setError(errorMessage(e)); }
  }, [id]);
  useLoadOnMount(load);

  const save = async () => {
    if (!form) return;
    setSaving(true);
    try { await api.companies.update(id, form); toast.push("저장했습니다.", "ok"); load(); }
    catch (e) { toast.push(errorMessage(e), "error"); } finally { setSaving(false); }
  };
  const remove = async () => {
    setDeleting(true);
    try { await api.companies.remove(id); toast.push("삭제했습니다.", "ok"); router.push("/companies"); }
    catch (e) { toast.push(errorMessage(e), "error"); setDeleting(false); setConfirm(false); }
  };

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!c || !form) return <Loading />;

  return (
    <>
      <PageHeader title={c.name} description={<><Link href="/companies" className="hover:underline">기업</Link> / {c.name}</>} actions={<><Button variant="danger" onClick={() => setConfirm(true)}>삭제</Button><Button variant="primary" onClick={save} loading={saving}>저장</Button></>} />
      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="기업 프로필" className="lg:col-span-2">
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="이름"><Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
            <Field label="산업"><Input value={form.industry ?? ""} onChange={(e) => setForm({ ...form, industry: e.target.value })} /></Field>
            <Field label="웹사이트" className="sm:col-span-2"><Input value={form.website ?? ""} onChange={(e) => setForm({ ...form, website: e.target.value })} /></Field>
            <Field label="인재상" className="sm:col-span-2"><Textarea value={form.talentProfile ?? ""} onChange={(e) => setForm({ ...form, talentProfile: e.target.value })} /></Field>
            <Field label="핵심 가치" className="sm:col-span-2"><TagInput value={form.coreValues} onChange={(v) => setForm({ ...form, coreValues: v })} /></Field>
            <Field label="기술 스택" className="sm:col-span-2"><TagInput value={form.techStack} onChange={(v) => setForm({ ...form, techStack: v })} /></Field>
            <Field label="채용 프로세스" className="sm:col-span-2"><Textarea value={form.hiringProcess ?? ""} onChange={(e) => setForm({ ...form, hiringProcess: e.target.value })} /></Field>
            <Field label="메모" className="sm:col-span-2"><Textarea value={form.notes ?? ""} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></Field>
          </div>
        </Card>
        <Card title={`지원 (${c.applications.length})`}>
          {c.applications.length === 0 ? <p className="text-sm text-muted">이 회사에 지원한 기록이 없습니다.</p> : (
            <ul className="divide-y divide-line">
              {c.applications.map((a) => {
                const d = dday(a.deadlineAt);
                return (
                  <li key={a.id} className="py-2">
                    <Link href={`/applications/${a.id}`} className="text-sm font-medium hover:underline">{a.positionTitle}</Link>
                    <div className="mt-0.5 flex items-center gap-2 text-xs text-muted">
                      <Chip>{STATUS_LABEL[a.status]}</Chip>
                      {d && <span className="tabular">{d.text}</span>}
                      <span className="tabular">{fmtDate(a.deadlineAt)}</span>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </Card>
      </div>
      <ConfirmDialog open={confirm} title="기업을 삭제할까요?" description="지원 기록이 있으면 삭제할 수 없습니다." onConfirm={remove} onCancel={() => setConfirm(false)} loading={deleting} />
    </>
  );
}
