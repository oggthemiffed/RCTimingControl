export function RaceControlHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Cockpit is the central race control interface for running a meeting. It shows
        the full run order for the event in the left sidebar, and the main panel updates
        based on the selected race's state — from calling the grid through to live timing
        and final results.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Select a race:</span> Click any race in the Run Order sidebar to make it the active race.</li>
        <li><span className="font-semibold">Call Grid:</span> When the race is in Pending state, click "Call Grid" to move it to Grid state and open the grid editor.</li>
        <li><span className="font-semibold">Start / Stop:</span> Use the "Start" button from the Grid editor to begin timing, and the "Stop" button during a running race to pause it.</li>
        <li><span className="font-semibold">Link unknown transponder:</span> If a transponder is detected but not linked to an entry, a badge appears with a "Link to entry" button — resolve it during the race.</li>
        <li><span className="font-semibold">Jump to race:</span> Select a pending race while another is active and click "Jump to this race" to skip ahead in the run order.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          The Start button only appears once the race is in Grid state — you must click
          "Call Grid" first. If a race shows Stopped, use "Resume Race" to continue or
          "Abandon" to discard it. Bump-up promotions are shown as a toast notification
          after a Final finishes — always check the grid before starting the next final.
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
