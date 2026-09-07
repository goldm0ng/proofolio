"use client";

import Link from "next/link";
import { useCallback, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { useLoadOnMount } from "@/lib/hooks";
import type { ProjectSummary } from "@/lib/types";
import { VISIBILITY_LABEL } from "@/lib/labels";
import { fmtDate } from "@/lib/format";
import { Chip, EmptyState, ErrorBox, LinkButton, Loading, PageHeader } from "@/components/ui";

export default function ProjectsPage() {
  const [list, setList] = useState<ProjectSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const load = useCallback(async () => {
    try { setList(await api.projects.list()); setError(null); } catch (e) { setError(errorMessage(e)); }
  }, []);
  useLoadOnMount(load);

  return (
    <>
      <PageHeader title="프로젝트" description="프로젝트 카드는 경험 뱅크의 원천이자 공개 프로필의 핵심 콘텐츠" actions={<LinkButton href="/projects/new" variant="primary">프로젝트 추가</LinkButton>} />
      {error ? <ErrorBox message={error} onRetry={load} /> : list === null ? <Loading /> : list.length === 0 ? (
        <EmptyState title="프로젝트가 없습니다" description="한 프로젝트를 정리하면 경험, 이력서, 면접 답변으로 재사용됩니다." action={<LinkButton href="/projects/new" variant="primary">첫 프로젝트 추가</LinkButton>} />
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {list.map((p) => (
            <Link key={p.id} href={`/projects/${p.id}`} className="flex flex-col gap-2 rounded border border-line bg-surface p-4 hover:border-line-strong">
              <div className="flex items-start justify-between gap-2">
                <h2 className="font-semibold">{p.name}</h2>
                <Chip tone={p.visibility === "PUBLIC" ? "ok" : "neutral"}>{VISIBILITY_LABEL[p.visibility]}</Chip>
              </div>
              {p.tagline && <p className="text-sm text-ink-2">{p.tagline}</p>}
              <div className="flex flex-wrap gap-1">{p.techStack.slice(0, 6).map((t) => <Chip key={t}>{t}</Chip>)}</div>
              <div className="mt-auto flex items-center justify-between pt-1 text-xs text-muted">
                <span className="tabular">{fmtDate(p.startedAt)} ~ {p.endedAt ? fmtDate(p.endedAt) : "진행 중"}</span>
                <span className="tabular">경험 {p.experienceCount}</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </>
  );
}
