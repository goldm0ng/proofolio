"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const NAV = [
  { href: "/", label: "대시보드", exact: true },
  { href: "/applications", label: "지원" },
  { href: "/companies", label: "기업" },
  { href: "/projects", label: "프로젝트" },
  { href: "/experiences", label: "경험" },
  { href: "/import", label: "임포트" },
  { href: "/settings/ai", label: "설정" },
];

export function Sidebar() {
  const path = usePathname();
  return (
    <aside className="flex w-52 shrink-0 flex-col border-r border-line bg-surface">
      <div className="border-b border-line px-4 py-4">
        <Link href="/" className="text-base font-semibold tracking-tight">proofolio</Link>
        <p className="text-[11px] text-muted">취업 준비 워크스페이스</p>
      </div>
      <nav className="flex flex-col gap-0.5 p-2">
        {NAV.map((n) => {
          const active = n.exact ? path === n.href : path === n.href || path.startsWith(n.href + "/");
          return (
            <Link
              key={n.href}
              href={n.href}
              className={`rounded px-3 py-1.5 text-sm ${active ? "bg-accent-soft font-medium text-accent-ink" : "text-ink-2 hover:bg-surface-2"}`}
            >
              {n.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
