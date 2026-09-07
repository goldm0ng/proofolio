"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { useLoadOnMount } from "@/lib/hooks";
import type { ApplicationDetail, ApplicationStatus, CompanyUpsert, EssayQuestion, EssayStatus, Requirement, RequirementKind } from "@/lib/types";
import { APPLICATION_STATUSES, ESSAY_STATUSES, ESSAY_STATUS_LABEL, REQUIREMENT_KINDS, REQUIREMENT_KIND_LABEL, STATUS_LABEL } from "@/lib/labels";
import { dday, fmtDateTime, fromLocalInput, toLocalInput } from "@/lib/format";
import { Button, Card, Chip, ConfirmDialog, ErrorBox, Field, Input, KV, Loading, Multiline, Select, TagInput, Textarea, useToast } from "@/components/ui";

/* ---------- requirement row ---------- */
function RequirementRow({ appId, r, onChange, onRemove }: { appId: string; r: Requirement; onChange: (r: Requirement) => void; onRemove: () => void }) {
  const toast = useToast();
  const [note, setNote] = useState(r.note ?? "");
  const [editing, setEditing] = useState(false);
  const patch = async (b: Partial<Pick<Requirement, "title" | "kind" | "done" | "note">>) => {
    try { onChange(await api.applications.updateRequirement(appId, r.id, b)); } catch (e) { toast.push(errorMessage(e), "error"); }
  };
  return (
    <li className="flex flex-col gap-1 py-2">
      <div className="flex items-center gap-2">
        <input type="checkbox" checked={r.done} onChange={(e) => patch({ done: e.target.checked })} className="h-4 w-4 accent-[var(--accent)]" />
        <span className={`flex-1 text-sm ${r.done ? "text-muted line-through" : ""}`}>{r.title}</span>
        <Chip>{REQUIREMENT_KIND_LABEL[r.kind]}</Chip>
        <Button variant="ghost" size="sm" onClick={() => setEditing((v) => !v)}>메모</Button>
        <Button variant="danger" size="sm" onClick={onRemove}>삭제</Button>
      </div>
      {(editing || r.note) && (
        <div className="ml-6 flex gap-2">
          <Input value={note} placeholder="메모" onChange={(e) => setNote(e.target.value)} onBlur={() => { if (note !== (r.note ?? "")) patch({ note }); }} />
        </div>
      )}
    </li>
  );
}

/* ---------- essay row ---------- */
function EssayRow({ appId, q, onChange, onRemove }: { appId: string; q: EssayQuestion; onChange: (q: EssayQuestion) => void; onRemove: () => void }) {
  const toast = useToast();
  const [question, setQuestion] = useState(q.question);
  const [maxLength, setMaxLength] = useState<string>(q.maxLength ? String(q.maxLength) : "");
  const [draft, setDraft] = useState(q.draft ?? "");
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const dirty = draft !== (q.draft ?? "") || question !== q.question || maxLength !== (q.maxLength ? String(q.maxLength) : "");

  const save = async (status?: EssayStatus) => {
    setSaving(true);
    try {
      onChange(await api.applications.updateEssay(appId, q.id, { question, maxLength: maxLength ? Number(maxLength) : null, draft, status }));
      toast.push("저장했습니다.", "ok");
    } catch (e) { toast.push(errorMessage(e), "error"); } finally { setSaving(false); }
  };
  const len = draft.length;
  const over = q.maxLength ? len > q.maxLength : false;
  const statusTone = q.status === "DONE" ? "ok" : q.status === "DRAFT" ? "warn" : "neutral";

  return (
    <li className="border-b border-line py-3 last:border-0">
      <div className="flex items-start gap-2">
        <button type="button" className="flex-1 text-left" onClick={() => setOpen((v) => !v)}>
          <div className="text-sm font-medium">{q.question || <span className="text-muted">문항 없음</span>}</div>
          <div className="mt-0.5 flex items-center gap-2 text-xs text-muted">
            <span className={`tabular ${over ? "text-danger" : ""}`}>{len}{q.maxLength ? ` / ${q.maxLength}` : ""}자</span>
            <span>·</span><span>{fmtDateTime(q.updatedAt)}</span>
          </div>
        </button>
        <Chip tone={statusTone}>{ESSAY_STATUS_LABEL[q.status]}</Chip>
        <Button variant="danger" size="sm" onClick={onRemove}>삭제</Button>
      </div>
      {open && (
        <div className="mt-3 flex flex-col gap-2">
          <div className="flex gap-2">
            <Textarea rows={2} value={question} onChange={(e) => setQuestion(e.target.value)} placeholder="문항" />
            <Input type="number" className="!w-28" placeholder="글자수" value={maxLength} onChange={(e) => setMaxLength(e.target.value)} />
          </div>
          <Textarea rows={10} value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="답변 초안" />
          <div className="flex items-center justify-between">
            <span className={`tabular text-xs ${over ? "font-medium text-danger" : "text-muted"}`}>{len}{maxLength ? ` / ${maxLength}` : ""}자{over ? " (초과)" : ""}</span>
            <div className="flex items-center gap-2">
              <Select className="!w-28" value={q.status} onChange={(e) => save(e.target.value as EssayStatus)}>
                {ESSAY_STATUSES.map((s) => <option key={s} value={s}>{ESSAY_STATUS_LABEL[s]}</option>)}
              </Select>
              <Button variant="primary" size="sm" onClick={() => save()} loading={saving} disabled={!dirty}>저장</Button>
            </div>
          </div>
        </div>
      )}
    </li>
  );
}

/* ---------- company panel ---------- */
function CompanyPanel({ app, onUpdated }: { app: ApplicationDetail; onUpdated: () => void }) {
  const toast = useToast();
  const c = app.company;
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<CompanyUpsert>({ name: c.name, industry: c.industry ?? "", website: c.website ?? "", talentProfile: c.talentProfile ?? "", coreValues: c.coreValues, techStack: c.techStack, hiringProcess: c.hiringProcess ?? "", notes: c.notes ?? "" });
  const [saving, setSaving] = useState(false);
  const save = async () => {
    setSaving(true);
    try { await api.companies.update(c.id, form); toast.push("기업 프로필을 저장했습니다.", "ok"); setEditing(false); onUpdated(); }
    catch (e) { toast.push(errorMessage(e), "error"); } finally { setSaving(false); }
  };
  return (
    <Card title={<Link href={`/companies/${c.id}`} className="hover:underline">{c.name}</Link>} actions={editing ? <><Button size="sm" onClick={() => setEditing(false)}>취소</Button><Button size="sm" variant="primary" onClick={save} loading={saving}>저장</Button></> : <Button size="sm" onClick={() => setEditing(true)}>편집</Button>}>
      {editing ? (
        <div className="flex flex-col gap-3">
          <Field label="산업"><Input value={form.industry ?? ""} onChange={(e) => setForm({ ...form, industry: e.target.value })} /></Field>
          <Field label="웹사이트"><Input value={form.website ?? ""} onChange={(e) => setForm({ ...form, website: e.target.value })} /></Field>
          <Field label="인재상"><Textarea value={form.talentProfile ?? ""} onChange={(e) => setForm({ ...form, talentProfile: e.target.value })} /></Field>
          <Field label="핵심 가치"><TagInput value={form.coreValues} onChange={(v) => setForm({ ...form, coreValues: v })} /></Field>
          <Field label="기술 스택"><TagInput value={form.techStack} onChange={(v) => setForm({ ...form, techStack: v })} /></Field>
          <Field label="채용 프로세스"><Textarea value={form.hiringProcess ?? ""} onChange={(e) => setForm({ ...form, hiringProcess: e.target.value })} /></Field>
          <Field label="메모"><Textarea value={form.notes ?? ""} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></Field>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          <KV k="산업" v={c.industry || "-"} />
          <KV k="웹사이트" v={c.website ? <a className="text-accent-ink hover:underline" href={c.website} target="_blank" rel="noreferrer">{c.website}</a> : "-"} />
          <KV k="인재상" v={<Multiline text={c.talentProfile} />} />
          <KV k="핵심 가치" v={c.coreValues.length ? <div className="flex flex-wrap gap-1">{c.coreValues.map((v) => <Chip key={v} tone="accent">{v}</Chip>)}</div> : "-"} />
          <KV k="기술 스택" v={c.techStack.length ? <div className="flex flex-wrap gap-1">{c.techStack.map((v) => <Chip key={v}>{v}</Chip>)}</div> : "-"} />
          <KV k="채용 프로세스" v={<Multiline text={c.hiringProcess} />} />
          <KV k="메모" v={<Multiline text={c.notes} />} />
        </div>
      )}
    </Card>
  );
}

/* ---------- page ---------- */
export default function ApplicationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const toast = useToast();
  const [app, setApp] = useState<ApplicationDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirm, setConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [pos, setPos] = useState(""); const [deadline, setDeadline] = useState(""); const [url, setUrl] = useState("");
  const [notes, setNotes] = useState(""); const [result, setResult] = useState(""); const [retro, setRetro] = useState("");

  const syncFields = (a: ApplicationDetail) => {
    setPos(a.positionTitle); setDeadline(toLocalInput(a.deadlineAt)); setUrl(a.postingUrl ?? "");
    setNotes(a.notes ?? ""); setResult(a.result ?? ""); setRetro(a.retrospective ?? "");
  };
  const load = useCallback(async () => {
    try { const a = await api.applications.get(id); setApp(a); syncFields(a); setError(null); } catch (e) { setError(errorMessage(e)); }
  }, [id]);
  useLoadOnMount(load);


  const update = async (b: Parameters<typeof api.applications.update>[1]) => {
    try { const a = await api.applications.update(id, b); setApp(a); syncFields(a); toast.push("저장했습니다.", "ok"); } catch (e) { toast.push(errorMessage(e), "error"); }
  };
  const setStatus = async (status: ApplicationStatus) => {
    try { setApp(await api.applications.setStatus(id, status)); toast.push(`상태: ${STATUS_LABEL[status]}`, "ok"); } catch (e) { toast.push(errorMessage(e), "error"); }
  };
  const remove = async () => {
    setDeleting(true);
    try { await api.applications.remove(id); toast.push("삭제했습니다.", "ok"); router.push("/applications"); }
    catch (e) { toast.push(errorMessage(e), "error"); setDeleting(false); }
  };

  // add requirement / essay
  const [newReq, setNewReq] = useState(""); const [newReqKind, setNewReqKind] = useState<RequirementKind>("OTHER");
  const [newQ, setNewQ] = useState(""); const [newQLen, setNewQLen] = useState("");
  const addReq = async () => {
    if (!newReq.trim() || !app) return;
    try { const r = await api.applications.addRequirement(id, { title: newReq.trim(), kind: newReqKind }); setApp({ ...app, requirements: [...app.requirements, r] }); setNewReq(""); }
    catch (e) { toast.push(errorMessage(e), "error"); }
  };
  const addEssay = async () => {
    if (!newQ.trim() || !app) return;
    try { const q = await api.applications.addEssay(id, { question: newQ.trim(), maxLength: newQLen ? Number(newQLen) : undefined }); setApp({ ...app, essayQuestions: [...app.essayQuestions, q] }); setNewQ(""); setNewQLen(""); }
    catch (e) { toast.push(errorMessage(e), "error"); }
  };

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!app) return <Loading />;
  const d = dday(app.deadlineAt);

  return (
    <>
      <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="text-xs text-muted"><Link href="/applications" className="hover:underline">지원</Link> / <Link href={`/companies/${app.companyId}`} className="hover:underline">{app.companyName}</Link></div>
          <h1 className="mt-0.5 text-xl font-semibold tracking-tight">{app.companyName} · {app.positionTitle}</h1>
          <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted">
            {d && <Chip tone={d.tone === "overdue" ? "danger" : d.tone === "urgent" ? "warn" : "neutral"} className="tabular">{d.text}</Chip>}
            {app.postingUrl && <a href={app.postingUrl} target="_blank" rel="noreferrer" className="text-accent-ink hover:underline">공고 열기</a>}
            {app.employmentType && <span>{app.employmentType}</span>}
            {app.location && <span>{app.location}</span>}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Select className="!w-36" value={app.status} onChange={(e) => setStatus(e.target.value as ApplicationStatus)}>
            {APPLICATION_STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
          </Select>
          <Button variant="danger" onClick={() => setConfirm(true)}>삭제</Button>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="flex flex-col gap-4 lg:col-span-2">
          <Card title="기본 정보">
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label="직무"><Input value={pos} onChange={(e) => setPos(e.target.value)} onBlur={() => { if (pos !== app.positionTitle && pos.trim()) update({ positionTitle: pos.trim() }); }} /></Field>
              <Field label="마감"><Input type="datetime-local" value={deadline} onChange={(e) => setDeadline(e.target.value)} onBlur={() => { const iso = fromLocalInput(deadline); if (iso !== (app.deadlineAt ?? null)) update({ deadlineAt: iso }); }} /></Field>
              <Field label="공고 URL" className="sm:col-span-2"><Input value={url} onChange={(e) => setUrl(e.target.value)} onBlur={() => { if (url !== (app.postingUrl ?? "")) update({ postingUrl: url || null }); }} /></Field>
            </div>
          </Card>

          <Card title={`준비물 체크리스트 (${app.requirements.filter((r) => r.done).length}/${app.requirements.length})`}>
            {app.requirements.length === 0 ? <p className="text-sm text-muted">준비물이 없습니다. 아래에서 추가하세요.</p> : (
              <ul className="divide-y divide-line">
                {app.requirements.map((r) => (
                  <RequirementRow key={r.id} appId={id} r={r}
                    onChange={(nr) => setApp({ ...app, requirements: app.requirements.map((x) => (x.id === nr.id ? nr : x)) })}
                    onRemove={async () => { try { await api.applications.removeRequirement(id, r.id); setApp({ ...app, requirements: app.requirements.filter((x) => x.id !== r.id) }); } catch (e) { toast.push(errorMessage(e), "error"); } }} />
                ))}
              </ul>
            )}
            <div className="mt-3 flex gap-2 border-t border-line pt-3">
              <Input value={newReq} placeholder="준비물 추가" onChange={(e) => setNewReq(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") addReq(); }} />
              <Select className="!w-36" value={newReqKind} onChange={(e) => setNewReqKind(e.target.value as RequirementKind)}>
                {REQUIREMENT_KINDS.map((k) => <option key={k} value={k}>{REQUIREMENT_KIND_LABEL[k]}</option>)}
              </Select>
              <Button onClick={addReq} disabled={!newReq.trim()}>추가</Button>
            </div>
          </Card>

          <Card title={`자소서 문항 (${app.essayQuestions.filter((q) => q.status === "DONE").length}/${app.essayQuestions.length})`}>
            {app.essayQuestions.length === 0 ? <p className="text-sm text-muted">문항이 없습니다.</p> : (
              <ul>
                {app.essayQuestions.map((q) => (
                  <EssayRow key={q.id} appId={id} q={q}
                    onChange={(nq) => setApp({ ...app, essayQuestions: app.essayQuestions.map((x) => (x.id === nq.id ? nq : x)) })}
                    onRemove={async () => { try { await api.applications.removeEssay(id, q.id); setApp({ ...app, essayQuestions: app.essayQuestions.filter((x) => x.id !== q.id) }); } catch (e) { toast.push(errorMessage(e), "error"); } }} />
                ))}
              </ul>
            )}
            <div className="mt-3 flex gap-2 border-t border-line pt-3">
              <Input value={newQ} placeholder="문항 추가" onChange={(e) => setNewQ(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") addEssay(); }} />
              <Input type="number" className="!w-28" placeholder="글자수" value={newQLen} onChange={(e) => setNewQLen(e.target.value)} />
              <Button onClick={addEssay} disabled={!newQ.trim()}>추가</Button>
            </div>
          </Card>

          <Card title="메모 · 결과 · 회고">
            <div className="flex flex-col gap-3">
              <Field label="메모"><Textarea value={notes} onChange={(e) => setNotes(e.target.value)} onBlur={() => { if (notes !== (app.notes ?? "")) update({ notes }); }} /></Field>
              <Field label="결과"><Input value={result} onChange={(e) => setResult(e.target.value)} onBlur={() => { if (result !== (app.result ?? "")) update({ result }); }} /></Field>
              <Field label="회고"><Textarea value={retro} onChange={(e) => setRetro(e.target.value)} onBlur={() => { if (retro !== (app.retrospective ?? "")) update({ retrospective: retro }); }} /></Field>
            </div>
          </Card>
        </div>

        <div className="flex flex-col gap-4">
          <CompanyPanel app={app} onUpdated={load} />
          <Card title="상태 이력">
            {app.statusHistory.length === 0 ? <p className="text-sm text-muted">이력이 없습니다.</p> : (
              <ol className="flex flex-col gap-2">
                {[...app.statusHistory].reverse().map((h, i) => (
                  <li key={i} className="flex gap-3 text-sm">
                    <span className="tabular w-28 shrink-0 text-xs text-muted">{fmtDateTime(h.changedAt)}</span>
                    <div><div className="font-medium">{STATUS_LABEL[h.status]}</div>{h.note && <div className="text-xs text-ink-2">{h.note}</div>}</div>
                  </li>
                ))}
              </ol>
            )}
          </Card>
        </div>
      </div>

      <ConfirmDialog open={confirm} title="지원 카드를 삭제할까요?" description={`${app.companyName} · ${app.positionTitle}의 체크리스트와 자소서 문항이 함께 삭제됩니다.`} onConfirm={remove} onCancel={() => setConfirm(false)} loading={deleting} />
    </>
  );
}
