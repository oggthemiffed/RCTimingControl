// Race control's check-in page (L11): the check-in desk, plus the transponder swap tool.
import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { useHelp } from '@/context/HelpContext';
import { CheckInHelp } from '@/help/CheckInHelp';
import CheckInDesk from './CheckInDesk';
import TransponderSwap from './TransponderSwap';

export default function CheckInPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const id = Number(eventId);
  const { setHelpContent } = useHelp();

  useEffect(() => {
    setHelpContent(<CheckInHelp />);
    return () => setHelpContent(null);
  }, [setHelpContent]);

  return (
    <div className="h-full overflow-y-auto">
      <div className="mx-auto grid max-w-5xl grid-cols-1 gap-6 p-6 lg:grid-cols-2">
        <Card className="p-6">
          <h1 className="mb-4 text-lg font-semibold">Check-in desk</h1>
          <CheckInDesk eventId={id} />
        </Card>
        <Card className="self-start p-6">
          <h2 className="mb-4 text-lg font-semibold">Swap a transponder</h2>
          <TransponderSwap eventId={id} />
        </Card>
      </div>
    </div>
  );
}
