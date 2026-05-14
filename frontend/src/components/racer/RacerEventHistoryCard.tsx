import { useState } from 'react';
import { Link } from 'react-router-dom';
import { RiArrowDownSLine, RiExternalLinkLine } from '@remixicon/react';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';
import type { RacerResultHistoryDto } from '@/lib/racerApi';

interface Props {
  event: RacerResultHistoryDto;
}

export function RacerEventHistoryCard({ event }: Props) {
  const [open, setOpen] = useState(false);
  const displayDate = new Date(event.eventDate).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });

  return (
    <Collapsible open={open} onOpenChange={setOpen} className="border border-border rounded-lg mb-3">
      <CollapsibleTrigger className="w-full flex items-center justify-between p-4 cursor-pointer hover:bg-muted/30 select-none min-h-[44px]">
        <div>
          <span className="font-semibold text-base">{event.eventName}</span>
          <span className="ml-3 text-sm text-muted-foreground">{displayDate}</span>
        </div>
        <RiArrowDownSLine className={`h-5 w-5 transition-transform ${open ? 'rotate-180' : ''}`} aria-hidden="true" />
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="px-4 pb-4 pt-0">
          {event.races.length === 0 ? (
            <p className="text-sm text-muted-foreground py-2">No race results for this event.</p>
          ) : (
            <table className="w-full text-sm">
              <tbody>
                {event.races.map(race => (
                  <tr key={race.raceId} className="border-t border-border/50">
                    <td className="py-2 pr-4">{race.raceLabel}</td>
                    <td className="py-2 pr-4 font-mono">
                      {race.position > 0 ? `P${race.position}` : '—'}
                    </td>
                    <td className="py-2 pr-4 font-mono">
                      {race.lapsCompleted} lap{race.lapsCompleted !== 1 ? 's' : ''}
                    </td>
                    <td className="py-2 text-right">
                      <Link
                        to={`/results/${race.raceId}`}
                        className="text-primary hover:underline inline-flex items-center gap-1 text-xs"
                      >
                        <RiExternalLinkLine className="h-3 w-3" aria-hidden="true" />
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </CollapsibleContent>
    </Collapsible>
  );
}
