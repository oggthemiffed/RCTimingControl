import { useState } from 'react';
import { Loader2, Users } from 'lucide-react';

import { Input } from '@/components/ui/input';
import { useAdminCompetitorsList } from '@/hooks/admin/useAdminCompetitors';

/**
 * Every competitor the club has timed: imported from RaceHub or added as a walk-in (L10, #18).
 * Replaces the old racer list; competitors have no login.
 */
export default function CompetitorsPage() {
  const [search, setSearch] = useState('');
  const { data: competitors, isLoading, isError } = useAdminCompetitorsList();

  const query = search.trim().toLowerCase();
  const shown = (competitors ?? []).filter(c =>
    !query
    || c.displayName.toLowerCase().includes(query)
    || (c.brcaNumber ?? '').toLowerCase().includes(query)
    || (c.homeClub ?? '').toLowerCase().includes(query));

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Competitors</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Drivers imported from RaceHub or added as walk-ins.
        </p>
      </div>

      {isLoading && (
        <div className="flex items-center gap-2 py-8">
          <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
          <span className="text-sm text-muted-foreground">Loading competitors…</span>
        </div>
      )}

      {isError && <p className="text-sm text-destructive">Could not load competitors. Try again.</p>}

      {!isLoading && !isError && competitors?.length === 0 && (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <Users className="h-10 w-10 text-muted-foreground mb-4" aria-hidden="true" />
          <h2 className="text-lg font-semibold">No competitors yet</h2>
          <p className="text-sm text-muted-foreground mt-1">
            Competitors appear here when you import entries from RaceHub or add a walk-in.
          </p>
        </div>
      )}

      {!isLoading && competitors && competitors.length > 0 && (
        <>
          <Input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search by name, BRCA number or club"
            aria-label="Search competitors"
          />
          {shown.length === 0 ? (
            <p className="text-sm text-muted-foreground">No competitors match “{search.trim()}”.</p>
          ) : (
            <ul className="divide-y rounded-lg border" aria-label="Competitors">
              {shown.map(c => (
                <li key={c.id} className="flex items-center justify-between gap-4 px-4 py-3">
                  <span className="font-medium">{c.displayName}</span>
                  <span className="text-xs text-muted-foreground text-right">
                    {[c.brcaNumber && `BRCA ${c.brcaNumber}`, c.homeClub].filter(Boolean).join(' · ')}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}
