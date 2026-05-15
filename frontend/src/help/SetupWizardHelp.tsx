export function SetupWizardHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Setup Wizard guides you through the five steps required before the system is
        ready to run a race meeting. Each step is listed in the sidebar (Club Profile,
        Track, Race Format, Staff Account, Decoder Config) and is marked complete with a
        green tick when done. Completed steps can be revisited by clicking them in the
        sidebar.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Club Profile:</span> Enter your club's name and contact details. This information appears on printed reports and public pages.</li>
        <li><span className="font-semibold">Track:</span> Add at least one track with its name and optional layout details. Tracks are selectable when creating events.</li>
        <li><span className="font-semibold">Race Format:</span> Create a default race format defining heat duration, number of qualifiers, and final structure.</li>
        <li><span className="font-semibold">Staff Account:</span> Create the first Race Director or Admin staff account so officials can log in.</li>
        <li><span className="font-semibold">Decoder Config:</span> Enter the IP address and port of the AMB/MyLaps decoder. The system will attempt a test connection.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          All five steps must be completed before you can access the Admin Panel and Race
          Control. If you need to come back later, use the "Skip wizard" link at the bottom
          of the sidebar (only visible once setup is complete). The decoder connection test
          requires the forwarder service to be running on the local network.
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
