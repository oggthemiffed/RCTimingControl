export function ChampionshipHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Championship Detail page configures a season-long series. It has six tabs:
        Config (name and scoring rules), Classes (racing classes in the championship),
        Events (linked round events), Points Scale (per-position points), Standings
        (live standings table), and Exclusions (DSQ and removal records).
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Add Class:</span> On the Classes tab, click "Add Class" to include a racing class and optionally set a per-class "best X from Y rounds" override.</li>
        <li><span className="font-semibold">Link Event:</span> On the Events tab, click "Link Event" to associate an existing event with a round number. Round numbers must be unique.</li>
        <li><span className="font-semibold">Points Scale:</span> On the Points Scale tab, set the points awarded per finishing position (P1, P2, etc.) for this championship.</li>
        <li><span className="font-semibold">Standings:</span> The Standings tab shows the live calculated standings — points are calculated on demand from result snapshots, not stored.</li>
        <li><span className="font-semibold">Add Exclusion:</span> On the Exclusions tab, click "Add Exclusion" to exclude a driver from a specific round with a recorded reason.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          Each event can only be linked to one round number — attempting to link the same
          event to two rounds will show a conflict error. The "best X from Y rounds"
          override on a class takes precedence over the championship-level setting; leave
          it blank on the class to inherit the championship default.
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
