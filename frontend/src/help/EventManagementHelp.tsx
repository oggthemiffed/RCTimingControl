export function EventManagementHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Event Detail page manages a single event's lifecycle, classes, and entries. It
        has three tabs — Overview (name, date, track), Classes (racing classes included in
        the event), and Entries (all submitted entries). Event details can only be edited
        while the event is in Draft status.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Publish Event:</span> Click "Publish Event" to make the event visible to racers on the portal so they can browse it.</li>
        <li><span className="font-semibold">Open Entries:</span> Click "Open Entries" to allow racers to submit their class entries for this event.</li>
        <li><span className="font-semibold">Close Entries:</span> Click "Close Entries" (shown as a destructive button) to stop accepting new entries before the meeting day.</li>
        <li><span className="font-semibold">Start Event:</span> Click "Start Event" to mark the meeting as In Progress — this enables the Race Control link in the Events list.</li>
        <li><span className="font-semibold">Complete Event:</span> Click "Complete Event" to finalise the event and publish results.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          Event Name, Date, and Track can only be changed while the event is in Draft
          status — fields are read-only once published. Status transitions are one-way
          (except Draft ↔ Published): you cannot revert from Entries Closed back to Open.
          Confirm the entry list is correct before clicking "Close Entries".
        </p>
      </div>

      <div className="mt-4 pt-4 border-t">
        <a
          href="/print/admin-guide"
          target="_blank"
          rel="noopener noreferrer"
          className="text-sm text-primary hover:underline"
        >
          Open Admin Configuration Guide (printable)
        </a>
      </div>
    </div>
  );
}
