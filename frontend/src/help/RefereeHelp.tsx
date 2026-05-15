export function RefereeHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Referee View provides a live timing display focused on race oversight. It shows
        the same live timing table as the Cockpit but adds controls for raising incident
        reports and applying time or lap penalties. Proximity alerts highlight drivers
        whose gaps are closing rapidly.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Select a race:</span> Click any race in the Run Order sidebar — the live timing table updates to show that race.</li>
        <li><span className="font-semibold">Raise Incident:</span> Click "Raise Incident" to open the incident report dialog. Select the driver involved, describe the incident, and submit. The report is logged with a timestamp.</li>
        <li><span className="font-semibold">Apply Penalty:</span> Click "Apply Penalty" to open the penalty dialog. Choose the affected driver and the penalty type (lap deduction or time addition).</li>
        <li><span className="font-semibold">Proximity alerts:</span> Rows highlighted in the live timing table indicate drivers closing on each other — useful for spotting incidents in real time.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          The "Raise Incident" and "Apply Penalty" buttons are disabled when no race is
          selected. Select the race from the Run Order sidebar first. Penalties are applied
          immediately and affect the live position calculation — double-check the driver
          and penalty type before submitting.
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
