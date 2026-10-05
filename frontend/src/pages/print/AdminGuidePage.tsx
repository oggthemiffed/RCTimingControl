import { useEffect } from 'react';

export default function AdminGuidePage() {
  useEffect(() => {
    document.title = 'Admin Configuration Guide';
  }, []);

  return (
    <div className="p-8 max-w-3xl mx-auto print:p-4">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold">Admin Configuration Guide</h1>
        <p className="text-sm text-muted-foreground mt-1">For administrators — RC Timing Club</p>
      </div>

      {/* Section 1: Club Configuration */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">1. Club Configuration</h2>
        <p className="text-sm mb-3">
          Club Profile holds the name, contact details, and logo displayed throughout
          the system. This should be completed during initial setup before creating
          events.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Open Club Profile:</span> In the Admin
            sidebar under <span className="font-semibold">Configuration</span>, click
            <span className="font-semibold"> Club Profile</span>. The page is at
            <span className="font-mono text-xs"> /admin/club</span>.
          </li>
          <li>
            <span className="font-semibold">Enter club details:</span> Fill in the club
            name, contact email, and contact phone number. These appear in event
            communications and on printable result sheets.
          </li>
          <li>
            <span className="font-semibold">Save:</span> Click
            <span className="font-semibold"> Save changes</span>. The club name updates
            throughout the Admin panel immediately.
          </li>
        </ol>
      </section>

      {/* Section 2: Tracks */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">2. Tracks</h2>
        <p className="text-sm mb-3">
          Tracks define the physical circuits where events are held. Each track has a
          name and configurable minimum and maximum lap time thresholds used to filter
          spurious decoder passings.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Open Tracks:</span> In the Admin sidebar
            under <span className="font-semibold">Configuration</span>, click
            <span className="font-semibold"> Tracks</span>. The page is at
            <span className="font-mono text-xs"> /admin/tracks</span>.
          </li>
          <li>
            <span className="font-semibold">Create a track:</span> Click
            <span className="font-semibold"> Add track</span>. Enter a track name (e.g.
            "Club Carpet Circuit") and set the minimum lap time in seconds. Passings
            faster than the minimum are discarded as loop noise.
          </li>
          <li>
            <span className="font-semibold">Assign to an event:</span> When creating or
            editing an event, select the track from the track dropdown. Each event runs
            on one track.
          </li>
        </ol>
      </section>

      {/* Section 3: Race Format Templates */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">3. Race Format Templates</h2>
        <p className="text-sm mb-3">
          Race Formats define how a race is run — duration, number of qualifiers, finals
          structure, and bump-up rules. Formats are templates: they are assigned to
          events at event creation time, and a snapshot is taken so that later template
          edits do not affect existing events.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Open Formats:</span> In the Admin sidebar
            under <span className="font-semibold">Configuration</span>, click
            <span className="font-semibold"> Formats</span>. The page is at
            <span className="font-mono text-xs"> /admin/formats</span>.
          </li>
          <li>
            <span className="font-semibold">Create a format:</span> Click
            <span className="font-semibold"> Add format</span>. Enter the format name
            (e.g. "5-Minute Timed Qualifier + ABC Finals") and configure the heat
            duration in seconds, number of qualifier rounds, and finals structure.
          </li>
          <li>
            <span className="font-semibold">Edit a format:</span> Click any existing
            format to open its editor. Changes only affect future event assignments —
            events already using this format are not affected.
          </li>
        </ol>
      </section>

      {/* Section 4: Creating an Event */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">4. Creating an Event</h2>
        <p className="text-sm mb-3">
          Events follow a state machine: DRAFT &rarr; PUBLISHED &rarr; OPEN &rarr;
          ENTRIES_CLOSED &rarr; IN_PROGRESS &rarr; COMPLETED. Each transition is
          triggered by a button in the Event Detail page.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Open Events:</span> In the Admin sidebar
            under <span className="font-semibold">Events &amp; Competitions</span>,
            click <span className="font-semibold">Events</span>.
          </li>
          <li>
            <span className="font-semibold">Create an event:</span> Click
            <span className="font-semibold"> Create event</span>. Enter the event name,
            date, and select a track. The event is created in DRAFT status.
          </li>
          <li>
            <span className="font-semibold">Add classes:</span> Open the event detail
            and switch to the <span className="font-semibold">Classes</span> tab. Add
            each racing class that will compete, assigning a format template to each.
            Generate the race schedule using the Generate Rounds button in each class.
          </li>
          <li>
            <span className="font-semibold">Publish:</span> Click
            <span className="font-semibold"> Publish Event</span>. The event becomes
            visible to all users on the public schedule.
          </li>
          <li>
            <span className="font-semibold">Open entries:</span> Click
            <span className="font-semibold"> Open Entries</span> to show on the public
            schedule that the event is taking entries. Booking happens in RaceHub; import the
            entries from the event&apos;s Entries tab.
          </li>
          <li>
            <span className="font-semibold">Close entries:</span> Click
            <span className="font-semibold"> Close Entries</span> to show that booking has
            closed. It cannot be undone, but walk-ins can still be added on the day.
          </li>
          <li>
            <span className="font-semibold">Start the event:</span> On the day, click
            <span className="font-semibold"> Start Event</span> to move to IN_PROGRESS.
            Open Race Control from the event row shortcut link.
          </li>
          <li>
            <span className="font-semibold">Complete the event:</span> After all races
            are finished, click <span className="font-semibold">Complete Event</span>
            to finalise and publish results.
          </li>
        </ol>
      </section>

      {/* Section 5: Managing Classes and Entries */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">5. Managing Classes and Entries</h2>
        <p className="text-sm mb-3">
          Within an event, the Classes tab manages which racing classes are running and
          their format assignment. The Entries tab shows the event&apos;s entries,
          imported from RaceHub or added as walk-ins.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Add a class to an event:</span> Open the
            event detail, click the <span className="font-semibold">Classes</span> tab,
            then click <span className="font-semibold">Add class</span>. Select a racing
            class and a format template, then save.
          </li>
          <li>
            <span className="font-semibold">Generate rounds:</span> After adding a class,
            click <span className="font-semibold">Generate Rounds</span> to create the
            race schedule (qualifiers and finals) for that class.
          </li>
          <li>
            <span className="font-semibold">View entries:</span> Click the
            <span className="font-semibold"> Entries</span> tab in the event detail to
            see all entries. The list shows each competitor's name, class, and transponders.
          </li>
          <li>
            <span className="font-semibold">Import from RaceHub:</span> Click
            <span className="font-semibold"> Import entries from RaceHub</span> and choose the
            Entry Export file. You see the changes before anything is saved.
          </li>
          <li>
            <span className="font-semibold">Add a walk-in:</span> Click
            <span className="font-semibold"> Add entry</span>, pick an existing driver or type a
            new name, and enter their transponder.
          </li>
          <li>
            <span className="font-semibold">Withdraw an entry:</span> Click
            <span className="font-semibold"> Withdraw</span> beside an entry and give a
            reason. The entry is kept, marked withdrawn, with an audit record.
          </li>
        </ol>
      </section>

      {/* Section 6: Championships */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">6. Championships</h2>
        <p className="text-sm mb-3">
          Championships aggregate results across multiple events using a best-X-from-Y
          scoring model. Points are calculated on demand from result snapshots — they
          are not stored incrementally.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Open Championships:</span> In the Admin
            sidebar under <span className="font-semibold">Events &amp; Competitions</span>,
            click <span className="font-semibold">Championships</span>.
          </li>
          <li>
            <span className="font-semibold">Create a championship:</span> Click
            <span className="font-semibold"> Create championship</span>. Enter a name and
            description.
          </li>
          <li>
            <span className="font-semibold">Configure scoring (Config tab):</span> Open
            the championship detail. The <span className="font-semibold">Config</span> tab
            holds the championship name, description, season, and the default best-X-from-Y
            rounds setting. Set the number of rounds that count toward the title (e.g.
            "Count best 8 from 10 rounds").
          </li>
          <li>
            <span className="font-semibold">Add classes (Classes tab):</span> Click
            <span className="font-semibold">Add Class</span> to assign racing classes.
            Each class can optionally override the global best-X-from-Y setting.
          </li>
          <li>
            <span className="font-semibold">Link events (Events tab):</span> Click
            <span className="font-semibold">Link Event</span> to add an event as a round
            in the championship. Assign a round number to each linked event.
          </li>
          <li>
            <span className="font-semibold">Configure points scale (Points Scale tab):</span>
            The Points Scale tab shows the points awarded for each finishing position
            (1st, 2nd, 3rd, etc.). Edit the points values to match your club's scale.
          </li>
          <li>
            <span className="font-semibold">View standings (Standings tab):</span> The
            Standings tab calculates and displays the current championship table,
            applying the best-X-from-Y rule and any driver exclusions.
          </li>
          <li>
            <span className="font-semibold">Add exclusions (Exclusions tab):</span>
            Use the <span className="font-semibold">Exclusions</span> tab to exclude a
            driver from a specific round (e.g. DQ at scrutineering). Enter the driver,
            the event, and the reason. The exclusion is reflected immediately in the
            Standings tab.
          </li>
        </ol>
      </section>

      {/* Section 7: Officials and Competitors */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">7. Officials and Competitors</h2>
        <p className="text-sm mb-3">
          Only race officials sign in. Staff roles are stackable — a single account can hold
          any combination of ADMIN, RACE_DIRECTOR, and REFEREE. Competitors do not have
          accounts.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Staff accounts:</span> The setup wizard&apos;s
            <span className="font-semibold"> Staff</span> step creates the first official
            accounts and their roles:
            <ul className="list-disc list-inside ml-4 mt-1 space-y-1">
              <li><span className="font-semibold">ADMIN</span> — full club configuration, all events</li>
              <li><span className="font-semibold">RACE_DIRECTOR</span> — access to race control: start/stop races, call grid, marshal laps</li>
              <li><span className="font-semibold">REFEREE</span> — Referee View: raise incidents, apply penalties, link transponders</li>
            </ul>
          </li>
          <li>
            <span className="font-semibold">Managing officials:</span> In the Admin sidebar
            under <span className="font-semibold">Operations</span>, click
            <span className="font-semibold"> Officials</span> to add an official, change their
            roles, set a new password (tell them it in person; there is no emailed reset) or
            disable someone who no longer helps. Disabled officials can&apos;t sign in but are never
            deleted, so their name stays on everything they did. The club always keeps at least one
            admin who can sign in, and every change is listed under
            <span className="font-semibold"> Recent changes</span> with who made it.
          </li>
          <li>
            <span className="font-semibold">Locked out:</span> If no admin can sign in, run
            <code className="mx-1 text-xs">RCTimingControl reset-admin-password &lt;email&gt;</code>
            on the timing laptop as an administrator, with the service stopped. The installing guide
            gives the exact command for each system.
          </li>
          <li>
            <span className="font-semibold">Competitors:</span> In the Admin sidebar
            under <span className="font-semibold">Operations</span>, click
            <span className="font-semibold"> Competitors</span> to see every driver imported
            from RaceHub or added as a walk-in.
          </li>
        </ol>
      </section>

      {/* Section 8: First-Run Setup Wizard */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">8. First-Run Setup Wizard</h2>
        <p className="text-sm mb-3">
          The Setup Wizard provides a guided sequence for configuring the system from
          scratch. It covers the five essentials: Club Profile, Track, Race Format,
          Staff Account, and Decoder Config.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Access the wizard:</span> In the Admin
            sidebar under <span className="font-semibold">Operations</span>, click
            <span className="font-semibold"> Setup Wizard</span>. The wizard is also
            shown automatically on first login before any club configuration exists.
          </li>
          <li>
            <span className="font-semibold">Step 1 — Club Profile:</span> Enter the club
            name and contact details. This is the same form as
            <span className="font-mono text-xs"> /admin/club</span>.
          </li>
          <li>
            <span className="font-semibold">Step 2 — Track:</span> Create at least one
            track with a name and minimum lap time threshold.
          </li>
          <li>
            <span className="font-semibold">Step 3 — Race Format:</span> Create at least
            one race format template. You can add more formats later from the Formats page.
          </li>
          <li>
            <span className="font-semibold">Step 4 — Staff Account:</span> Create the
            first staff (admin) account. This step is shown only before any admin account
            exists.
          </li>
          <li>
            <span className="font-semibold">Step 5 — Decoder Config:</span> Enter the
            IP address and port for the AMB decoder (default port 5100 for RC-4 text
            protocol). RCTC uses this configuration to connect to the decoder.
          </li>
          <li>
            <span className="font-semibold">Navigate the wizard:</span> Click each step
            in the left sidebar to move between steps. Completed steps show a green
            tick. You can return to any step at any time.
          </li>
          <li>
            <span className="font-semibold">Skip the wizard:</span> If you prefer to
            configure sections individually, use the standard Admin sidebar pages at any
            time. The wizard is advisory — all its steps are also accessible via the
            individual Configuration pages.
          </li>
        </ol>
      </section>

      <div className="mt-8 print:hidden">
        <button
          onClick={() => window.print()}
          className="px-4 py-2 bg-primary text-primary-foreground rounded text-sm"
        >
          Print / Save as PDF
        </button>
      </div>
    </div>
  );
}
