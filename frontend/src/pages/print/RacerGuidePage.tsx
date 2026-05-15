import { useEffect } from 'react';

export default function RacerGuidePage() {
  useEffect(() => {
    document.title = 'Racer Quick-Start Guide';
  }, []);

  return (
    <div className="p-8 max-w-3xl mx-auto print:p-4">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold">Racer Quick-Start Guide</h1>
        <p className="text-sm text-muted-foreground mt-1">For racers — RC Timing Club</p>
      </div>

      {/* Section 1: Getting Started */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">1. Getting Started</h2>
        <p className="text-sm mb-3">
          To enter events at RC Timing Club you need a registered account. Registration
          takes about two minutes and is free. Once registered, you can manage your cars,
          transponders, and event entries from any device.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Go to the registration page:</span> Open
            <span className="font-mono text-xs"> /register</span> in your browser. Enter
            your email address, choose a password, and click
            <span className="font-semibold"> Create account</span>.
          </li>
          <li>
            <span className="font-semibold">Log in:</span> After registering, go to
            <span className="font-mono text-xs"> /login</span> and sign in with your email
            and password. You will be taken to your racer dashboard.
          </li>
          <li>
            <span className="font-semibold">Complete your profile:</span> Before entering
            any event, fill in your name and contact details in the Profile section
            (see section 2 below). An incomplete profile may prevent entry submission.
          </li>
        </ol>
      </section>

      {/* Section 2: Your Profile */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">2. Your Profile</h2>
        <p className="text-sm mb-3">
          Your profile holds your personal details, emergency contact, governing body
          membership numbers, and announcement voice preference.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Open Profile:</span> Click
            <span className="font-semibold"> Profile</span> in the racer navigation. The
            Profile card shows your first name, last name, phone number, and emergency
            contact fields.
          </li>
          <li>
            <span className="font-semibold">Fill in your name:</span> Enter your first
            and last name. These appear in race control and on results sheets.
          </li>
          <li>
            <span className="font-semibold">Add a phonetic name (optional):</span> If
            your name is often mispronounced, enter a phonetic spelling in the
            <span className="font-semibold"> Phonetic name</span> field. The audio
            announcement system uses this when calling you to the grid.
          </li>
          <li>
            <span className="font-semibold">Emergency contact:</span> Enter an emergency
            contact name and phone number. This information is used by club officials
            in the event of an incident at the venue.
          </li>
          <li>
            <span className="font-semibold">Governing body memberships:</span> Scroll to
            the <span className="font-semibold">Governing body memberships</span> card.
            Select your governing body (e.g. BRCA) and enter your membership number, then
            click <span className="font-semibold">Add</span>. You can hold memberships
            with multiple bodies.
          </li>
          <li>
            <span className="font-semibold">Save changes:</span> Click
            <span className="font-semibold"> Save changes</span> at the bottom of the
            Profile card. The button is only active when you have unsaved edits.
          </li>
          <li>
            <span className="font-semibold">Announcement voice (optional):</span> Scroll
            to the <span className="font-semibold">Announcement Voice</span> card. Select
            a voice from the dropdown, then click
            <span className="font-semibold"> Preview</span> to hear how your name will
            sound during race announcements. Click
            <span className="font-semibold"> Save Voice Preferences</span> to confirm
            your choice.
          </li>
        </ol>
      </section>

      {/* Section 3: Adding Cars */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">3. Adding Cars</h2>
        <p className="text-sm mb-3">
          Each car you race must be registered in the system. When you submit an event
          entry, you select one of your registered cars. You can maintain multiple cars
          across different classes.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Open Cars:</span> Click
            <span className="font-semibold"> Cars</span> in the racer navigation. The
            page lists all your registered cars as cards.
          </li>
          <li>
            <span className="font-semibold">Add a car:</span> Click
            <span className="font-semibold"> Add car</span> (or
            <span className="font-semibold"> Add your first car</span> if you have none).
            A side panel opens where you enter the car details.
          </li>
          <li>
            <span className="font-semibold">Enter car details:</span> Fill in the car
            name or description (e.g. "Xray T4 — BRCA 10th Stock") so you can identify
            it easily when entering events.
          </li>
          <li>
            <span className="font-semibold">Save the car:</span> Click
            <span className="font-semibold"> Save</span> in the side panel. The new car
            appears as a card on the Cars page.
          </li>
          <li>
            <span className="font-semibold">Edit a car:</span> Click any car card to
            re-open the side panel and update its details.
          </li>
        </ol>
      </section>

      {/* Section 4: Registering Transponders */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">4. Registering Transponders</h2>
        <p className="text-sm mb-3">
          A transponder is the small electronic device fitted to your car that the
          AMB decoder reads as you cross the timing line. Transponder numbers are
          unique across the entire system — each number can only be registered to one
          account.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Open Transponders:</span> Click
            <span className="font-semibold"> Transponders</span> in the racer navigation.
          </li>
          <li>
            <span className="font-semibold">Add a transponder:</span> Click
            <span className="font-semibold"> Add transponder</span>. Enter the transponder
            number printed on your device (e.g. 12345). Click
            <span className="font-semibold"> Save</span>.
          </li>
          <li>
            <span className="font-semibold">Transponder uniqueness:</span> If the number
            is already registered to another account the system will show an error. Contact
            a club official to resolve transponder ownership disputes.
          </li>
          <li>
            <span className="font-semibold">Using a club transponder:</span> If the club
            lends you a transponder for the day, a race official can link the transponder
            to your entry directly from race control during the race.
          </li>
        </ol>
      </section>

      {/* Section 5: Finding and Entering an Event */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">5. Finding and Entering an Event</h2>
        <p className="text-sm mb-3">
          Events are visible on the public schedule once published. You can submit an
          entry online when the event is in "Open" status.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">Browse events:</span> Click
            <span className="font-semibold"> Events</span> in the racer navigation (or
            visit <span className="font-mono text-xs">/events</span> without logging in).
            The page lists upcoming events with their date, venue, and entry status.
          </li>
          <li>
            <span className="font-semibold">Open an event:</span> Click an event to view
            its detail page. Published events show the list of racing classes available.
          </li>
          <li>
            <span className="font-semibold">Submit an entry:</span> When the event is
            Open, an entry form appears. Select the racing class you wish to enter, then
            select your car. The car's transponder number is recorded as a snapshot at
            submission time.
          </li>
          <li>
            <span className="font-semibold">Entry deadline:</span> Entries close when
            the club administrator closes the entry period (status changes to "Entries
            Closed"). Submit your entry before the deadline — late entries require the
            club secretary to add you manually.
          </li>
        </ol>
      </section>

      {/* Section 6: Managing Your Entries */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">6. Managing Your Entries</h2>
        <p className="text-sm mb-3">
          You can view and manage your event entries from the Entries section of your
          racer portal.
        </p>
        <ol className="list-decimal list-inside space-y-2 text-sm">
          <li>
            <span className="font-semibold">View your entries:</span> Click
            <span className="font-semibold"> Entries</span> in the racer navigation. All
            your submitted entries are listed, showing the event name, class, car, and
            current status.
          </li>
          <li>
            <span className="font-semibold">Withdraw an entry:</span> If you can no
            longer attend an event, withdraw your entry before the entry period closes.
            Select the entry and click <span className="font-semibold">Withdraw</span>.
            Withdrawn entries cannot be re-submitted unless the event is still Open.
          </li>
          <li>
            <span className="font-semibold">Entry confirmation:</span> Once the event
            is in progress, your entry is locked. Contact a club official if you need
            to make changes on the day.
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
