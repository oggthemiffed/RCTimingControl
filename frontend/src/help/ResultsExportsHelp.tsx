export function ResultsExportsHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Results to RaceHub page shows the results RCTC has sent back to RaceHub. It only
        applies to events whose entries were imported from RaceHub. Results are queued when a
        race finishes, when a finished race is corrected (for example a penalty is given after
        the finish), and when the race day is closed, then sent in the background. Racing is
        never held up by sending.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Waiting to send / Failed, will retry:</span> RaceHub could not be reached, or refused the send. RCTC keeps trying, and the reason is shown under the row.</li>
        <li><span className="font-semibold">Sent:</span> RaceHub has the results.</li>
        <li><span className="font-semibold">Replaced by a newer export:</span> Each send carries the whole event, so a newer one makes an older, unsent one unnecessary.</li>
        <li><span className="font-semibold">Send now:</span> Click it on a waiting or failed row to try again straight away.</li>
        <li><span className="font-semibold">Download results:</span> Each event&apos;s page has a &quot;Download results&quot; button for taking the results across by hand.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          If the page says no RaceHub address or key is set, results wait here and nothing is
          sent. The address and the club&apos;s key go in the settings file in the data folder, and
          the service must be restarted afterwards. The installation guide, under &quot;Sending
          results to RaceHub&quot;, has the steps. An event with no entries imported from RaceHub
          is never sent.
        </p>
      </div>
    </div>
  );
}
