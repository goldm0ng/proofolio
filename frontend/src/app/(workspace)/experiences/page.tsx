"use client";

import Link from "next/link";
import { useCallback, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { useLoadOnMount } from "@/lib/hooks";
import type { ExperienceSummary, TagCount } from "@/lib/types";
import { VISIBILITY_LABEL } from "@/lib/labels";
import { fmtDate } from "@/lib/format";
import { Button, Card, Chip, EmptyState, ErrorBox, Input, LinkButton, Loading, PageHeader } from "@/components/ui";

export default function ExperiencesPage() {
  const [list, setList] = useState<ExperienceSummary[] | null>(null);
  const [tags, setTags] = useState<TagCount[]>([]);
  const [tag, setTag] = useState<string | null>(null);
  const [q, setQ] = useState("");
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [l, t] = await Promise.all([api.experiences.list({ tag: tag ?? undefined, q: q || undefined }), api.experiences.tags()]);
      setList(l); setTags(t);
    } catch (e) { setError(errorMessage(e)); }
  }, [tag, q]);
  useLoadOnMount(load);

  return (
    <>
      <PageHeader title="경험 뱅크" description="STAR 단위로 쪼갠 경험. 이력서·자소서·면접 답변에서 재사용됩니다" actions={<LinkButton href="/experiences/new" variant="primary">경험 추가</LinkButton>} />
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div className="flex w-64 gap-1.5"><Input value={q} placeholder="검색" onChange={(e) => setQ(e.target.value)} /><Button onClick={load}>검색</Button></div>
        <div className="flex flex-wrap gap-1">
          <Chip tone={tag === null ? "accent" : "neutral"} onClick={() => setTag(null)}>전체</Chip>
          {tags.map((t) => <Chip key={t.tag} tone={tag === t.tag ? "accent" : "neutral"} onClick={() => setTag(tag === t.tag ? null : t.tag)}>{t.tag} <span className="tabular ml-1 opacity-60">{t.count}</span></Chip>)}
        </div>
      </div>
      {error ? <ErrorBox message={error} onRetry={load} /> : list === null ? <Loading /> : list.length === 0 ? (
        <EmptyState title="경험이 없습니다" description="프로젝트에서 한 일을 STAR로 정리해 두면 자소서와 면접에서 근거로 씁니다." action={<LinkButton href="/experiences/new" variant="primary">첫 경험 추가</LinkButton>} />
      ) : (
        <Card>
          <ul className="divide-y divide-line">
            {list.map((e) => (
              <li key={e.id} className="flex items-center gap-3 py-2.5">
                <div className="min-w-0 flex-1">
                  <Link href={`/experiences/${e.id}`} className="text-sm font-medium hover:underline">{e.title}</Link>
                  <div className="text-xs text-muted">{e.projectName ? <Link href={`/projects/${e.projectId}`} className="hover:underline">{e.projectName}</Link> : "프로젝트 없음"} · {fmtDate(e.updatedAt)}</div>
                </div>
                <div className="flex flex-wrap gap-1">{e.tags.map((t) => <Chip key={t} tone="accent">{t}</Chip>)}</div>
                <Chip tone={e.visibility === "PUBLIC" ? "ok" : "neutral"}>{VISIBILITY_LABEL[e.visibility]}</Chip>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </>
  );
}
