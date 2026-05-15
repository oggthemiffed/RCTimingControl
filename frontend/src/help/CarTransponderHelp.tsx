export function CarTransponderHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Cars page lets you manage the cars registered to your account. Each car card
        shows the car's name and class details. Cars are referenced when submitting event
        entries, so make sure each car is set up with the correct information before
        entering a meeting.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Add car:</span> Click "Add car" (or "Add your first car" when the list is empty) to open the car editor sheet and fill in the car's details.</li>
        <li><span className="font-semibold">Edit a car:</span> Click any car card to open the editor sheet for that car — update its name, class, or transponder details and save.</li>
        <li><span className="font-semibold">Transponders:</span> Transponder numbers are registered separately via the Transponders page in your portal navigation. Link a transponder to your car there before entering an event.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          You must add a car before you can submit an event entry — the entry form will ask
          you to select a car from your list. If no cars appear, click "Add car" to create
          one. A car can be used for multiple entries across different events.
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
