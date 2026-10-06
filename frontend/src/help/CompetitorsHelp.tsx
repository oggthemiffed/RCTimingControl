import { GuideLinks } from './GuideLinks';

export function CompetitorsHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Competitors are the drivers RCTC has seen, from RaceHub imports, CSV imports and walk-ins.
        They never sign in. The announcer reads each name aloud at the grid call and in the
        running order.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Say as:</span> If a name is mispronounced, click Say as… on the row and type it the way it should sound, such as <em>Shiv-awn Keen</em> for Siobhan Keane. The display name on results and boards does not change.</li>
        <li><span className="font-semibold">Play:</span> Click Play to hear it in the announcer voice (the voice set under Audio) before you save. If that voice is not running, Play uses the browser&apos;s voice, which may sound different.</li>
        <li><span className="font-semibold">Clear:</span> Click Clear, or save it empty, to go back to saying the name as written.</li>
        <li><span className="font-semibold">Stays with the driver:</span> It belongs to the competitor, so it carries over to later meetings. A RaceHub or CSV import never changes it.</li>
        <li><span className="font-semibold">When it takes effect:</span> From the next race to reach the grid. The audio for a race is made when it gets there, so a race already on the grid keeps its old voice clips.</li>
      </ul>

      <GuideLinks admin />
    </div>
  );
}
