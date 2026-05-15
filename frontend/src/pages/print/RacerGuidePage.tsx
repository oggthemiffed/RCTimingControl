import { useEffect } from 'react';

export default function RacerGuidePage() {
  useEffect(() => {
    document.title = 'Racer Quick-Start Guide';
  }, []);

  return (
    <div className="p-8 max-w-3xl mx-auto print:p-4">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold">Racer Quick-Start Guide</h1>
        <p className="text-sm text-muted-foreground mt-1">For racers — RC Timing Club</p>
      </div>

      <p className="text-sm text-muted-foreground italic">
        Content will be completed in Plan 09-04.
      </p>

      <div className="mt-6 print:hidden">
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
