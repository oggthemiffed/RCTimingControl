import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getResults, getSession } from '@/lib/practiceApi';
import type { PracticeTimingRowDto } from '@/lib/practiceApi';

function fmtMs(ms: number | null): string {
  if (ms === null || ms <= 0) return '—';
  const totalMs = Math.floor(ms);
  const m = Math.floor(totalMs / 60000);
  const s = Math.floor((totalMs % 60000) / 1000);
  const millis = totalMs % 1000;
  if (m > 0) {
    return `${m}:${String(s).padStart(2, '0')}.${String(millis).padStart(3, '0')}`;
  }
  return `${s}.${String(millis).padStart(3, '0')}`;
}

export default function PrintPracticeResultsPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const id = Number(sessionId);

  const validId = Number.isFinite(id) && id > 0;

  const { data: session, isPending: sessionPending, isError: sessionError } = useQuery({
    queryKey: ['practice-session', id],
    queryFn: () => getSession(id).then((r) => r.data),
    enabled: validId,
  });

  const { data: results, isPending: resultsPending, isError: resultsError } = useQuery({
    queryKey: ['practice-results', id],
    queryFn: () => getResults(id).then((r) => r.data),
    enabled: validId,
  });

  useEffect(() => {
    if (session) {
      document.title = `Practice Results — ${session.name}`;
    }
  }, [session]);

  if (!validId) {
    return <div className="p-8 text-sm text-destructive">Invalid session ID.</div>;
  }

  if (sessionPending || resultsPending) {
    return <div className="p-8 text-sm text-muted-foreground">Loading results…</div>;
  }

  if (sessionError || resultsError || !session || !results) {
    return <div className="p-8 text-sm text-destructive">Could not load results.</div>;
  }

  const bestN = session.bestLapN;

  return (
    <div className="p-8 max-w-3xl mx-auto print:p-4">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold">{session.name}</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Practice session &bull; Best {bestN} consecutive laps
          {session.stoppedAt
            ? ` &bull; Ended ${new Date(session.stoppedAt).toLocaleString()}`
            : ''}
        </p>
      </div>

      {/* Results table */}
      <table className="w-full text-sm border-collapse">
        <thead>
          <tr className="border-b-2 border-foreground">
            <th className="text-left py-2 pr-4 w-10">Pos</th>
            <th className="text-left py-2 pr-4">Racer</th>
            <th className="text-right py-2 pr-4">Laps</th>
            <th className="text-right py-2 pr-4">Best Lap</th>
            <th className="text-right py-2 pr-4">Best {bestN} Consec.</th>
            <th className="text-right py-2">Last Lap</th>
          </tr>
        </thead>
        <tbody>
          {results.map((row: PracticeTimingRowDto) => (
            <tr key={row.transponderNumber} className="border-b border-border/50">
              <td className="py-1.5 pr-4 font-semibold">
                {row.laps > 0 ? row.position : '—'}
              </td>
              <td className="py-1.5 pr-4">{row.racerName}</td>
              <td className="py-1.5 pr-4 text-right font-mono">{row.laps}</td>
              <td className="py-1.5 pr-4 text-right font-mono">{fmtMs(row.bestLapMs)}</td>
              <td className="py-1.5 pr-4 text-right font-mono">
                {row.bestConsecutiveNMs !== null
                  ? fmtMs(Math.round(row.bestConsecutiveNMs / bestN)) + ' avg'
                  : '—'}
              </td>
              <td className="py-1.5 text-right font-mono">{fmtMs(row.lastLapMs)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Print action */}
      <div className="mt-6 print:hidden">
        <button
          onClick={() => window.print()}
          className="px-4 py-2 bg-primary text-primary-foreground rounded text-sm"
        >
          Print
        </button>
      </div>
    </div>
  );
}
