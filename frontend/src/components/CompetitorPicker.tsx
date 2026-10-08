import { Button } from '@/components/ui/button';
import type { CompetitorSummaryDto } from '@/lib/adminApi';
import { formatCompetitorMeta } from '@/lib/competitors';

/** Competitors to pick from, each with its BRCA number and club so namesakes can be told apart. */
export function CompetitorChoices({ choices, onPick, label, emptyText }: {
  choices: CompetitorSummaryDto[];
  onPick: (competitor: CompetitorSummaryDto) => void;
  /** The list's accessible name. */
  label: string;
  /** Shown when nothing matches; the list is left out instead when there is none. */
  emptyText?: string;
}) {
  if (choices.length === 0 && !emptyText) return null;
  return (
    <ul className="max-h-48 overflow-y-auto rounded-md border divide-y" aria-label={label}>
      {choices.map(c => {
        const meta = formatCompetitorMeta(c);
        return (
          <li key={c.id}>
            <button
              type="button"
              className="w-full px-3 py-1.5 text-left text-sm hover:bg-muted"
              onClick={() => onPick(c)}
            >
              {c.displayName}
              {meta && <span className="text-muted-foreground"> · {meta}</span>}
            </button>
          </li>
        );
      })}
      {choices.length === 0 && <li className="px-3 py-2 text-sm text-muted-foreground">{emptyText}</li>}
    </ul>
  );
}

/** The competitor picked, with a button to pick someone else. */
export function ChosenCompetitor({ competitor, prefix, onChange }: {
  competitor: CompetitorSummaryDto;
  /** A word before the name, such as "Keep". */
  prefix?: string;
  onChange: () => void;
}) {
  return (
    <div className="flex items-center gap-2 rounded-md border px-3 py-2 text-sm">
      {prefix && <span className="text-muted-foreground">{prefix}</span>}
      <span className="flex-1 font-medium">{competitor.displayName}</span>
      {competitor.homeClub && <span className="text-muted-foreground">{competitor.homeClub}</span>}
      <Button type="button" size="sm" variant="ghost" onClick={onChange}>
        Change
      </Button>
    </div>
  );
}
