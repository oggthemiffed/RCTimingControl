import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getEventSchedule } from '@/lib/raceControlApi';
import type { EventScheduleDto } from '@/lib/raceControlApi';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';

function entryAvailabilityBadge(availability: EventScheduleDto['entryAvailability']) {
  switch (availability) {
    case 'ENTRY_OPEN':
      return <Badge variant="default">Open for Entry</Badge>;
    case 'ENTRY_CLOSED':
      return <Badge variant="secondary">Entries Closed</Badge>;
    case 'ENTRY_NOT_YET_OPEN':
    default:
      return <Badge variant="outline">Coming Soon</Badge>;
  }
}

export default function EventSchedulePage() {
  const { data: events, isLoading, isError } = useQuery({
    queryKey: ['public', 'events'],
    queryFn: getEventSchedule,
    staleTime: 60_000,
  });

  if (isLoading) {
    return (
      <div className="p-8 max-w-3xl mx-auto" aria-live="polite">
        {[1, 2, 3].map(i => (
          <div key={i} className="animate-pulse bg-muted rounded h-32 mb-3" />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="p-8 max-w-3xl mx-auto">
        <div role="alert" className="text-destructive">
          Could not load events. Try refreshing the page.
        </div>
      </div>
    );
  }

  if (!events || events.length === 0) {
    return (
      <div className="p-8 max-w-3xl mx-auto">
        <div className="py-12 text-center text-muted-foreground">No events scheduled.</div>
      </div>
    );
  }

  return (
    <div className="p-8 max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">Event Schedule</h1>
      <div>
        {events.map(event => (
          <div key={event.id} className="border rounded-lg p-4 mb-3">
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1 min-w-0">
                <h2 className="font-semibold text-base">{event.name}</h2>
                <p className="text-sm text-muted-foreground mt-0.5">
                  {new Intl.DateTimeFormat('en-GB', { dateStyle: 'long' }).format(
                    new Date(event.eventDate),
                  )}
                </p>
              </div>
              <div className="flex-shrink-0">
                {entryAvailabilityBadge(event.entryAvailability)}
              </div>
            </div>

            {(event.finishedRaceIds.length > 0 || event.championshipId !== null) && (
              <div className="flex flex-wrap items-center gap-2 mt-3 pt-3 border-t border-border/50">
                {event.finishedRaceIds.map(raceId => (
                  <Button key={raceId} variant="outline" size="sm" asChild>
                    <Link to={`/results/${raceId}`}>
                      View Results
                    </Link>
                  </Button>
                ))}
                {event.championshipId !== null && (
                  <Button variant="outline" size="sm" asChild>
                    <Link to={`/championships/${event.championshipId}`}>
                      View Standings
                    </Link>
                  </Button>
                )}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
