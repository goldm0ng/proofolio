"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import {Suspense, useCallback, useState, type DragEvent} from "react";
import { api, errorMessage } from "@/lib/api";
import { useLoadOnMount } from "@/lib/hooks";
import type { ApplicationCard, ApplicationStatus, Board } from "@/lib/types";
import { CLOSED_STATUSES, OPEN_STATUSES, STATUS_LABEL } from "@/lib/labels";
import { dday, fmtDate } from "@/lib/format";
import { Chip, ErrorBox, LinkButton, Loading, PageHeader, useToast } from "@/components/ui";

function CardItem({ a, onDragStart, dragging }: { a: ApplicationCard; onDragStart: (e: DragEvent, a: ApplicationCard) => void; dragging: boolean }) {
  const d = dday(a.deadlineAt);
  return (
    <Link
      href={`/applications/${a.id}`}
      draggable
      onDragStart={(e) => onDragStart(e, a)}
      className={`block rounded border border-line bg-surface p-2.5 text-sm shadow-sm hover:border-line-strong ${dragging ? "opacity-40" : ""}`}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="truncate font-medium">{a.companyName}</div>
          <div className="truncate text-xs text-ink-2">{a.positionTitle}</div>
        </div>
        {d && <Chip tone={d.tone === "overdue" ? "danger" : d.tone === "urgent" ? "warn" : "neutral"} className="tabular">{d.text}</Chip>}
      </div>
      <div className="mt-2 flex items-center gap-2 text-[11px] text-muted">
        <span className="tabular">준비물 {a.requirementsDone}/{a.requirementsTotal}</span>
        <span className="tabular">자소서 {a.essayDone}/{a.essayTotal}</span>
        {a.deadlineAt && <span className="tabular ml-auto">{fmtDate(a.deadlineAt)}</span>}
      </div>
    </Link>
  );
}

function BoardView() {
  const params = useSearchParams();
  const highlight = params.get("status") as ApplicationStatus | null;
  const toast = useToast();
  const [board, setBoard] = useState<Board | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [over, setOver] = useState<ApplicationStatus | null>(null);
  const [closedOpen, setClosedOpen] = useState(() => !!highlight && CLOSED_STATUSES.includes(highlight));

  const load = useCallback(async () => {
    try { setBoard(await api.applications.board()); setError(null); } catch (e) { setError(errorMessage(e)); }
  }, []);
  useLoadOnMount(load);

  const itemsOf = (s: ApplicationStatus) => board?.columns.find((c) => c.status === s)?.items ?? [];
  const closedItems = CLOSED_STATUSES.flatMap(itemsOf);

  const onDragStart = (e: DragEvent, a: ApplicationCard) => {
    e.dataTransfer.setData("text/plain", a.id);
    e.dataTransfer.effectAllowed = "move";
    setDraggingId(a.id);
  };
  const drop = async (status: ApplicationStatus, e: DragEvent) => {
    e.preventDefault();
    setOver(null);
    const id = e.dataTransfer.getData("text/plain") || draggingId;
    setDraggingId(null);
    if (!id || !board) return;
    const from = board.columns.find((c) => c.items.some((i) => i.id === id));
    if (!from || from.status === status) return;
    const item = from.items.find((i) => i.id === id)!;
    // optimistic
    setBoard({
      columns: board.columns.map((c) =>
        c.status === from.status ? { ...c, items: c.items.filter((i) => i.id !== id) }
        : c.status === status ? { ...c, items: [{ ...item, status }, ...c.items] } : c),
    });
    try {
      await api.applications.setStatus(id, status);
      toast.push(`${item.companyName} → ${STATUS_LABEL[status]}`, "ok");
    } catch (err) {
      toast.push(errorMessage(err), "error");
      load();
    }
  };
  const colProps = (s: ApplicationStatus) => ({
    onDragOver: (e: DragEvent) => { e.preventDefault(); e.dataTransfer.dropEffect = "move"; if (over !== s) setOver(s); },
    onDragLeave: () => { if (over === s) setOver(null); },
    onDrop: (e: DragEvent) => drop(s, e),
  });

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!board) return <Loading />;

  return (
    <div className="overflow-x-auto pb-2">
      <div className="flex min-w-max gap-3">
        {OPEN_STATUSES.map((s) => {
          const items = itemsOf(s);
          return (
            <div key={s} {...colProps(s)} className={`flex w-60 shrink-0 flex-col rounded border bg-surface-2 ${over === s ? "border-accent" : highlight === s ? "border-line-strong" : "border-line"}`}>
              <div className="flex items-center justify-between px-3 py-2 text-xs font-medium text-ink-2">
                <span>{STATUS_LABEL[s]}</span><span className="tabular text-muted">{items.length}</span>
              </div>
              <div className="flex min-h-24 flex-1 flex-col gap-2 px-2 pb-2">
                {items.map((a) => <CardItem key={a.id} a={a} onDragStart={onDragStart} dragging={draggingId === a.id} />)}
              </div>
            </div>
          );
        })}
        <div className="flex w-60 shrink-0 flex-col rounded border border-line bg-surface-2">
          <button type="button" className="flex items-center justify-between px-3 py-2 text-left text-xs font-medium text-ink-2" onClick={() => setClosedOpen((v) => !v)}>
            <span>종료 {closedOpen ? "▾" : "▸"}</span><span className="tabular text-muted">{closedItems.length}</span>
          </button>
          {closedOpen && CLOSED_STATUSES.map((s) => {
            const items = itemsOf(s);
            return (
              <div key={s} {...colProps(s)} className={`mx-2 mb-2 rounded border border-dashed p-1.5 ${over === s ? "border-accent" : "border-line"}`}>
                <div className="mb-1 px-1 text-[11px] text-muted">{STATUS_LABEL[s]} · {items.length}</div>
                <div className="flex flex-col gap-2">
                  {items.map((a) => <CardItem key={a.id} a={a} onDragStart={onDragStart} dragging={draggingId === a.id} />)}
                  {items.length === 0 && <div className="px-1 py-1 text-[11px] text-muted">여기로 끌어다 놓기</div>}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export default function ApplicationsPage() {
  return (
    <>
      <PageHeader title="지원 보드" description="카드를 끌어 상태를 바꿀 수 있습니다" actions={<LinkButton href="/applications/new" variant="primary">공고 등록</LinkButton>} />
      <Suspense fallback={<Loading />}><BoardView /></Suspense>
    </>
  );
}
