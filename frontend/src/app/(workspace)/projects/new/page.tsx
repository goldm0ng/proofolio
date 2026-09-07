"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { api, errorMessage } from "@/lib/api";
import type { ProjectUpsert } from "@/lib/types";
import { Button, Card, PageHeader, useToast } from "@/components/ui";
import { ProjectFormFields, cleanProject } from "@/components/project-form";

export default function NewProjectPage() {
  const router = useRouter();
  const toast = useToast();
  const [form, setForm] = useState<ProjectUpsert>({ name: "", visibility: "PRIVATE" });
  const [saving, setSaving] = useState(false);
  const save = async () => {
    setSaving(true);
    try { const p = await api.projects.create(cleanProject(form)); toast.push("프로젝트를 만들었습니다.", "ok"); router.push(`/projects/${p.id}`); }
    catch (e) { toast.push(errorMessage(e), "error"); setSaving(false); }
  };
  return (
    <>
      <PageHeader title="프로젝트 추가" />
      <Card>
        <ProjectFormFields form={form} onChange={setForm} />
        <div className="mt-4 flex justify-end"><Button variant="primary" onClick={save} loading={saving} disabled={!form.name.trim()}>만들기</Button></div>
      </Card>
    </>
  );
}
