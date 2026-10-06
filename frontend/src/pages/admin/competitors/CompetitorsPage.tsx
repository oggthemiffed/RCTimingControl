import { useEffect, useState } from 'react';
import { Loader2, Play, Users } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useAdminCompetitorsList, useSetSpokenName } from '@/hooks/admin/useAdminCompetitors';
import { useHelp } from '@/context/HelpContext';
import { useAuth } from '@/hooks/useAuth';
import { CompetitorsHelp } from '@/help/CompetitorsHelp';
import { adminApi, type CompetitorSummaryDto } from '@/lib/adminApi';

const MAX_SPOKEN_NAME = 100;

/** Plays the text in the club's Piper voice, or the browser's voice when Piper is not reachable. */
async function speak(text: string): Promise<'piper' | 'browser'> {
  try {
    const wav = await adminApi.competitors.previewSpeech(text);
    const url = URL.createObjectURL(wav);
    const audio = new Audio(url);
    audio.addEventListener('ended', () => URL.revokeObjectURL(url));
    await audio.play();
    return 'piper';
  } catch {
    if (typeof window !== 'undefined' && window.speechSynthesis) {
      window.speechSynthesis.speak(new SpeechSynthesisUtterance(text));
    }
    return 'browser';
  }
}

/** One competitor, with an editor for how their name is said aloud (#119). */
function CompetitorRow({ competitor, canEdit }: { competitor: CompetitorSummaryDto; canEdit: boolean }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState('');
  const [note, setNote] = useState<string | null>(null);
  const save = useSetSpokenName();

  const meta = [competitor.brcaNumber && `BRCA ${competitor.brcaNumber}`, competitor.homeClub]
    .filter(Boolean).join(' · ');
  const heard = draft.trim() || competitor.displayName;

  const startEditing = () => {
    setDraft(competitor.spokenName ?? '');
    setNote(null);
    setEditing(true);
  };

  const submit = (text: string) =>
    save.mutate({ id: competitor.id, spokenName: text }, { onSuccess: () => setEditing(false) });

  const play = async () => {
    setNote(null);
    const used = await speak(heard);
    if (used === 'browser') setNote('The announcer voice is not available, so this used the browser voice.');
  };

  return (
    <li className="px-4 py-3 space-y-2">
      <div className="flex items-center justify-between gap-4">
        <span className="font-medium">{competitor.displayName}</span>
        <span className="text-xs text-muted-foreground text-right">{meta}</span>
      </div>
      {editing ? (
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <Input
              value={draft}
              onChange={e => setDraft(e.target.value)}
              maxLength={MAX_SPOKEN_NAME}
              placeholder={`Say as (leave empty to say “${competitor.displayName}”)`}
              aria-label={`Say ${competitor.displayName} as`}
            />
            <Button type="button" variant="outline" size="sm" onClick={play}
              aria-label={`Play ${competitor.displayName}`}>
              <Play className="h-4 w-4 mr-1" aria-hidden="true" />Play
            </Button>
          </div>
          <div className="flex items-center gap-2">
            <Button type="button" size="sm" disabled={save.isPending} onClick={() => submit(draft)}>Save</Button>
            {competitor.spokenName && (
              <Button type="button" variant="outline" size="sm" disabled={save.isPending}
                onClick={() => submit('')}>Clear</Button>
            )}
            <Button type="button" variant="ghost" size="sm" onClick={() => setEditing(false)}>Cancel</Button>
          </div>
          {note && <p className="text-xs text-muted-foreground">{note}</p>}
          {save.isError && <p className="text-xs text-destructive">Could not save. Try again.</p>}
        </div>
      ) : (
        <div className="flex items-center justify-between gap-4 text-sm">
          <span className="text-muted-foreground">
            {competitor.spokenName ? <>Say as: <span className="text-foreground">{competitor.spokenName}</span></> : 'Said as written'}
          </span>
          {canEdit && (
            <Button type="button" variant="ghost" size="sm" onClick={startEditing}
              aria-label={`Edit how ${competitor.displayName} is said`}>
              Say as…
            </Button>
          )}
        </div>
      )}
    </li>
  );
}

/**
 * Every competitor the club has timed: imported from RaceHub or added as a walk-in (L10, #18).
 * Replaces the old racer list; competitors have no login.
 */
export default function CompetitorsPage() {
  const [search, setSearch] = useState('');
  const { setHelpContent } = useHelp();
  // Race directors and referees can see the list, but only admins change how a name is said
  const canEdit = useAuth().user?.roles.includes('ADMIN') ?? false;
  useEffect(() => {
    setHelpContent(<CompetitorsHelp />);
    return () => setHelpContent(null);
  }, [setHelpContent]);
  const { data: competitors, isLoading, isError } = useAdminCompetitorsList();

  const query = search.trim().toLowerCase();
  const shown = (competitors ?? []).filter(c =>
    !query
    || c.displayName.toLowerCase().includes(query)
    || (c.brcaNumber ?? '').toLowerCase().includes(query)
    || (c.homeClub ?? '').toLowerCase().includes(query));

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Competitors</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Drivers imported from RaceHub or added as walk-ins. If the announcer says a name wrongly, an admin can use Say as… to type how it should sound.
        </p>
      </div>

      {isLoading && (
        <div className="flex items-center gap-2 py-8">
          <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
          <span className="text-sm text-muted-foreground">Loading competitors…</span>
        </div>
      )}

      {isError && <p className="text-sm text-destructive">Could not load competitors. Try again.</p>}

      {!isLoading && !isError && competitors?.length === 0 && (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <Users className="h-10 w-10 text-muted-foreground mb-4" aria-hidden="true" />
          <h2 className="text-lg font-semibold">No competitors yet</h2>
          <p className="text-sm text-muted-foreground mt-1">
            Competitors appear here when you import entries from RaceHub or add a walk-in.
          </p>
        </div>
      )}

      {!isLoading && competitors && competitors.length > 0 && (
        <>
          <Input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search by name, BRCA number or club"
            aria-label="Search competitors"
          />
          {shown.length === 0 ? (
            <p className="text-sm text-muted-foreground">No competitors match “{search.trim()}”.</p>
          ) : (
            <ul className="divide-y rounded-lg border" aria-label="Competitors">
              {shown.map(c => <CompetitorRow key={c.id} competitor={c} canEdit={canEdit} />)}
            </ul>
          )}
        </>
      )}
    </div>
  );
}
