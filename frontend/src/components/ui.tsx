"use client";

import Link from "next/link";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

/* ---------- Button ---------- */
type Variant = "primary" | "secondary" | "ghost" | "danger";
const variantClass: Record<Variant, string> = {
  primary: "bg-accent text-white border-transparent hover:opacity-90 dark:text-[#0e1318]",
  secondary: "bg-surface text-ink border-line-strong hover:bg-surface-2",
  ghost: "bg-transparent text-ink-2 border-transparent hover:bg-surface-2",
  danger: "bg-transparent text-danger border-transparent hover:bg-danger-soft",
};
export function Button({
  variant = "secondary", size = "md", className = "", loading, children, disabled, ...rest
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; size?: "sm" | "md"; loading?: boolean }) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={`inline-flex items-center gap-1.5 rounded border font-medium whitespace-nowrap transition disabled:opacity-50 disabled:cursor-not-allowed ${size === "sm" ? "px-2 py-1 text-xs" : "px-3 py-1.5 text-sm"} ${variantClass[variant]} ${className}`}
    >
      {loading && <Spinner />}
      {children}
    </button>
  );
}
export function LinkButton({ href, variant = "secondary", size = "md", className = "", children }: { href: string; variant?: Variant; size?: "sm" | "md"; className?: string; children: ReactNode }) {
  return (
    <Link href={href} className={`inline-flex items-center gap-1.5 rounded border font-medium whitespace-nowrap transition ${size === "sm" ? "px-2 py-1 text-xs" : "px-3 py-1.5 text-sm"} ${variantClass[variant]} ${className}`}>
      {children}
    </Link>
  );
}

/* ---------- Form controls ---------- */
export function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={`field ${props.className ?? ""}`} />;
}
export function Textarea(props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={`field ${props.className ?? ""}`} />;
}
export function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={`field ${props.className ?? ""}`} />;
}
export function Field({ label, hint, children, className = "" }: { label: string; hint?: string; children: ReactNode; className?: string }) {
  return (
    <label className={`flex flex-col gap-1 ${className}`}>
      <span className="text-xs font-medium text-ink-2">{label}</span>
      {children}
      {hint && <span className="text-xs text-muted">{hint}</span>}
    </label>
  );
}

/* ---------- Chip / Card / Empty / Spinner ---------- */
export function Chip({ tone = "neutral", children, className = "", onClick, active }: { tone?: "neutral" | "accent" | "danger" | "warn" | "ok"; children: ReactNode; className?: string; onClick?: () => void; active?: boolean }) {
  const tones = {
    neutral: "border-line text-ink-2 bg-surface",
    accent: "border-transparent bg-accent-soft text-accent-ink",
    danger: "border-transparent bg-danger-soft text-danger",
    warn: "border-transparent bg-warn-soft text-warn",
    ok: "border-transparent bg-ok-soft text-ok",
  };
  const cls = `inline-flex items-center rounded-sm border px-1.5 py-0.5 text-[11px] leading-4 whitespace-nowrap ${tones[tone]} ${active ? "ring-1 ring-accent" : ""} ${className}`;
  if (onClick) return <button type="button" onClick={onClick} className={`${cls} hover:opacity-80`}>{children}</button>;
  return <span className={cls}>{children}</span>;
}
export function Card({ children, className = "", title, actions }: { children: ReactNode; className?: string; title?: ReactNode; actions?: ReactNode }) {
  return (
    <section className={`rounded border border-line bg-surface ${className}`}>
      {(title || actions) && (
        <header className="flex items-center justify-between gap-2 border-b border-line px-4 py-2.5">
          <h2 className="text-sm font-semibold">{title}</h2>
          <div className="flex items-center gap-2">{actions}</div>
        </header>
      )}
      <div className="p-4">{children}</div>
    </section>
  );
}
export function EmptyState({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded border border-dashed border-line px-6 py-10 text-center">
      <p className="text-sm font-medium text-ink-2">{title}</p>
      {description && <p className="max-w-sm text-xs text-muted">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
export function Spinner({ className = "" }: { className?: string }) {
  return <span className={`inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-t-transparent ${className}`} aria-label="로딩 중" />;
}
export function Loading({ label = "불러오는 중" }: { label?: string }) {
  return <div className="flex items-center gap-2 py-8 text-sm text-muted"><Spinner /> {label}</div>;
}
export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded border border-danger/40 bg-danger-soft px-3 py-2 text-sm text-danger">
      <span>{message}</span>
      {onRetry && <Button size="sm" onClick={onRetry}>다시 시도</Button>}
    </div>
  );
}
export function PageHeader({ title, description, actions }: { title: ReactNode; description?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">{title}</h1>
        {description && <p className="mt-0.5 text-sm text-muted">{description}</p>}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
}

/* ---------- Toast ---------- */
type Toast = { id: number; message: string; tone: "info" | "error" | "ok" };
const ToastCtx = createContext<{ push: (message: string, tone?: Toast["tone"]) => void }>({ push: () => {} });
export function useToast() { return useContext(ToastCtx); }
export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<Toast[]>([]);
  const push = useCallback((message: string, tone: Toast["tone"] = "info") => {
    const id = Date.now() + Math.random();
    setItems((s) => [...s, { id, message, tone }]);
    setTimeout(() => setItems((s) => s.filter((t) => t.id !== id)), tone === "error" ? 6000 : 3000);
  }, []);
  const value = useMemo(() => ({ push }), [push]);
  return (
    <ToastCtx.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed bottom-4 right-4 z-50 flex flex-col gap-2">
        {items.map((t) => (
          <div key={t.id} className={`pointer-events-auto rounded border px-3 py-2 text-sm shadow-sm ${t.tone === "error" ? "border-danger/40 bg-danger-soft text-danger" : t.tone === "ok" ? "border-transparent bg-ok-soft text-ok" : "border-line bg-surface text-ink"}`}>
            {t.message}
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}

/* ---------- Confirm dialog ---------- */
export function ConfirmDialog({ open, title, description, confirmLabel = "삭제", onConfirm, onCancel, loading }: { open: boolean; title: string; description?: string; confirmLabel?: string; onConfirm: () => void; onCancel: () => void; loading?: boolean }) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onCancel(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onCancel]);
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4" role="dialog" aria-modal="true" onClick={onCancel}>
      <div className="w-full max-w-sm rounded border border-line bg-surface p-5 shadow-lg" onClick={(e) => e.stopPropagation()}>
        <h3 className="text-base font-semibold">{title}</h3>
        {description && <p className="mt-1 text-sm text-ink-2">{description}</p>}
        <div className="mt-4 flex justify-end gap-2">
          <Button onClick={onCancel} disabled={loading}>취소</Button>
          <Button variant="primary" className="!bg-danger" onClick={onConfirm} loading={loading}>{confirmLabel}</Button>
        </div>
      </div>
    </div>
  );
}

/* ---------- Tag input ---------- */
export function TagInput({ value, onChange, placeholder = "쉼표 또는 Enter로 추가" }: { value: string[]; onChange: (v: string[]) => void; placeholder?: string }) {
  const [draft, setDraft] = useState("");
  const commit = () => {
    const parts = draft.split(",").map((s) => s.trim()).filter(Boolean);
    if (parts.length) onChange(Array.from(new Set([...value, ...parts])));
    setDraft("");
  };
  return (
    <div className="field flex flex-wrap items-center gap-1 !py-1">
      {value.map((t) => (
        <span key={t} className="inline-flex items-center gap-1 rounded-sm bg-accent-soft px-1.5 py-0.5 text-xs text-accent-ink">
          {t}
          <button type="button" aria-label={`${t} 제거`} className="opacity-60 hover:opacity-100" onClick={() => onChange(value.filter((x) => x !== t))}>×</button>
        </span>
      ))}
      <input
        className="min-w-[120px] flex-1 bg-transparent px-1 py-0.5 text-sm outline-none"
        value={draft}
        placeholder={value.length ? "" : placeholder}
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === ",") { e.preventDefault(); commit(); }
          if (e.key === "Backspace" && !draft && value.length) onChange(value.slice(0, -1));
        }}
        onBlur={commit}
      />
    </div>
  );
}

/* ---------- misc ---------- */
export function Divider() { return <hr className="my-4 border-line" />; }
export function KV({ k, v }: { k: string; v: ReactNode }) {
  return (
    <div className="flex gap-3 text-sm">
      <span className="w-24 shrink-0 text-muted">{k}</span>
      <span className="min-w-0 flex-1 break-words">{v}</span>
    </div>
  );
}
export function Multiline({ text }: { text?: string | null }) {
  if (!text) return <span className="text-muted">-</span>;
  return <div className="whitespace-pre-wrap text-sm leading-relaxed">{text}</div>;
}
