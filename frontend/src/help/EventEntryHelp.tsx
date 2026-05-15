export function EventEntryHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Entries page shows all the events you have entered and lets you submit new
        entries when an event's entries are open. You must have at least one car registered
        in your account before you can enter an event.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Browse open events:</span> Events accepting entries are listed here — click an event to view the classes available and submit your entry.</li>
        <li><span className="font-semibold">Select a class:</span> Choose the racing class you want to enter. Each class may have different eligibility requirements set by the club.</li>
        <li><span className="font-semibold">Select a car:</span> Pick the car from your registered cars that you will be racing. The transponder number is snapshotted at submission time.</li>
        <li><span className="font-semibold">View your entries:</span> Submitted entries appear in the list with the event name, class, and submission status.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          You cannot submit an entry if you have not added a car to your account — visit
          the Cars page first. Your transponder number is recorded at the time of entry
          submission, so if you change your transponder after entering, contact a club
          official to update your entry.
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
