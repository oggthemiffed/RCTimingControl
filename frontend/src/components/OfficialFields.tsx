import type { ReactNode } from 'react';
import { Controller } from 'react-hook-form';
import type { Control, FieldPath, FieldValues } from 'react-hook-form';

import { TextField } from '@/components/TextField';
import { OfficialRoleCheckboxes } from '@/components/OfficialRoleCheckboxes';
import { MIN_PASSWORD_LENGTH } from '@/lib/officials';
import type { OfficialFormValues } from '@/lib/officials';

/** The fields of officialSchema, for any form whose values include them. */
export function OfficialFields<T extends FieldValues & OfficialFormValues>({ control, rolesIdPrefix, afterPassword }: {
  control: Control<T>;
  rolesIdPrefix: string;
  /** Fields that belong with the password, such as typing it again. */
  afterPassword?: ReactNode;
}) {
  // FieldPath<T> can't see that T has these keys, so name them once here
  const field = (name: keyof OfficialFormValues) => name as FieldPath<T>;
  return (
    <>
      <div className="grid grid-cols-2 gap-3">
        <TextField control={control} name={field('firstName')} label="First name" autoComplete="given-name" />
        <TextField control={control} name={field('lastName')} label="Last name" autoComplete="family-name" />
      </div>
      <TextField control={control} name={field('email')} label="Email" type="email" autoComplete="email" />
      <TextField
        control={control}
        name={field('password')}
        label="Password"
        type="password"
        autoComplete="new-password"
        description={`At least ${MIN_PASSWORD_LENGTH} characters.`}
      />
      {afterPassword}
      <Controller
        control={control}
        name={field('roles')}
        render={({ field: roles, fieldState }) => (
          <OfficialRoleCheckboxes
            idPrefix={rolesIdPrefix}
            value={roles.value}
            onChange={roles.onChange}
            error={fieldState.error?.message}
          />
        )}
      />
    </>
  );
}
