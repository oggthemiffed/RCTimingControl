import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { cn } from '@/lib/utils';
import type { OfficialRole } from '@/lib/adminApi';

const ROLES: { role: OfficialRole; label: string; description: string }[] = [
  { role: 'ADMIN', label: 'Admin', description: 'Club set-up, events, entries, officials and backups' },
  { role: 'RACE_DIRECTOR', label: 'Race director', description: 'Race control: grid, start, stop, marshal laps, unknown transponders' },
  { role: 'REFEREE', label: 'Referee', description: 'Penalties, incidents and marshal absences' },
];

/** Ticks for an official's roles, each with what it lets them do. */
export function OfficialRoleCheckboxes({
  idPrefix,
  value,
  onChange,
  error,
}: {
  idPrefix: string;
  value: OfficialRole[];
  onChange: (roles: OfficialRole[]) => void;
  /** A validation message, shown under the ticks and announced with them. */
  error?: string;
}) {
  const errorId = `${idPrefix}-error`;
  return (
    <fieldset
      className="space-y-2"
      aria-invalid={error ? true : undefined}
      aria-describedby={error ? errorId : undefined}
    >
      <legend className={cn('text-sm font-medium mb-1', error && 'text-destructive')}>Roles</legend>
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
      {error && <p id={errorId} className="text-sm font-medium text-destructive">{error}</p>}
    </fieldset>
  );
}
