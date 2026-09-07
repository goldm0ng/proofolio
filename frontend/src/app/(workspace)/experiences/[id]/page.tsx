"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { useLoadOnMount } from "@/lib/hooks";
import type { Experience, ExperienceUpsert } from "@/lib/types";
import { fmtDateTime } from "@/lib/format";
import { Button, Card, ConfirmDialog, ErrorBox, Loading, PageHeader, useToast } from "@/components/ui";
import { ExperienceForm, cleanExperience } from "@/components/experience-form";

export default function ExperienceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const toast = useToast();
  const [exp, setExp] = useState<Experience | null>(null);
  const [form, setForm] = useState<ExperienceUpsert | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [confirm, setConfirm] = useState(false);

  const load = useCallback(async () => {
    try {
      const e = await api.experiences.get(id);
      setExp(e);
      setForm({ projectId: e.projectId ?? null, title: e.title, situation: e.situation, task: e.task, action: e.action, result: e.result, tags: e.tags, evidence: e.evidence, visibility: e.visibility });
    } catch (err) { setError(errorMessage(err)); }
  }, [id]);
  useLoadOnMount(load);

  const save = async () => {
    if (!form) return;
    setSaving(true);
    try { await api.experiences.update(id, cleanExperience(form)); toast.push("저장했습니다.", "ok"); load(); }
    catch (err) { toast.push(errorMessage(err), "error"); } finally { setSaving(false); }
  };

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!exp || !form) return <Loading />;
  return (
    <>
      <PageHeader title={exp.title} description={<><Link href="/experiences" className="hover:underline">경험</Link>{exp.projectName && <> / <Link href={`/projects/${exp.projectId}`} className="hover:underline">{exp.projectName}</Link></>} · 수정 {fmtDateTime(exp.updatedAt)}</>} actions={<><Button variant="danger" onClick={() => setConfirm(true)}>삭제</Button><Button variant="primary" onClick={save} loading={saving}>저장</Button></>} />
      <Card><ExperienceForm form={form} onChange={setForm} /></Card>
      <ConfirmDialog open={confirm} title="경험을 삭제할까요?" onConfirm={async () => { try { await api.experiences.remove(id); toast.push("삭제했습니다.", "ok"); router.push("/experiences"); } catch (err) { toast.push(errorMessage(err), "error"); setConfirm(false); } }} onCancel={() => setConfirm(false)} />
    </>
  );
}
