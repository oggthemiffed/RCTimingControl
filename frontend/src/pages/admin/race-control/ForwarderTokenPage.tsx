import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Copy, AlertTriangle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  getForwarderTokenStatus,
  generateForwarderToken,
  revokeForwarderToken,
  type ForwarderTokenStatus,
} from '@/lib/raceControlApi';

type ConfirmAction = 'regenerate' | 'revoke' | null;

export default function ForwarderTokenPage() {
  const queryClient = useQueryClient();
  const [confirmAction, setConfirmAction] = useState<ConfirmAction>(null);
  const [newToken, setNewToken] = useState<string | null>(null);

  const { data: tokenStatus, isLoading } = useQuery({
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
    onError: (error: Error) => {
      toast.error(`Failed to generate token: ${error.message}`);
    },
  });

  const revokeMutation = useMutation({
    mutationFn: revokeForwarderToken,
    onSuccess: () => {
      toast.success('Token revoked.');
      setConfirmAction(null);
      queryClient.invalidateQueries({ queryKey: ['forwarder-token-status'] });
    },
    onError: (error: Error) => {
      toast.error(`Failed to revoke token: ${error.message}`);
    },
  });

  function handleCopyToken() {
    if (newToken) {
      navigator.clipboard.writeText(newToken);
      toast.success('Token copied to clipboard.');
    }
  }

  function handleDone() {
    setNewToken(null);
  }

  function formatDate(dateStr: string | null): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleString();
  }

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-semibold mb-6">Forwarder Token</h1>
        <Card>
          <CardContent className="py-8">
            <div className="h-8 bg-muted animate-pulse rounded" />
          </CardContent>
        </Card>
      </div>
    );
  }

  const status: ForwarderTokenStatus = tokenStatus?.status ?? 'NONE';

  // State C: Token just generated (one-time reveal)
  if (newToken) {
    return (
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-semibold mb-6">Forwarder Token</h1>
        <Card>
          <CardHeader>
            <CardTitle>Forwarder API Token</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-start gap-2 p-3 rounded-md bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800">
              <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
              <p className="text-sm text-amber-800 dark:text-amber-200">
                Copy this token now. It will not be shown again.
              </p>
            </div>

            <div className="flex gap-2">
              <Input
                value={newToken}
                readOnly
                className="font-mono text-sm"
                aria-label="Forwarder API token"
              />
              <Button
                variant="outline"
                size="icon"
                onClick={handleCopyToken}
                aria-label="Copy token to clipboard"
              >
                <Copy className="h-4 w-4" />
              </Button>
            </div>

            <p className="text-xs text-muted-foreground">
              Paste this value into forwarder.properties as{' '}
              <code className="bg-muted px-1 rounded">forwarder.api-token=&lt;value&gt;</code>
            </p>

            <Button onClick={handleDone}>Done</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  // State A: No token generated yet
  if (status === 'NONE') {
    return (
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-semibold mb-6">Forwarder Token</h1>
        <Card>
          <CardHeader>
            <CardTitle>Forwarder API Token</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">
              No token has been generated yet. Generate a token to allow the forwarder
              application to connect to this service.
            </p>
            <Button
              onClick={() => generateMutation.mutate()}
              disabled={generateMutation.isPending}
            >
              {generateMutation.isPending ? 'Generating…' : 'Generate Token'}
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  // State D: Token revoked
  if (status === 'REVOKED') {
    return (
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-semibold mb-6">Forwarder Token</h1>
        <Card>
          <CardHeader>
            <CardTitle>Forwarder API Token</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-3">
              <span className="text-sm">Status</span>
              <Badge variant="destructive">Revoked</Badge>
            </div>
            <p className="text-sm text-muted-foreground">
              The forwarder cannot connect until a new token is generated.
            </p>
            <Button
              onClick={() => generateMutation.mutate()}
              disabled={generateMutation.isPending}
            >
              {generateMutation.isPending ? 'Generating…' : 'Generate Token'}
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  // State B: Token exists (ACTIVE)
  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-semibold mb-6">Forwarder Token</h1>
      <Card>
        <CardHeader>
          <CardTitle>Forwarder API Token</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-3">
            <span className="text-sm">Status</span>
            <Badge
              variant="outline"
              className="text-[var(--flag-green)] border-[var(--flag-green)]"
            >
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
              <Button variant="outline" onClick={() => setConfirmAction('regenerate')}>
                Regenerate Token
              </Button>
              <Button variant="destructive" onClick={() => setConfirmAction('revoke')}>
                Revoke Token
              </Button>
            </div>
          )}

          {confirmAction === 'regenerate' && (
            <div className="space-y-3 p-3 rounded-md border bg-muted/50">
              <p className="text-sm">
                Regenerating will disconnect the forwarder until you update its config. Continue?
              </p>
              <div className="flex gap-2">
                <Button variant="ghost" onClick={() => setConfirmAction(null)}>
                  Keep current token
                </Button>
                <Button
                  onClick={() => generateMutation.mutate()}
                  disabled={generateMutation.isPending}
                >
                  {generateMutation.isPending ? 'Regenerating…' : 'Confirm Regenerate'}
                </Button>
              </div>
            </div>
          )}

          {confirmAction === 'revoke' && (
            <div className="space-y-3 p-3 rounded-md border bg-destructive/10">
              <p className="text-sm">
                Revoking will disconnect the forwarder immediately. Continue?
              </p>
              <div className="flex gap-2">
                <Button variant="ghost" onClick={() => setConfirmAction(null)}>
                  Keep token
                </Button>
                <Button
                  variant="destructive"
                  onClick={() => revokeMutation.mutate()}
                  disabled={revokeMutation.isPending}
                >
                  {revokeMutation.isPending ? 'Revoking…' : 'Confirm Revoke'}
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
