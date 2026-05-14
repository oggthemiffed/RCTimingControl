import { useState, useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Copy, AlertTriangle, Download } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormMessage,
} from '@/components/ui/form';
import {
  getForwarderTokenStatus,
  generateForwarderToken,
  revokeForwarderToken,
  fetchForwarderStatus,
  type ForwarderTokenStatus,
} from '@/lib/raceControlApi';
import { updateDecoderConfig } from '@/lib/setupApi';
import api from '@/lib/api';

const PORT_DEFAULTS: Record<string, number> = { RC4: 5100, P3: 5403 };
const MAX_ATTEMPTS = 15;

const schema = z.object({
  decoderHost: z.string().min(1, 'Decoder host required').max(255),
  decoderPort: z.coerce.number().int().min(1).max(65535),
  decoderProtocol: z.enum(['RC4', 'P3']),
});

type FormValues = z.infer<typeof schema>;
type ConfirmAction = 'regenerate' | 'revoke' | null;

interface Props {
  onNext: () => void;
  onBack?: () => void;
}

export default function DecoderConfigStep({ onNext, onBack }: Props) {
  const queryClient = useQueryClient();

  // ── Decoder form ───────────────────────────────────────────────────────────
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    mode: 'onBlur',
    defaultValues: { decoderHost: '', decoderProtocol: 'RC4', decoderPort: 5100 },
  });

  const userEditedPortRef = useRef(false);
  const watchedProtocol = form.watch('decoderProtocol');

  useEffect(() => {
    if (!userEditedPortRef.current) {
      form.setValue('decoderPort', PORT_DEFAULTS[watchedProtocol], { shouldDirty: false });
    }
  }, [watchedProtocol, form]);

  // ── Token state ────────────────────────────────────────────────────────────
  const [confirmAction, setConfirmAction] = useState<ConfirmAction>(null);
  const [newToken, setNewToken] = useState<string | null>(null);

  const { data: tokenStatus } = useQuery({
    queryKey: ['forwarder-token-status'],
    queryFn: getForwarderTokenStatus,
  });

  const generateMutation = useMutation({
    mutationFn: generateForwarderToken,
    onSuccess: (result) => {
      setNewToken(result.token);
      setConfirmAction(null);
      queryClient.invalidateQueries({ queryKey: ['forwarder-token-status'] });
    },
    onError: (error: Error) => toast.error(`Failed to generate token: ${error.message}`),
  });

  const revokeMutation = useMutation({
    mutationFn: revokeForwarderToken,
    onSuccess: () => {
      toast.success('Token revoked.');
      setConfirmAction(null);
      queryClient.invalidateQueries({ queryKey: ['forwarder-token-status'] });
    },
    onError: (error: Error) => toast.error(`Failed to revoke token: ${error.message}`),
  });

  const tokenStatusValue: ForwarderTokenStatus = tokenStatus?.status ?? 'NONE';

  // ── Test Connection polling ────────────────────────────────────────────────
  const [polling, setPolling] = useState(false);
  const [attempts, setAttempts] = useState(0);
  const [testResult, setTestResult] = useState<'idle' | 'connected' | 'timeout'>('idle');

  const statusQuery = useQuery({
    queryKey: ['forwarder-status-test'],
    queryFn: fetchForwarderStatus,
    enabled: polling && attempts < MAX_ATTEMPTS,
    refetchInterval: polling && attempts < MAX_ATTEMPTS ? 2000 : false,
    staleTime: 0,
  });

  // dataUpdatedAt changes on every successful fetch (even when structural data is unchanged),
  // so this fires reliably once per refetch cycle regardless of TanStack Query's structural sharing.
  useEffect(() => {
    if (!polling || statusQuery.dataUpdatedAt === 0) return;
    if (statusQuery.data?.forwarderState === 'CONNECTED') {
      setTestResult('connected');
      setPolling(false);
    } else {
      setAttempts(a => {
        const next = a + 1;
        if (next >= MAX_ATTEMPTS) {
          setTestResult('timeout');
          setPolling(false);
        }
        return next;
      });
    }
  }, [statusQuery.dataUpdatedAt, polling]); // eslint-disable-line react-hooks/exhaustive-deps

  const onTestConnection = () => {
    setAttempts(0);
    setTestResult('idle');
    setPolling(true);
  };

  // ── Download forwarder.env ─────────────────────────────────────────────────
  const onDownload = async () => {
    try {
      const response = await api.get('/api/v1/setup/forwarder-config-download', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const a = document.createElement('a');
      a.href = url;
      a.download = 'forwarder.env';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch {
      toast.error('Could not download forwarder.env. Try again.');
    }
  };

  // ── Save ───────────────────────────────────────────────────────────────────
  async function onSave(values: FormValues) {
    try {
      await updateDecoderConfig(values);
      queryClient.invalidateQueries({ queryKey: ['setup-status'] });
      queryClient.invalidateQueries({ queryKey: ['setup-progress'] });
      toast.success('Decoder configuration saved');
      onNext();
    } catch {
      toast.error('Could not save decoder configuration. Try again.');
    }
  }

  function formatDate(dateStr: string | null): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleString();
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-2">Decoder Config</h1>
      <p className="text-sm text-muted-foreground mb-6">
        Configure your AMB decoder connection so the forwarder can send live lap data.
      </p>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSave)} className="space-y-4">

          {/* Decoder connection fields */}
          <FormField
            control={form.control}
            name="decoderHost"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Decoder Host</FormLabel>
                <FormControl>
                  <Input placeholder="e.g. 192.168.1.50" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="decoderProtocol"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Protocol</FormLabel>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="RC4">RC4 (firmware &lt; 4.5, port 5100)</SelectItem>
                    <SelectItem value="P3">P3 binary (firmware ≥ 4.5, port 5403)</SelectItem>
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="decoderPort"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Port</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={1}
                    max={65535}
                    step={1}
                    {...field}
                    onChange={(e) => {
                      userEditedPortRef.current = true;
                      field.onChange(e);
                    }}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Token management */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Forwarder API Token</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {newToken ? (
                <>
                  <div className="flex items-start gap-2 p-3 rounded-md bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800">
                    <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
                    <p className="text-sm text-amber-800 dark:text-amber-200">
                      Copy this token now — it will not be shown again.
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Input value={newToken} readOnly className="font-mono text-sm" aria-label="Forwarder API token" />
                    <Button
                      type="button"
                      variant="outline"
                      size="icon"
                      onClick={() => { navigator.clipboard.writeText(newToken); toast.success('Token copied to clipboard.'); }}
                      aria-label="Copy token to clipboard"
                    >
                      <Copy className="h-4 w-4" />
                    </Button>
                  </div>
                  <Button type="button" variant="ghost" onClick={() => setNewToken(null)}>Done</Button>
                </>
              ) : tokenStatusValue === 'NONE' || tokenStatusValue === 'REVOKED' ? (
                <>
                  {tokenStatusValue === 'REVOKED' && (
                    <div className="flex items-center gap-3 mb-2">
                      <span className="text-sm">Status</span>
                      <Badge variant="destructive">Revoked</Badge>
                    </div>
                  )}
                  <p className="text-sm text-muted-foreground">
                    No token has been generated yet. Generate a token to allow the forwarder to connect.
                  </p>
                  <Button
                    type="button"
                    onClick={() => generateMutation.mutate()}
                    disabled={generateMutation.isPending}
                  >
                    {generateMutation.isPending ? 'Generating…' : 'Generate Token'}
                  </Button>
                </>
              ) : (
                <>
                  <div className="flex items-center gap-3">
                    <span className="text-sm">Status</span>
                    <Badge variant="outline" className="text-[var(--flag-green)] border-[var(--flag-green)]">
                      <span className="inline-block h-2 w-2 rounded-full bg-[var(--flag-green)] mr-1.5" />
                      Active
                    </Badge>
                  </div>
                  <div className="text-sm">
                    <span className="text-muted-foreground">Generated</span>{' '}
                    {formatDate(tokenStatus?.generatedAt ?? null)}
                  </div>
                  <p className="text-sm text-muted-foreground">
                    The token value is not shown for security. Regenerate to get a new token.
                  </p>
                  {confirmAction === null && (
                    <div className="flex gap-2">
                      <Button type="button" variant="outline" onClick={() => setConfirmAction('regenerate')}>
                        Regenerate Token
                      </Button>
                      <Button type="button" variant="destructive" onClick={() => setConfirmAction('revoke')}>
                        Revoke Token
                      </Button>
                    </div>
                  )}
                  {confirmAction === 'regenerate' && (
                    <div className="space-y-3 p-3 rounded-md border bg-muted/50">
                      <p className="text-sm">Regenerating will disconnect the forwarder until you update its config. Continue?</p>
                      <div className="flex gap-2">
                        <Button type="button" variant="ghost" onClick={() => setConfirmAction(null)}>Keep current token</Button>
                        <Button type="button" onClick={() => generateMutation.mutate()} disabled={generateMutation.isPending}>
                          {generateMutation.isPending ? 'Regenerating…' : 'Confirm Regenerate'}
                        </Button>
                      </div>
                    </div>
                  )}
                  {confirmAction === 'revoke' && (
                    <div className="space-y-3 p-3 rounded-md border bg-destructive/10">
                      <p className="text-sm">Revoking will disconnect the forwarder immediately. Continue?</p>
                      <div className="flex gap-2">
                        <Button type="button" variant="ghost" onClick={() => setConfirmAction(null)}>Keep token</Button>
                        <Button type="button" variant="destructive" onClick={() => revokeMutation.mutate()} disabled={revokeMutation.isPending}>
                          {revokeMutation.isPending ? 'Revoking…' : 'Confirm Revoke'}
                        </Button>
                      </div>
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>

          {/* Download forwarder.env */}
          <div>
            <Button
              type="button"
              variant="outline"
              onClick={onDownload}
              disabled={tokenStatusValue !== 'ACTIVE'}
            >
              <Download className="h-4 w-4 mr-2" />
              Download forwarder.env
            </Button>
          </div>

          {/* Test Connection */}
          <div className="space-y-2">
            <Button type="button" variant="outline" onClick={onTestConnection} disabled={polling}>
              {polling ? 'Testing…' : 'Test Connection'}
            </Button>
            {testResult === 'connected' && (
              <div className="flex items-center gap-2">
                <Badge variant="outline" className="text-[var(--flag-green)] border-[var(--flag-green)]">
                  Connected
                </Badge>
                <span className="text-sm text-muted-foreground">Connection confirmed. You can proceed.</span>
              </div>
            )}
            {testResult === 'timeout' && (
              <div className="flex items-start gap-2 p-3 rounded-md bg-muted/50 border">
                <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5 text-muted-foreground" />
                <p className="text-sm">
                  Forwarder not yet connected. Make sure the forwarder JAR is running with the downloaded config.
                </p>
              </div>
            )}
          </div>

          {/* Action row */}
          <div className="flex justify-between gap-2 pt-4">
            <Button type="button" variant="ghost" onClick={onBack}>
              Back
            </Button>
            <div className="flex gap-2">
              <Button type="button" variant="ghost" onClick={onNext}>
                Skip for now
              </Button>
              <Button type="submit" disabled={form.formState.isSubmitting}>
                Save and Finish
              </Button>
            </div>
          </div>
        </form>
      </Form>

      <a href="/admin/forwarder" className="text-sm text-muted-foreground underline mt-4 inline-block">
        Manage more in Admin →
      </a>
    </div>
  );
}
