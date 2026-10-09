import { useState } from 'react';
import { KeyRound, Loader2, Plus, ShieldCheck, UserCheck, UserX } from 'lucide-react';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { OfficialFields } from '@/components/OfficialFields';
import { OfficialRoleCheckboxes } from '@/components/OfficialRoleCheckboxes';
import { TextField } from '@/components/TextField';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Form } from '@/components/ui/form';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { useHelpContent } from '@/context/HelpContext';
import { OfficialsHelp } from '@/help/OfficialsHelp';
import {
  useAddOfficial,
  useChangeOfficialRoles,
  useOfficialChanges,
  useOfficials,
  useSetOfficialEnabled,
  useSetOfficialPassword,
} from '@/hooks/admin/useAdminOfficials';
import { useAuth } from '@/hooks/useAuth';
import { useRecheckConfirmation } from '@/hooks/useRecheckConfirmation';
import type { OfficialAction, OfficialDto, OfficialRole } from '@/lib/adminApi';
import { getApiErrorMessage } from '@/lib/errors';
import { formatDate, formatDateTime } from '@/lib/dates';
import { MIN_PASSWORD_LENGTH, officialSchema, passwordRule, PASSWORDS_DIFFER } from '@/lib/officials';
import type { OfficialFormValues } from '@/lib/officials';

const ROLE_LABEL: Record<OfficialRole, string> = {
  ADMIN: 'Admin',
  RACE_DIRECTOR: 'Race director',
  REFEREE: 'Referee',
};

const ACTION_LABEL: Record<OfficialAction, string> = {
  ADDED: 'Added',
  ROLES_CHANGED: 'Roles changed',
  PASSWORD_SET: 'Password set',
  DISABLED: 'Disabled',
  ENABLED: 'Enabled',
};

function AddOfficialDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const addOfficial = useAddOfficial();
  const form = useForm<OfficialFormValues>({
    resolver: zodResolver(officialSchema),
    defaultValues: { firstName: '', lastName: '', email: '', password: '', roles: ['RACE_DIRECTOR'] },
  });

  function close(next: boolean) {
    onOpenChange(next);
    if (!next) form.reset();
  }

  function submit(values: OfficialFormValues) {
    addOfficial.mutate(values, {
      onSuccess: official => {
        toast.success(`Added ${official.firstName} ${official.lastName}`);
        close(false);
      },
      onError: err => toast.error(getApiErrorMessage(err, 'Could not add the official. Try again.')),
    });
  }

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add an official</DialogTitle>
          <DialogDescription>Give them their password in person. They can sign in straight away.</DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(submit)} className="space-y-4">
            <OfficialFields control={form.control} rolesIdPrefix="add-role" />
            <DialogFooter>
              <Button type="submit" disabled={addOfficial.isPending}>
                {addOfficial.isPending && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
                Add official
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

function RolesDialog({ official, onClose }: { official: OfficialDto; onClose: () => void }) {
  const changeRoles = useChangeOfficialRoles();
  const [roles, setRoles] = useState<OfficialRole[]>(official.roles);

  function submit(e: React.FormEvent) {
    e.preventDefault();
    changeRoles.mutate({ id: official.id, roles }, {
      onSuccess: () => {
        toast.success(`Roles saved for ${official.firstName} ${official.lastName}`);
        onClose();
      },
      onError: err => toast.error(getApiErrorMessage(err, 'Could not change the roles. Try again.')),
    });
  }

  return (
    <Dialog open onOpenChange={open => { if (!open) onClose(); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Roles for {official.firstName} {official.lastName}</DialogTitle>
          <DialogDescription>Changes apply the next time they sign in.</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-4">
          <OfficialRoleCheckboxes idPrefix="edit-role" value={roles} onChange={setRoles} />
          <DialogFooter>
            <Button type="submit" disabled={roles.length === 0 || changeRoles.isPending}>
              {changeRoles.isPending && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
              Save roles
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

const passwordSchema = z
  .object({ password: passwordRule, confirm: z.string() })
  .refine(d => d.password === d.confirm, { message: PASSWORDS_DIFFER, path: ['confirm'] });
type PasswordFormValues = z.infer<typeof passwordSchema>;

function PasswordDialog({ official, onClose }: { official: OfficialDto; onClose: () => void }) {
  const setOfficialPassword = useSetOfficialPassword();
  // Checked as they type, so Set password stays off until the two match
  const form = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema),
    mode: 'onChange',
    defaultValues: { password: '', confirm: '' },
  });
  useRecheckConfirmation(form, 'password', 'confirm');

  function submit({ password }: PasswordFormValues) {
    setOfficialPassword.mutate({ id: official.id, password }, {
      onSuccess: () => {
        toast.success(`New password set for ${official.firstName} ${official.lastName}`);
        onClose();
      },
      onError: err => toast.error(getApiErrorMessage(err, 'Could not set the password. Try again.')),
    });
  }

  return (
    <Dialog open onOpenChange={open => { if (!open) onClose(); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New password for {official.firstName} {official.lastName}</DialogTitle>
          <DialogDescription>
            Tell them it in person. Their other sessions end, so they sign in again with the new password.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(submit)} className="space-y-4">
            <TextField
              control={form.control}
              name="password"
              label="New password"
              type="password"
              autoComplete="new-password"
              description={`At least ${MIN_PASSWORD_LENGTH} characters.`}
            />
            <TextField
              control={form.control}
              name="confirm"
              label="Type it again"
              type="password"
              autoComplete="new-password"
            />
            <DialogFooter>
              <Button type="submit" disabled={!form.formState.isValid || setOfficialPassword.isPending}>
                {setOfficialPassword.isPending && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
                Set password
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

function DisableDialog({ official, onClose }: { official: OfficialDto; onClose: () => void }) {
  const setEnabled = useSetOfficialEnabled();

  function confirm() {
    setEnabled.mutate({ id: official.id, enabled: false }, {
      onSuccess: () => {
        toast.success(`${official.firstName} ${official.lastName} can no longer sign in`);
        onClose();
      },
      onError: err => toast.error(getApiErrorMessage(err, 'Could not disable the official. Try again.')),
    });
  }

  return (
    <Dialog open onOpenChange={open => { if (!open) onClose(); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Disable {official.firstName} {official.lastName}?</DialogTitle>
          <DialogDescription>
            They can&apos;t sign in until you enable them again, and they are signed out within 15 minutes.
            Their name stays on everything they did.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button variant="destructive" onClick={confirm} disabled={setEnabled.isPending}>
            {setEnabled.isPending && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
            Disable
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/**
 * Officials (#61): who can sign in to RCTC and with which roles. Admins add officials, change
 * their roles, set a new password and disable or re-enable them. Every change is logged.
 */
export default function OfficialsPage() {
  const { user } = useAuth();
  const { data: officials, isLoading, isError } = useOfficials();
  const { data: changes, hasNextPage, fetchNextPage, isFetchingNextPage } = useOfficialChanges();
  const setEnabled = useSetOfficialEnabled();

  const [adding, setAdding] = useState(false);
  const [editingRoles, setEditingRoles] = useState<OfficialDto | null>(null);
  const [settingPassword, setSettingPassword] = useState<OfficialDto | null>(null);
  const [disabling, setDisabling] = useState<OfficialDto | null>(null);

  useHelpContent(OfficialsHelp);

  function enable(official: OfficialDto) {
    setEnabled.mutate({ id: official.id, enabled: true }, {
      onSuccess: () => toast.success(`${official.firstName} ${official.lastName} can sign in again`),
      onError: err => toast.error(getApiErrorMessage(err, 'Could not enable the official. Try again.')),
    });
  }

  return (
    <div className="max-w-4xl space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Officials</h1>
          <p className="text-sm text-muted-foreground mt-1">
            The people who sign in to RCTC. Competitors and spectators never need an account.
          </p>
        </div>
        <Button onClick={() => setAdding(true)}>
          <Plus className="h-4 w-4" aria-hidden="true" />
          Add official
        </Button>
      </div>

      {isLoading && (
        <div className="flex items-center gap-2 py-8">
          <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
          <span className="text-sm text-muted-foreground">Loading officials…</span>
        </div>
      )}
      {isError && <p className="text-sm text-destructive">Could not load the officials. Try again.</p>}

      {officials && (
        <Table aria-label="Officials">
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Roles</TableHead>
              <TableHead>Sign-in</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {officials.map(official => {
              const isMe = user?.id === String(official.id);
              const name = `${official.firstName} ${official.lastName}`;
              return (
                <TableRow key={official.id} className={official.enabled ? undefined : 'text-muted-foreground'}>
                  <TableCell>
                    <div className="font-medium">{name}{isMe && <span className="text-muted-foreground font-normal"> (you)</span>}</div>
                    <div className="text-xs text-muted-foreground">{official.email}</div>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {official.roles.map(role => (
                        <Badge key={role} variant={role === 'ADMIN' ? 'default' : 'secondary'}>{ROLE_LABEL[role]}</Badge>
                      ))}
                    </div>
                  </TableCell>
                  <TableCell>
                    {official.enabled ? (
                      <span className="text-sm">Can sign in</span>
                    ) : (
                      <span className="text-sm">
                        Disabled{official.disabledAt && ` ${formatDate(official.disabledAt)}`}
                      </span>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="sm" onClick={() => setEditingRoles(official)}
                        aria-label={`Change roles for ${name}`}>
                        <ShieldCheck className="h-4 w-4" aria-hidden="true" />
                        Roles
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => setSettingPassword(official)}
                        aria-label={`Set a new password for ${name}`}>
                        <KeyRound className="h-4 w-4" aria-hidden="true" />
                        Password
                      </Button>
                      {official.enabled ? (
                        <Button variant="ghost" size="sm" onClick={() => setDisabling(official)} disabled={isMe}
                          aria-label={`Disable ${name}`} title={isMe ? "You can't disable yourself" : undefined}>
                          <UserX className="h-4 w-4" aria-hidden="true" />
                          Disable
                        </Button>
                      ) : (
                        <Button variant="ghost" size="sm" onClick={() => enable(official)} disabled={setEnabled.isPending}
                          aria-label={`Enable ${name}`}>
                          <UserCheck className="h-4 w-4" aria-hidden="true" />
                          Enable
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}

      {changes && changes.length > 0 && (
        <section className="space-y-2">
          <h2 className="text-lg font-semibold">Recent changes</h2>
          <ul className="divide-y rounded-lg border text-sm" aria-label="Recent changes">
            {changes.map(change => (
              <li key={change.id} className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 px-4 py-2">
                <span>
                  <span className="font-medium">{ACTION_LABEL[change.action]}</span>
                  {': '}{change.officialName}
                  {change.detail && <span className="text-muted-foreground"> ({change.detail})</span>}
                </span>
                <span className="text-xs text-muted-foreground">
                  {change.actorName ? `by ${change.actorName}` : 'from the command line'}
                  {' · '}{formatDateTime(change.at)}
                </span>
              </li>
            ))}
          </ul>
          {hasNextPage && (
            <Button variant="outline" size="sm" onClick={() => fetchNextPage()} disabled={isFetchingNextPage}>
              {isFetchingNextPage ? 'Loading…' : 'Show older changes'}
            </Button>
          )}
        </section>
      )}

      <AddOfficialDialog open={adding} onOpenChange={setAdding} />
      {editingRoles && <RolesDialog official={editingRoles} onClose={() => setEditingRoles(null)} />}
      {settingPassword && <PasswordDialog official={settingPassword} onClose={() => setSettingPassword(null)} />}
      {disabling && <DisableDialog official={disabling} onClose={() => setDisabling(null)} />}
    </div>
  );
}
