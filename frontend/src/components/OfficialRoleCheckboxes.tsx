import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import type { OfficialRole } from '@/lib/adminApi';

const ROLES: { role: OfficialRole; label: string; description: string }[] = [
  { role: 'ADMIN', label: 'Admin', description: 'Club set-up, events, entries, officials and backups' },
  { role: 'RACE_DIRECTOR', label: 'Race director', description: 'Race control: grid, start, stop, marshal laps' },
  { role: 'REFEREE', label: 'Referee', description: 'Penalties, incidents and unknown transponders' },
];

/** Ticks for an official's roles, each with what it lets them do. */
export function OfficialRoleCheckboxes({
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
