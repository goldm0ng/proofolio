"use client";

import Link from "next/link";
import { useCallback, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { useLoadOnMount } from "@/lib/hooks";
import type { Company } from "@/lib/types";
import { Button, Card, Chip, EmptyState, ErrorBox, Field, Input, Loading, PageHeader, useToast } from "@/components/ui";

export default function CompaniesPage() {
  const toast = useToast();
  const [list, setList] = useState<Company[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [q, setQ] = useState("");
  const [name, setName] = useState("");
  const [creating, setCreating] = useState(false);

  const load = useCallback(async (search?: string) => {
    try { setList(await api.companies.list(search)); setError(null); } catch (e) { setError(errorMessage(e)); }
  }, []);
  useLoadOnMount(load);

  const create = async () => {
    if (!name.trim()) return;
    setCreating(true);
    try {
      await api.companies.create({ name: name.trim(), coreValues: [], techStack: [] });
      setName(""); toast.push("기업을 추가했습니다.", "ok"); load(q);
    } catch (e) { toast.push(errorMessage(e), "error"); } finally { setCreating(false); }
  };

  return (
    <>
      <PageHeader title="기업" description="지원과 분리된 기업 프로필. 같은 회사에 여러 번 지원해도 한 번만 쌓입니다" />
      <div className="mb-4 flex flex-wrap items-end gap-2">
        <Field label="검색" className="w-64"><Input value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") load(q); }} placeholder="회사 이름" /></Field>
        <Button onClick={() => load(q)}>검색</Button>
        <div className="ml-auto flex items-end gap-2">
          <Field label="새 기업" className="w-56"><Input value={name} onChange={(e) => setName(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") create(); }} placeholder="회사 이름" /></Field>
          <Button variant="primary" onClick={create} loading={creating} disabled={!name.trim()}>추가</Button>
        </div>
      </div>
      {error ? <ErrorBox message={error} onRetry={() => load(q)} /> : list === null ? <Loading /> : list.length === 0 ? (
        <EmptyState title="기업이 없습니다" description="공고를 등록하면 기업이 자동으로 생깁니다." />
      ) : (
        <Card>
          <ul className="divide-y divide-line">
            {list.map((c) => (
              <li key={c.id} className="flex items-center gap-3 py-2.5">
                <Link href={`/companies/${c.id}`} className="min-w-0 flex-1 hover:underline">
                  <span className="font-medium">{c.name}</span>{c.industry && <span className="ml-2 text-xs text-muted">{c.industry}</span>}
                </Link>
                <div className="flex flex-wrap gap-1">{c.techStack.slice(0, 5).map((t) => <Chip key={t}>{t}</Chip>)}</div>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </>
  );
}
