"use client";

import { useState } from "react";
import { api, errorMessage } from "@/lib/api";
import type { ApplicationStatus, ImportPreview, ImportResult, NotionMapping } from "@/lib/types";
import { APPLICATION_STATUSES, STATUS_LABEL } from "@/lib/labels";
import { Button, Card, ErrorBox, Field, PageHeader, Select, useToast } from "@/components/ui";

const NONE = "__none__";

export default function ImportPage() {
  const toast = useToast();
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ImportPreview | null>(null);
  const [mapping, setMapping] = useState<NotionMapping | null>(null);
  const [statusValues, setStatusValues] = useState<string[]>([]);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const doPreview = async () => {
    if (!file) return;
    setBusy(true); setError(null); setResult(null);
    try {
      const p = await api.importer.preview(file);
      setPreview(p);
      const m: NotionMapping = { ...p.suggestedMapping, companyName: p.suggestedMapping.companyName || p.columns[0] };
      setMapping(m);
      recomputeStatusValues(p, m.status);
    } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  };
  const recomputeStatusValues = (p: ImportPreview, statusCol?: string) => {
    if (!statusCol) { setStatusValues([]); return; }
    const idx = p.columns.indexOf(statusCol);
    const vals = Array.from(new Set(p.sampleRows.map((r) => r[idx]).filter((v) => v && v.trim())));
    setStatusValues(vals);
  };
  const setCol = (k: keyof NotionMapping, v: string) => {
    if (!mapping || !preview) return;
    const next: NotionMapping = { ...mapping, [k]: v === NONE ? undefined : v };
    if (k === "status") { next.statusValues = {}; recomputeStatusValues(preview, next.status); }
    setMapping(next);
  };
  const run = async () => {
    if (!file || !mapping) return;
    setBusy(true); setError(null);
    try { const r = await api.importer.run(file, mapping); setResult(r); toast.push(`생성 ${r.created} · 갱신 ${r.updated} · 건너뜀 ${r.skipped}`, "ok"); }
    catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  };

  const colSelect = (k: keyof NotionMapping, label: string, required = false) => (
    <Field label={label}>
      <Select value={(mapping?.[k] as string | undefined) ?? NONE} onChange={(e) => setCol(k, e.target.value)}>
        {!required && <option value={NONE}>사용 안 함</option>}
        {preview?.columns.map((c) => <option key={c} value={c}>{c}</option>)}
      </Select>
    </Field>
  );

  return (
    <>
      <PageHeader title="노션 임포트" description="노션 데이터베이스를 CSV로 내보낸 파일을 올립니다. (회사, 직무) 조합이 같으면 기존 지원을 갱신합니다" />
      <div className="flex flex-col gap-4">
        <Card title="1. CSV 선택">
          <div className="flex flex-wrap items-center gap-3">
            <input type="file" accept=".csv,text/csv" onChange={(e) => { setFile(e.target.files?.[0] ?? null); setPreview(null); setResult(null); }} className="text-sm" />
            <Button variant="primary" onClick={doPreview} loading={busy && !preview} disabled={!file}>미리보기</Button>
          </div>
          {error && <div className="mt-3"><ErrorBox message={error} /></div>}
        </Card>

        {preview && mapping && (
          <>
            <Card title={`2. 컬럼 매핑 (${preview.rowCount}행)`}>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {colSelect("companyName", "회사 이름 (필수)", true)}
                {colSelect("positionTitle", "직무")}
                {colSelect("deadlineAt", "마감")}
                {colSelect("status", "상태")}
                {colSelect("postingUrl", "공고 URL")}
                <Field label="메모로 합칠 컬럼" hint="Ctrl/Cmd 누르고 여러 개 선택">
                  <select multiple className="field h-28" value={mapping.notes ?? []} onChange={(e) => setMapping({ ...mapping, notes: Array.from(e.target.selectedOptions).map((o) => o.value) })}>
                    {preview.columns.map((c) => <option key={c} value={c}>{c}</option>)}
                  </select>
                </Field>
              </div>
              {mapping.status && (
                <div className="mt-4">
                  <div className="mb-1 text-xs font-medium text-ink-2">상태 값 매핑 <span className="font-normal text-muted">(샘플 행에서 발견된 값)</span></div>
                  {statusValues.length === 0 ? <p className="text-xs text-muted">샘플 행에 상태 값이 없습니다. 매핑되지 않은 값은 &lsquo;관심&rsquo;으로 들어갑니다.</p> : (
                    <table className="text-sm">
                      <tbody>
                        {statusValues.map((v) => (
                          <tr key={v}>
                            <td className="py-1 pr-4">{v}</td>
                            <td className="py-1">
                              <Select className="!w-40" value={mapping.statusValues?.[v] ?? ""} onChange={(e) => setMapping({ ...mapping, statusValues: { ...(mapping.statusValues ?? {}), [v]: e.target.value as ApplicationStatus } })}>
                                <option value="">(관심)</option>
                                {APPLICATION_STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
                              </Select>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              )}
            </Card>

            <Card title="샘플 행">
              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead><tr>{preview.columns.map((c) => <th key={c} className="whitespace-nowrap border-b border-line px-2 py-1 text-left font-medium text-ink-2">{c}</th>)}</tr></thead>
                  <tbody>{preview.sampleRows.map((r, i) => <tr key={i} className="border-b border-line">{r.map((v, j) => <td key={j} className="max-w-64 truncate px-2 py-1" title={v}>{v}</td>)}</tr>)}</tbody>
                </table>
              </div>
            </Card>

            <div className="flex justify-end"><Button variant="primary" onClick={run} loading={busy && !!preview} disabled={!mapping.companyName}>3. 임포트 실행</Button></div>
          </>
        )}

        {result && (
          <Card title="결과">
            <div className="flex gap-6 text-sm"><span>생성 <b className="tabular">{result.created}</b></span><span>갱신 <b className="tabular">{result.updated}</b></span><span>건너뜀 <b className="tabular">{result.skipped}</b></span><span>오류 <b className="tabular">{result.errors.length}</b></span></div>
            {result.errors.length > 0 && (
              <ul className="mt-3 flex flex-col gap-1 text-xs text-danger">{result.errors.map((e, i) => <li key={i} className="tabular">{e.row}행: {e.message}</li>)}</ul>
            )}
          </Card>
        )}
      </div>
    </>
  );
}
