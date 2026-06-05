import { useEffect } from 'react';
import { useHelp } from '@/context/HelpContext';
import { EventEntryHelp } from '@/help/EventEntryHelp';

export default function EntriesPage() {
  const { setHelpContent } = useHelp();

  useEffect(() => {
    setHelpContent(<EventEntryHelp />);
    return () => setHelpContent(null);
  }, [setHelpContent]);

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-4">Entries</h1>
      <div className="text-muted-foreground">Entries — coming in Plan 06.</div>
    </div>
  );
}
