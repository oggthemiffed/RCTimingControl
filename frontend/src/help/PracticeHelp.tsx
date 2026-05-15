export function PracticeHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Practice Sessions page lists all free-practice sessions. Practice timing runs
        independently from the event run order — it does not affect championship points.
        Sessions display their current status (Idle, Running, or Stopped) and the number
        of laps used for personal best calculation.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">New Session:</span> Click the "New Session" button to open the creation dialog. Give the session a name and optionally link it to an event.</li>
        <li><span className="font-semibold">Open a session:</span> Click any session card in the list to open its live timing view where you can start, stop, and view lap times.</li>
        <li><span className="font-semibold">Session status:</span> A green play icon indicates a running session; a check icon means stopped; a dumbbell icon means idle (not yet started).</li>
        <li><span className="font-semibold">Best laps:</span> The "Best N laps" count shown on each card is the number of consecutive laps used for that session's personal best calculation.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          Practice sessions are not linked to race entries — any transponder detected by
          the decoder will be recorded. Make sure drivers are only on track during a
          session intended for them, or lap times from other drivers will appear in the
          results.
        </p>
      </div>

      <div className="mt-4 pt-4 border-t">
        <a
          href="/print/meeting-guide"
          target="_blank"
          rel="noopener noreferrer"
          className="text-sm text-primary hover:underline"
        >
          Open Race Meeting Guide (printable)
        </a>
      </div>
    </div>
  );
}
