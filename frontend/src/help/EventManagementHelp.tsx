export function EventManagementHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Event Detail page manages a single event's lifecycle, classes, and entries. It
        has three tabs — Overview (name, date, track), Classes (racing classes included in
        the event), and Entries (entries imported from RaceHub and walk-ins). Event details
        can only be edited while the event is in Draft status.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Publish Event:</span> Click "Publish Event" to show the event on the public schedule.</li>
        <li><span className="font-semibold">Import entries:</span> On the Entries tab, click "Import entries from RaceHub" and choose the Entry Export file. You see the changes before anything is saved, and importing a newer file later updates the same entries.</li>
        <li><span className="font-semibold">Add a walk-in:</span> On the Entries tab, click "Add entry" for a driver who did not book through RaceHub.</li>
        <li><span className="font-semibold">Open Entries / Close Entries:</span> These mark on the public schedule whether the event is taking entries. Booking itself happens in RaceHub, and walk-ins can still be added on the day.</li>
        <li><span className="font-semibold">Start Event:</span> Click "Start Event" to mark the meeting as In Progress — this enables the Race Control link in the Events list.</li>
        <li><span className="font-semibold">Complete Event:</span> Click "Complete Event" to finalise the event and publish results.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          Event Name, Date, and Track can only be changed while the event is in Draft
          status — fields are read-only once published. Status transitions are one-way
          (except Draft ↔ Published): you cannot revert from Entries Closed back to Open.
          Map every RaceHub class to one of the event's classes before importing: an import
          with an unmapped class saves nothing.
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
