/** Shown while a page's code downloads, the first time it opens (pages load on demand, see App.tsx). */
export default function PageLoading() {
  return (
    <div role="status" className="flex items-center justify-center py-16 text-sm text-muted-foreground">
      Loading…
    </div>
  );
}
