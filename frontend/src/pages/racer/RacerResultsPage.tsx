import { useRacerResults } from '@/hooks/racer/useRacerResults';
import { RacerEventHistoryCard } from '@/components/racer/RacerEventHistoryCard';

export default function RacerResultsPage() {
  const { data, isPending, error } = useRacerResults();

  if (isPending) {
    return (
      <div aria-live="polite" className="max-w-5xl mx-auto">
        {[1, 2, 3].map(i => (
          <div key={i} className="animate-pulse bg-muted rounded h-24 mb-3" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div role="alert" className="text-destructive">
        Could not load results. Try refreshing the page.
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">Results</h1>
      </div>
      {(!data || data.length === 0) ? (
        <div className="py-12 text-center text-muted-foreground">
          <p className="text-base font-semibold">No results yet</p>
          <p className="text-sm mt-1">Results appear here after your first race finishes.</p>
        </div>
      ) : (
        data.map(event => (
          <RacerEventHistoryCard key={event.eventId} event={event} />
        ))
      )}
    </div>
  );
}
