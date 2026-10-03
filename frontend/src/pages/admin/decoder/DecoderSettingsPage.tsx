import { DecoderSettingsForm } from '@/components/decoder/DecoderSettingsForm';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export default function DecoderSettingsPage() {
  return (
    <div>
      <h1 className="text-2xl font-semibold mb-2">Decoder</h1>
      <p className="text-sm text-muted-foreground mb-6">
        The AMB decoder that RCTC reads live laps from. RCTC connects to it directly; no separate
        forwarder is needed.
      </p>

      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle className="text-base">Connection</CardTitle>
        </CardHeader>
        <CardContent>
          <DecoderSettingsForm saveLabel="Save" />
        </CardContent>
      </Card>
    </div>
  );
}
