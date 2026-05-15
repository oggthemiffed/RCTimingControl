export function RacerProfileHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Profile page lets you manage your personal details, emergency contact,
        governing body memberships, and announcement voice preference. Your email address
        is fixed and cannot be changed here. Ability ratings shown at the bottom are
        assigned by the club and are read-only.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Save changes:</span> Edit your First name, Last name, Phone number, or Emergency contact fields and click "Save changes" — the button is only active when you have unsaved edits.</li>
        <li><span className="font-semibold">Phonetic name:</span> Enter your name spelled out phonetically (e.g. "David AN-der-son") so the announcement system pronounces it correctly during races.</li>
        <li><span className="font-semibold">Add membership:</span> Select your governing body and enter your membership number, then click "Add" to register it. Multiple memberships are supported.</li>
        <li><span className="font-semibold">Announcement Voice:</span> Select a voice from the dropdown, click "Preview" to hear how your name sounds, then click "Save Voice Preferences" to apply it.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>
          The "Save changes" button is greyed out until you edit a field — if you think
          your changes did not save, check whether the button was active before clicking
          away. Voice preferences are saved separately from profile details — remember to
          click "Save Voice Preferences" after choosing a voice.
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
