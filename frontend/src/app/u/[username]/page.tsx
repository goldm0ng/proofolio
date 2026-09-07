export default async function PublicProfilePage({ params }: PageProps<"/u/[username]">) {
  const { username } = await params;
  return (
    <div className="rounded border border-dashed border-line px-6 py-12 text-center">
      <p className="text-base font-medium">@{username}</p>
      <p className="mt-1 text-sm text-muted">공개 프로필은 Phase 1에서 제공됩니다.</p>
    </div>
  );
}
