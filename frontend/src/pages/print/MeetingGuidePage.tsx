import { useEffect } from 'react';

export default function MeetingGuidePage() {
  useEffect(() => {
    document.title = 'Race Meeting Guide';
  }, []);

  return (
    <div className="p-8 max-w-3xl mx-auto print:p-4">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold">Race Meeting Guide</h1>
        <p className="text-sm text-muted-foreground mt-1">For race officials — RC Timing Club</p>
      </div>

      {/* Section 1: Pre-Meeting Setup */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">1. Pre-Meeting Setup</h2>
        <p className="text-sm mb-3">
          Before the meeting begins, verify that the event is configured, the decoder forwarder is
          running, and race control is accessible.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Open the event in Admin:</span> Navigate to Admin
            panel &rarr; Events. Locate the event for today and confirm it shows status
            "In Progress". If still "Entries Closed", click <span className="font-semibold">Start
            Event</span> to transition it.
          </li>
          <li>
            <span className="font-semibold">Start the forwarder agent:</span> On the timing
            PC, launch the RC Timing Forwarder application. It connects to the AMB decoder
            on port 5100 (RC-4 text protocol) and streams passings to the cloud service.
            Confirm the forwarder status shows "Connected" before proceeding.
          </li>
          <li>
            <span className="font-semibold">Open Race Control:</span> In the Admin panel,
            locate the event row and click the Race Control shortcut link, or navigate
            directly to <span className="font-mono text-xs">/race-control/event/{'{eventId}'}</span>.
            You need the <strong>Race Director</strong> role to access race control.
          </li>
          <li>
            <span className="font-semibold">Verify the run order loads:</span> The left
            sidebar labelled <span className="font-semibold">Run Order</span> should list
            all races for the event. If it is empty, the round generator has not been run
            — go to the event's Classes tab in Admin and generate rounds for each class.
          </li>
        </ol>
      </section>

      {/* Section 2: Managing the Run Order */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">2. Managing the Run Order</h2>
        <p className="text-sm mb-3">
          The Run Order panel on the left side of race control lists every race in the
          meeting in sequence — qualifiers, then finals — grouped by class.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Read the run order:</span> Each row shows the
            race type (Qualifier 1, Final A, etc.), class name, and heat number. The
            current race is highlighted. Races with a green status have finished.
          </li>
          <li>
            <span className="font-semibold">Select a race:</span> Click any row in the Run
            Order sidebar to load that race in the main panel. The main panel updates to
            show the race state and available actions.
          </li>
          <li>
            <span className="font-semibold">Jump to a race out of sequence:</span> If you
            need to skip ahead (e.g. a class is running late), select the target race in
            the Run Order and click <span className="font-semibold">Jump to this race</span>.
            This button appears below the Run Order list only when you select a pending
            race while another race is still active.
          </li>
        </ol>
      </section>

      {/* Section 3: Grid Call */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">3. Grid Call</h2>
        <p className="text-sm mb-3">
          Calling the grid transitions the race from Pending to Grid state and triggers
          the pre-race audio announcements for that heat.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Select the next race:</span> Click the race
            in the Run Order sidebar. The main panel shows a "Race is pending. Call the
            grid when ready." message.
          </li>
          <li>
            <span className="font-semibold">Click "Call Grid":</span> The button appears
            in the main panel. Clicking it transitions the race to GRID state. The system
            announces the heat via audio ("Heat 1, Qualifier 1, to the grid please").
          </li>
          <li>
            <span className="font-semibold">Review the grid editor:</span> After calling
            grid, the Grid Editor panel loads. It shows each entry's starting position.
            Verify that transponders are correctly assigned to drivers before starting.
          </li>
          <li>
            <span className="font-semibold">Pre-race readiness check:</span> Confirm all
            drivers are on the grid and transponders are responding. Unknown transponders
            (not linked to any entry) will appear as alerts during the race — link them
            before starting if possible.
          </li>
        </ol>
      </section>

      {/* Section 4: Starting a Race */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">4. Starting a Race</h2>
        <p className="text-sm mb-3">
          Starting a race transitions from GRID to RUNNING and begins live timing. The
          stagger start sequence announces each driver's name at their start interval.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Confirm grid is ready:</span> All drivers
            should be in position with their cars on the grid. The Grid Editor panel shows
            the starting order.
          </li>
          <li>
            <span className="font-semibold">Click "Start Race":</span> The Start Race
            button appears in the Grid Editor panel. Clicking it initiates the race. The
            audio system plays the stagger start sequence, announcing each driver's name
            at the configured interval.
          </li>
          <li>
            <span className="font-semibold">The Live Timing panel appears:</span> Once
            the race transitions to RUNNING, the main panel switches to the Live Timing
            display showing position, laps completed, last lap time, and best lap time
            for each driver.
          </li>
        </ol>
      </section>

      {/* Section 5: Running a Race */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">5. Running a Race</h2>
        <p className="text-sm mb-3">
          During a race, the Live Timing Panel updates in real time as the decoder
          receives transponder passings. The race director can monitor positions and
          manage marshal lap adjustments.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Monitor the Live Timing panel:</span> The
            panel shows each driver's current position, total laps, last lap time, and
            best lap time. Rows are ordered by race position.
          </li>
          <li>
            <span className="font-semibold">Audio beeps on lap completion:</span> The
            system plays a beep each time a driver completes a lap. An improved (personal
            best) lap triggers a distinct "improving" beep.
          </li>
          <li>
            <span className="font-semibold">Handle unknown transponders:</span> If the
            decoder picks up a transponder not linked to any entry, it appears as an
            "Unknown Transponders" alert in the Live Timing panel. Click
            <span className="font-semibold"> Link to entry</span> beside the transponder
            number to open the Link Transponder dialog and assign it to the correct driver.
          </li>
          <li>
            <span className="font-semibold">Marshal lap adjustments:</span> If a driver's
            car is retrieved from the track by a marshal, the Referee View (accessible via
            the Referee tab in the race control navigation) allows a +1 or &minus;1 lap
            adjustment. All adjustments are recorded with a full audit trail.
          </li>
          <li>
            <span className="font-semibold">Stop the race temporarily:</span> Click
            <span className="font-semibold"> Stop</span> if a red flag is needed. The race
            pauses in STOPPED state. Click <span className="font-semibold">Resume Race</span>
            to continue.
          </li>
        </ol>
      </section>

      {/* Section 6: Stopping and Finishing a Race */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">6. Stopping and Finishing a Race</h2>
        <p className="text-sm mb-3">
          A race finishes automatically when the configured race duration elapses, or
          can be manually finished by the race director.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Automatic finish:</span> When the race timer
            expires, the system transitions the race to FINISHED state and records a final
            result snapshot with positions and lap times.
          </li>
          <li>
            <span className="font-semibold">Review the Finished panel:</span> The main
            panel switches to the Finished panel showing the final standings. Check the
            positions and lap counts for accuracy before moving on.
          </li>
          <li>
            <span className="font-semibold">Restart if needed:</span> If timing data is
            incorrect and the race must be re-run, click
            <span className="font-semibold"> Restart</span>. This clears all timing data
            and returns the race to PENDING state. This action requires confirmation.
          </li>
          <li>
            <span className="font-semibold">Abandon a race:</span> To discard a race
            entirely (it will not count toward standings), click
            <span className="font-semibold"> Abandon</span> during RUNNING or STOPPED
            state. This cannot be undone.
          </li>
        </ol>
      </section>

      {/* Section 7: Handling Incidents */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">7. Handling Incidents</h2>
        <p className="text-sm mb-3">
          The Referee View provides tools for raising incident reports and applying
          time or lap penalties during or after a race.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Open Referee View:</span> The Referee tab
            is accessible from the race control navigation. It requires the
            <strong> Referee</strong> role. The main panel shows the Live Timing display
            with proximity highlights for drivers running close together.
          </li>
          <li>
            <span className="font-semibold">Raise an incident report:</span> Click
            <span className="font-semibold"> Raise Incident</span> in the top-right of
            the Referee View. The incident dialog lets you select the driver involved
            and add a description. Reports are logged with timestamp and race position.
          </li>
          <li>
            <span className="font-semibold">Apply a penalty:</span> Click
            <span className="font-semibold"> Apply Penalty</span> to open the penalty
            dialog. Select the driver and specify whether the penalty is a time addition
            or a lap deduction. Penalties are applied immediately and positions
            recalculate.
          </li>
          <li>
            <span className="font-semibold">Link an unknown transponder:</span> If a
            transponder appears that is not registered to any entry, the Race Director
            can link it during the race. Select the transponder from the Unknown
            Transponders alert in the Cockpit and assign it to the correct entry via
            the Link Transponder dialog.
          </li>
        </ol>
      </section>

      {/* Section 8: Publishing Results */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">8. Publishing Results</h2>
        <p className="text-sm mb-3">
          After all races in the event are finished, results are available to view and
          print from the public Results pages.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Complete the event:</span> In Admin &rarr;
            Events, open the event detail and click
            <span className="font-semibold"> Complete Event</span> to transition from
            "In Progress" to "Completed". This finalises the result snapshot.
          </li>
          <li>
            <span className="font-semibold">View results:</span> Completed event results
            are accessible from the public Results page at
            <span className="font-mono text-xs"> /results</span>. Each event shows
            collapsible class results with positions, laps, and best lap times.
          </li>
          <li>
            <span className="font-semibold">Print results:</span> From the race control
            Finished panel or the public Results page, use the Print Results button
            (or Ctrl+P) to open the browser print dialog. The print layout removes
            navigation and headers for a clean output.
          </li>
        </ol>
      </section>

      {/* Section 9: Moving to the Next Race */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">9. Moving to the Next Race</h2>
        <p className="text-sm mb-3">
          After one race finishes, select the next race in the Run Order and repeat
          the grid call cycle. The system auto-selects the next non-finished race.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Select the next race:</span> The Run Order
            sidebar auto-highlights the next race in sequence. Click it to load it in
            the main panel.
          </li>
          <li>
            <span className="font-semibold">Call the grid:</span> Click
            <span className="font-semibold"> Call Grid</span> for the next race. The
            audio system announces the new heat. Repeat sections 3–6 above.
          </li>
          <li>
            <span className="font-semibold">Bump-up for finals:</span> When a B or
            C-Final finishes, the system automatically applies bump-up promotions and
            shows a toast notification listing how many drivers have been promoted to
            the next final. Review the grid before starting the promoted final.
          </li>
          <li>
            <span className="font-semibold">End of meeting:</span> Once all races in
            the Run Order are marked Finished, complete the event in Admin. Championship
            standings update automatically from the final result snapshots.
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
