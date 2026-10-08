import type { CompetitorSummaryDto } from '@/lib/adminApi';

type CompetitorMeta = Pick<CompetitorSummaryDto, 'brcaNumber' | 'homeClub'>;

/** What tells two drivers of the same name apart: "BRCA 12345 · Hilltop RC", or "" when neither is known. */
export function formatCompetitorMeta(competitor: CompetitorMeta): string {
  return [competitor.brcaNumber && `BRCA ${competitor.brcaNumber}`, competitor.homeClub].filter(Boolean).join(' · ');
}

/** Whether a search matches the competitor's name, BRCA number or club, ignoring case. An empty search matches. */
export function matchesCompetitor(competitor: CompetitorSummaryDto, search: string): boolean {
  const query = search.trim().toLowerCase();
  return !query
    || competitor.displayName.toLowerCase().includes(query)
    || (competitor.brcaNumber ?? '').toLowerCase().includes(query)
    || (competitor.homeClub ?? '').toLowerCase().includes(query);
}
