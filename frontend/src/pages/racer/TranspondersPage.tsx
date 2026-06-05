import { useEffect } from 'react';
import { useHelp } from '@/context/HelpContext';
import { CarTransponderHelp } from '@/help/CarTransponderHelp';

export default function TranspondersPage() {
  const { setHelpContent } = useHelp();

  useEffect(() => {
    setHelpContent(<CarTransponderHelp />);
    return () => setHelpContent(null);
  }, [setHelpContent]);

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-4">Transponders</h1>
      <div className="text-muted-foreground">Transponders — coming in Plan 06.</div>
    </div>
  );
}
