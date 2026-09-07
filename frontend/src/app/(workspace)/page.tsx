"use client";

import Link from "next/link";
import { useCallback, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { useLoadOnMount } from "@/lib/hooks";
import type { AiCall, ApplicationCard, Board } from "@/lib/types";
import { APPLICATION_STATUSES, STATUS_LABEL } from "@/lib/labels";
import { dday, fmtDate, fmtDateTime, fmtUsd } from "@/lib/format";
import { Card, Chip, EmptyState, ErrorBox, LinkButton, Loading, PageHeader } from "@/components/ui";

export default function DashboardPage() {
  const [upcoming, setUpcoming] = useState<ApplicationCard[] | null>(null);
  const [board, setBoard] = useState<Board | null>(null);
  const [calls, setCalls] = useState<AiCall[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [u, b, c] = await Promise.all([api.applications.upcoming(14), api.applications.board(), api.ai.calls(5)]);
      setUpcoming(u); setBoard(b); setCalls(c);
    } catch (e) { setError(errorMessage(e)); }
  }, []);
  useLoadOnMount(load);

  const counts = new Map<string, number>();
  board?.columns.forEach((c) => counts.set(c.status, c.items.length));
  const total = board ? board.columns.reduce((n, c) => n + c.items.length, 0) : 0;

  return (
    <>
      <PageHeader title="대시보드" description="다가오는 마감과 진행 현황" actions={<LinkButton href="/applications/new" variant="primary">공고 등록</LinkButton>} />
      {error && <div className="mb-4"><ErrorBox message={error} onRetry={load} /></div>}
      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="다가오는 마감 (14일)" className="lg:col-span-2">
          {upcoming === null && !error ? <Loading /> : !upcoming || upcoming.length === 0 ? (
            <EmptyState title="14일 내 마감이 없습니다" action={<LinkButton href="/applications" size="sm">지원 보드 보기</LinkButton>} />
          ) : (
            <ul className="divide-y divide-line">
              {upcoming.map((a) => {
                const d = dday(a.deadlineAt);
                return (
                  <li key={a.id} className="flex items-center gap-3 py-2">
                    {d && <Chip tone={d.tone === "overdue" ? "danger" : d.tone === "urgent" ? "warn" : "neutral"} className="tabular w-14 justify-center">{d.text}</Chip>}
                    <Link href={`/applications/${a.id}`} className="min-w-0 flex-1 hover:underline">
                      <span className="font-medium">{a.companyName}</span> <span className="text-ink-2">{a.positionTitle}</span>
                    </Link>
                    <span className="text-xs text-muted">{STATUS_LABEL[a.status]}</span>
                    <span className="tabular text-xs text-muted">{fmtDate(a.deadlineAt)}</span>
                  </li>
                );
              })}
            </ul>
          )}
        </Card>
        <Card title={`상태별 현황 (${total})`}>
          {board === null && !error ? <Loading /> : (
            <ul className="flex flex-col gap-1">
              {APPLICATION_STATUSES.map((s) => (
                <li key={s} className="flex items-center justify-between text-sm">
                  <Link href={`/applications?status=${s}`} className="text-ink-2 hover:underline">{STATUS_LABEL[s]}</Link>
                  <span className="tabular font-medium">{counts.get(s) ?? 0}</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card title="최근 AI 호출" className="lg:col-span-3" actions={<LinkButton href="/settings/ai" size="sm">전체 보기</LinkButton>}>
          {calls === null && !error ? <Loading /> : !calls || calls.length === 0 ? (
            <p className="text-sm text-muted">아직 AI 호출이 없습니다.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="text-left text-xs text-muted"><th className="py-1 pr-3 font-medium">작업</th><th className="py-1 pr-3 font-medium">모델</th><th className="py-1 pr-3 font-medium">토큰(입/출)</th><th className="py-1 pr-3 font-medium">비용</th><th className="py-1 pr-3 font-medium">상태</th><th className="py-1 font-medium">시각</th></tr></thead>
                <tbody>
                  {calls.map((c) => (
                    <tr key={c.id} className="border-t border-line">
                      <td className="py-1.5 pr-3">{c.task}</td>
                      <td className="py-1.5 pr-3 font-mono text-xs">{c.model}</td>
                      <td className="tabular py-1.5 pr-3">{c.inputTokens} / {c.outputTokens}</td>
                      <td className="tabular py-1.5 pr-3">{fmtUsd(c.costUsd)}</td>
                      <td className="py-1.5 pr-3">{c.status === "OK" ? <Chip tone="ok">OK</Chip> : <Chip tone="danger">오류</Chip>}</td>
                      <td className="tabular py-1.5 text-muted">{fmtDateTime(c.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>
    </>
  );
}
