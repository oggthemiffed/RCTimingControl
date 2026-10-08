import { useState } from 'react';
import { KeyRound, Loader2, Plus, ShieldCheck, UserCheck, UserX } from 'lucide-react';
import { toast } from 'sonner';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
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
import type { OfficialAction, OfficialDto, OfficialRole } from '@/lib/adminApi';
import { getApiErrorMessage } from '@/lib/errors';
import { formatDate, formatDateTime } from '@/lib/dates';

const MIN_PASSWORD_LENGTH = 8;

const ROLES: { role: OfficialRole; label: string; description: string }[] = [
  { role: 'ADMIN', label: 'Admin', description: 'Club set-up, events, entries, officials and backups' },
  { role: 'RACE_DIRECTOR', label: 'Race director', description: 'Race control: grid, start, stop, marshal laps' },
  { role: 'REFEREE', label: 'Referee', description: 'Penalties, incidents and unknown transponders' },
];

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

function RoleCheckboxes({
  idPrefix,
  value,
  onChange,
}: {
  idPrefix: string;
  value: OfficialRole[];
  onChange: (roles: OfficialRole[]) => void;
}) {
  return (
    <fieldset className="space-y-2">
      <legend className="text-sm font-medium mb-1">Roles</legend>
      {ROLES.map(({ role, label, description }) => (
        <div key={role} className="flex items-start gap-2">
          <Checkbox
            id={`${idPrefix}-${role}`}
            checked={value.includes(role)}
            onCheckedChange={checked =>
              onChange(checked ? [...value, role] : value.filter(r => r !== role))
            }
          />
          <div className="grid gap-0.5 leading-none">
            <Label htmlFor={`${idPrefix}-${role}`}>{label}</Label>
            <span className="text-xs text-muted-foreground">{description}</span>
          </div>
        </div>
      ))}
    </fieldset>
  );
}

function AddOfficialDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const addOfficial = useAddOfficial();
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [roles, setRoles] = useState<OfficialRole[]>(['RACE_DIRECTOR']);

  function close(next: boolean) {
    onOpenChange(next);
    if (!next) {
      setFirstName('');
      setLastName('');
      setEmail('');
      setPassword('');
      setRoles(['RACE_DIRECTOR']);
    }
  }

  const valid = firstName.trim() && lastName.trim() && email.includes('@')
    && password.length >= MIN_PASSWORD_LENGTH && roles.length > 0;

  function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!valid) return;
    addOfficial.mutate(
      { firstName: firstName.trim(), lastName: lastName.trim(), email: email.trim(), password, roles },
      {
        onSuccess: official => {
          toast.success(`Added ${official.firstName} ${official.lastName}`);
          close(false);
        },
        onError: err => toast.error(getApiErrorMessage(err, 'Could not add the official. Try again.')),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add an official</DialogTitle>
          <DialogDescription>Give them their password in person. They can sign in straight away.</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="official-first-name">First name</Label>
              <Input id="official-first-name" value={firstName} onChange={e => setFirstName(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="official-last-name">Last name</Label>
              <Input id="official-last-name" value={lastName} onChange={e => setLastName(e.target.value)} />
            </div>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="official-email">Email</Label>
            <Input id="official-email" type="email" value={email} onChange={e => setEmail(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="official-password">Password</Label>
            <Input id="official-password" type="password" autoComplete="new-password" value={password}
              onChange={e => setPassword(e.target.value)} />
            <p className="text-xs text-muted-foreground">At least {MIN_PASSWORD_LENGTH} characters.</p>
          </div>
          <RoleCheckboxes idPrefix="add-role" value={roles} onChange={setRoles} />
          <DialogFooter>
            <Button type="submit" disabled={!valid || addOfficial.isPending}>
              {addOfficial.isPending && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
              Add official
            </Button>
          </DialogFooter>
        </form>
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
          <RoleCheckboxes idPrefix="edit-role" value={roles} onChange={setRoles} />
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

function PasswordDialog({ official, onClose }: { official: OfficialDto; onClose: () => void }) {
  const setOfficialPassword = useSetOfficialPassword();
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const mismatch = confirm.length > 0 && password !== confirm;
  const valid = password.length >= MIN_PASSWORD_LENGTH && password === confirm;

  function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!valid) return;
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
        <form onSubmit={submit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="new-password">New password</Label>
            <Input id="new-password" type="password" autoComplete="new-password" value={password}
              onChange={e => setPassword(e.target.value)} />
            <p className="text-xs text-muted-foreground">At least {MIN_PASSWORD_LENGTH} characters.</p>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="confirm-password">Type it again</Label>
            <Input id="confirm-password" type="password" autoComplete="new-password" value={confirm}
              onChange={e => setConfirm(e.target.value)} />
            {mismatch && <p className="text-xs text-destructive">The passwords don&apos;t match.</p>}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={!valid || setOfficialPassword.isPending}>
              {setOfficialPassword.isPending && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
              Set password
            </Button>
          </DialogFooter>
        </form>
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
