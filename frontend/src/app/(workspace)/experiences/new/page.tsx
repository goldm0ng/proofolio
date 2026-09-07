"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import type { ExperienceUpsert } from "@/lib/types";
import { Button, Card, Loading, PageHeader, useToast } from "@/components/ui";
import { EMPTY_EXPERIENCE, ExperienceForm, cleanExperience } from "@/components/experience-form";

function NewExperience() {
  const router = useRouter();
  const toast = useToast();
  const projectId = useSearchParams().get("projectId");
  const [form, setForm] = useState<ExperienceUpsert>({ ...EMPTY_EXPERIENCE, projectId: projectId ?? null });
  const [saving, setSaving] = useState(false);
  const save = async () => {
    setSaving(true);
    try { const e = await api.experiences.create(cleanExperience(form)); toast.push("경험을 저장했습니다.", "ok"); router.push(`/experiences/${e.id}`); }
    catch (err) { toast.push(errorMessage(err), "error"); setSaving(false); }
  };
  const valid = form.title.trim() && form.situation.trim() && form.task.trim() && form.action.trim() && form.result.trim();
  return (
    <Card>
      <ExperienceForm form={form} onChange={setForm} />
      <div className="mt-4 flex justify-end"><Button variant="primary" onClick={save} loading={saving} disabled={!valid}>저장</Button></div>
    </Card>
  );
}

export default function NewExperiencePage() {
  return (
    <>
      <PageHeader title="경험 추가" description="상황 · 과제 · 행동 · 결과" />
      <Suspense fallback={<Loading />}><NewExperience /></Suspense>
    </>
  );
}
