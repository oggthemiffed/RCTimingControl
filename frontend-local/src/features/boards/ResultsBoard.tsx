// Anonymous, read-only spectator board showing the most recently finished race's results. Polls
// `/api/v1/boards/results` on an interval plus an initial fetch on mount.
import { useEffect, useState } from 'react';
import { getResults, type ResultsDto } from '@/lib/api';
import ResultsTable from './ResultsTable';
import { formatRaceLabel } from './format';

const POLL_INTERVAL_MS = 5000;

export default function ResultsBoard() {
  const [data, setData] = useState<ResultsDto | null>(null);

  useEffect(() => {
    let cancelled = false;
    function poll() {
      getResults()
        .then((result) => {
          if (!cancelled) setData(result);
        })
        .catch(() => {
          // Best-effort kiosk polling — leave whatever was last shown on screen and retry next tick.
        });
    }
    poll();
    const interval = setInterval(poll, POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  if (!data || !data.race) {
    return (
      <div className="flex min-h-screen items-center justify-center p-8">
        <p className="text-xl text-slate-500">No results yet.</p>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col gap-6 p-8">
      <h1 className="text-3xl font-semibold">{formatRaceLabel(data.race)}</h1>
      <ResultsTable results={data.results} />
    </div>
  );
}
