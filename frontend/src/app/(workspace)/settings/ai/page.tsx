"use client";

import { useCallback, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { useLoadOnMount } from "@/lib/hooks";
import type { AiCall } from "@/lib/types";
import { fmtDateTime, fmtUsd } from "@/lib/format";
import { Card, Chip, EmptyState, ErrorBox, Loading, PageHeader } from "@/components/ui";

export default function AiSettingsPage() {
  const [calls, setCalls] = useState<AiCall[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const load = useCallback(async () => {
    try { setCalls(await api.ai.calls(100)); setError(null); } catch (e) { setError(errorMessage(e)); }
  }, []);
  useLoadOnMount(load);
  const total = calls?.reduce((n, c) => n + c.costUsd, 0) ?? 0;

  return (
    <>
      <PageHeader title="AI 호출 로그" description={calls ? `최근 ${calls.length}건 · 합계 ${fmtUsd(total)}` : undefined} />
      {error ? <ErrorBox message={error} onRetry={load} /> : calls === null ? <Loading /> : calls.length === 0 ? <EmptyState title="호출 기록이 없습니다" description="공고 추출을 실행하면 여기에 쌓입니다." /> : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="text-left text-xs text-muted">
                <th className="py-1 pr-3 font-medium">시각</th><th className="py-1 pr-3 font-medium">작업</th><th className="py-1 pr-3 font-medium">모델</th>
                <th className="py-1 pr-3 text-right font-medium">입력</th><th className="py-1 pr-3 text-right font-medium">출력</th><th className="py-1 pr-3 text-right font-medium">캐시</th>
                <th className="py-1 pr-3 text-right font-medium">비용</th><th className="py-1 pr-3 text-right font-medium">ms</th><th className="py-1 font-medium">상태</th>
              </tr></thead>
              <tbody>
                {calls.map((c) => (
                  <tr key={c.id} className="border-t border-line align-top">
                    <td className="tabular whitespace-nowrap py-1.5 pr-3 text-muted">{fmtDateTime(c.createdAt)}</td>
                    <td className="py-1.5 pr-3">{c.task}</td>
                    <td className="py-1.5 pr-3 font-mono text-xs">{c.model}</td>
                    <td className="tabular py-1.5 pr-3 text-right">{c.inputTokens}</td>
                    <td className="tabular py-1.5 pr-3 text-right">{c.outputTokens}</td>
                    <td className="tabular py-1.5 pr-3 text-right">{c.cacheReadTokens}</td>
                    <td className="tabular py-1.5 pr-3 text-right">{fmtUsd(c.costUsd)}</td>
                    <td className="tabular py-1.5 pr-3 text-right">{c.durationMs}</td>
                    <td className="py-1.5">{c.status === "OK" ? <Chip tone="ok">OK</Chip> : <Chip tone="danger" className="max-w-64 truncate" >{c.errorMessage || "오류"}</Chip>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </>
  );
}
