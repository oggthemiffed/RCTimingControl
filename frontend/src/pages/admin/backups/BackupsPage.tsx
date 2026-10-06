import { useEffect } from 'react';
import { isAxiosError } from 'axios';
import { DatabaseBackup, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { useAdminBackups, useBackupNow } from '@/hooks/admin/useAdminBackups';
import { useHelp } from '@/context/HelpContext';
import { BackupsHelp } from '@/help/BackupsHelp';

const REASONS: Record<string, string> = {
  manual: 'Taken by hand',
  nightly: 'Nightly',
  'day-close': 'Race day closed',
};

function formatSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * Database backups (#22). The app backs up when a race day is closed and every night; an admin
 * can take one now. Restoring is done with the app stopped, using the restore command.
 */
export default function BackupsPage() {
  const { setHelpContent } = useHelp();
  useEffect(() => {
    setHelpContent(<BackupsHelp />);
    return () => setHelpContent(null);
  }, [setHelpContent]);
  const { data, isLoading, isError } = useAdminBackups();
  const backupNow = useBackupNow();

  function onBackupNow() {
    backupNow.mutate(undefined, {
      onSuccess: backup => toast.success(`Backed up to ${backup.name}`),
      onError: err => {
        const detail = isAxiosError(err) ? err.response?.data?.detail : undefined;
        toast.error(detail ?? 'The backup failed. Check the backup folder is there and has space.');
      },
    });
  }

  return (
    <div className="max-w-2xl space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Backups</h1>
          <p className="text-sm text-muted-foreground mt-1">
            The app backs up its database when you complete an event (that closes the race day) and every
            night, keeping the newest copies. Backing up is safe while racing carries on.
          </p>
        </div>
        <Button onClick={onBackupNow} disabled={backupNow.isPending}>
          {backupNow.isPending && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
          Back up now
        </Button>
      </div>

      {data && (
        <p className="text-sm">
          Backup folder: <code className="rounded bg-muted px-1 py-0.5 text-xs break-all">{data.directory}</code>
        </p>
      )}

      {isLoading && (
        <div className="flex items-center gap-2 py-8">
          <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
          <span className="text-sm text-muted-foreground">Loading backups…</span>
        </div>
      )}

      {isError && <p className="text-sm text-destructive">Could not load backups. Check the backup folder is reachable.</p>}

      {data && data.backups.length === 0 && (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <DatabaseBackup className="h-10 w-10 text-muted-foreground mb-4" aria-hidden="true" />
          <h2 className="text-lg font-semibold">No backups yet</h2>
          <p className="text-sm text-muted-foreground mt-1">
            The first one is taken tonight or when you close a race day. You can take one now.
          </p>
        </div>
      )}

      {data && data.backups.length > 0 && (
        <ul className="divide-y rounded-lg border" aria-label="Backups">
          {data.backups.map(b => (
            <li key={b.name} className="flex items-center justify-between gap-4 px-4 py-3">
              <div className="min-w-0">
                <p className="font-medium">{new Date(b.createdAt).toLocaleString()}</p>
                <p className="text-xs text-muted-foreground truncate">{b.name}</p>
              </div>
              <span className="text-xs text-muted-foreground text-right shrink-0">
                {[REASONS[b.reason] ?? b.reason, formatSize(b.sizeBytes)].join(' · ')}
              </span>
            </li>
          ))}
        </ul>
      )}

      <div className="rounded-lg border p-4 text-sm space-y-2">
        <h2 className="font-semibold">Restoring a backup</h2>
        <p className="text-muted-foreground">
          Stop the app, then run <code className="rounded bg-muted px-1 py-0.5 text-xs">java -jar app.jar restore &lt;backup file&gt;</code>.
          The current database is kept beside the restored one, and the app picks the restored data up when it
          starts again.
        </p>
      </div>
    </div>
  );
}
