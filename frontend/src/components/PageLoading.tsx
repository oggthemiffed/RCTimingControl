import { Loader2 } from 'lucide-react';

/** Shown while a page's code downloads, the first time it opens (pages load on demand, see App.tsx). */
export default function PageLoading() {
  return (
    <div className="flex items-center justify-center py-16">
      <Loader2 className="h-8 w-8 animate-spin text-primary" role="status" aria-label="Loading" />
    </div>
  );
}
