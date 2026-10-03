import { useState, useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { AlertTriangle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
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
import { fetchDecoderStatus } from '@/lib/raceControlApi';
import { getDecoderConfig, updateDecoderConfig } from '@/lib/setupApi';

const PORT_DEFAULTS: Record<string, number> = { RC4: 5100, P3: 5403 };
const MAX_ATTEMPTS = 15;

const schema = z.object({
  decoderHost: z.string().min(1, 'Decoder host required').max(255),
  decoderPort: z.coerce.number().int().min(1).max(65535),
  decoderProtocol: z.enum(['RC4', 'P3']),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  /** Called after the settings are saved. */
  onSaved?: () => void;
  /** Adds a Back button when provided. */
  onBack?: () => void;
  /** Adds a Skip button when provided. */
  onSkip?: () => void;
  saveLabel?: string;
}

/**
 * Decoder host, protocol and port, with Test Connection. Used by the setup wizard and the
 * admin Decoder page. Pre-fills from the saved settings.
 */
export function DecoderSettingsForm({ onSaved, onBack, onSkip, saveLabel = 'Save' }: Props) {
  const queryClient = useQueryClient();

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

  // Pre-fill from the saved settings. A saved port is kept rather than replaced by the protocol default.
  const configQuery = useQuery({
    queryKey: ['decoder-config'],
    queryFn: getDecoderConfig,
  });

  useEffect(() => {
    const saved = configQuery.data;
    if (saved?.decoderHost && saved.decoderPort && saved.decoderProtocol) {
      userEditedPortRef.current = true;
      form.reset({
        decoderHost: saved.decoderHost,
        decoderPort: saved.decoderPort,
        decoderProtocol: saved.decoderProtocol,
      });
    }
  }, [configQuery.data, form]);

  // ── Test Connection polling ────────────────────────────────────────────────
  const [polling, setPolling] = useState(false);
  const [attempts, setAttempts] = useState(0);
  const [testResult, setTestResult] = useState<'idle' | 'connected' | 'timeout'>('idle');

  const statusQuery = useQuery({
    queryKey: ['decoder-status-test'],
    queryFn: fetchDecoderStatus,
    enabled: polling && attempts < MAX_ATTEMPTS,
    refetchInterval: polling && attempts < MAX_ATTEMPTS ? 2000 : false,
    staleTime: 0,
  });

  // dataUpdatedAt changes on every successful fetch (even when structural data is unchanged),
  // so this fires reliably once per refetch cycle regardless of TanStack Query's structural sharing.
  useEffect(() => {
    if (!polling || statusQuery.dataUpdatedAt === 0) return;
    if (statusQuery.data?.decoderState === 'CONNECTED') {
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

  // ── Save ───────────────────────────────────────────────────────────────────
  async function onSave(values: FormValues) {
    try {
      await updateDecoderConfig(values);
      queryClient.invalidateQueries({ queryKey: ['decoder-config'] });
      queryClient.invalidateQueries({ queryKey: ['setup-status'] });
      queryClient.invalidateQueries({ queryKey: ['setup-progress'] });
      toast.success('Decoder configuration saved');
      onSaved?.();
    } catch {
      toast.error('Could not save decoder configuration. Try again.');
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSave)} className="space-y-4">
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
                Decoder not yet connected. Check the decoder address and that the decoder is powered on.
              </p>
            </div>
          )}
        </div>

        <div className="flex justify-between gap-2 pt-4">
          <div>
            {onBack && (
              <Button type="button" variant="ghost" onClick={onBack}>
                Back
              </Button>
            )}
          </div>
          <div className="flex gap-2">
            {onSkip && (
              <Button type="button" variant="ghost" onClick={onSkip}>
                Skip for now
              </Button>
            )}
            <Button type="submit" disabled={form.formState.isSubmitting}>
              {saveLabel}
            </Button>
          </div>
        </div>
      </form>
    </Form>
  );
}
