export function ResultsHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Results page shows your personal race history across all completed events.
        Events are listed as collapsible cards — expand an event to see your finishing
        position, laps completed, and a link to the full race result for each race you
        participated in.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Expand an event:</span> Click any event card to expand it and view the race-by-race breakdown for that meeting.</li>
        <li><span className="font-semibold">Finishing position:</span> Your position is shown as P1, P2, etc. A dash (—) means no position was recorded (e.g. the race was abandoned).</li>
        <li><span className="font-semibold">Laps completed:</span> The number of laps you completed in that race is shown alongside your position.</li>
        <li><span className="font-semibold">View full result:</span> Click the "View" link on any race row to open the full race result page, which includes all finishers and lap time details.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          Results only appear after a race has been fully completed and the event marked as
          finished. If you raced recently and your result is not showing, the officials may
          still be finalising the event — check back after the meeting concludes.
        </p>
      </div>

      <div className="mt-4 pt-4 border-t">
        <a
          href="/print/racer-guide"
          target="_blank"
          rel="noopener noreferrer"
          className="text-sm text-primary hover:underline"
        >
          Open Racer Quick-Start Guide (printable)
        </a>
      </div>
    </div>
  );
}
