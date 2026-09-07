export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto w-full max-w-4xl px-6 py-3 text-sm font-semibold tracking-tight">proofolio</div>
      </header>
      <main className="mx-auto w-full max-w-4xl px-6 py-8">{children}</main>
    </div>
  );
}
