import { useEffect } from 'react';
import { useHelp } from '@/context/HelpContext';
import { EventEntryHelp } from '@/help/EventEntryHelp';

export default function EntriesPage() {
  const { setHelpContent } = useHelp();

  useEffect(() => {
    setHelpContent(<EventEntryHelp />);
    return () => setHelpContent(null);
  }, [setHelpContent]);

  return <div className="text-muted-foreground">Entries — coming in Plan 06.</div>;
}
