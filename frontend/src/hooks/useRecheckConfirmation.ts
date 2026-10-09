import { useEffect } from 'react';
import { useWatch } from 'react-hook-form';
import type { FieldPath, FieldValues, UseFormReturn } from 'react-hook-form';

/**
 * Checks a "type it again" field again whenever the field it repeats changes, once someone has typed in it, so its
 * "don't match" message comes and goes with either box. React Hook Form only re-checks the field being edited.
 */
export function useRecheckConfirmation<T extends FieldValues>(
  form: UseFormReturn<T>,
  original: FieldPath<T>,
  confirmation: FieldPath<T>,
) {
  const value = useWatch({ control: form.control, name: original });
  useEffect(() => {
    if (form.getFieldState(confirmation).isDirty) void form.trigger(confirmation);
  }, [value, form, confirmation]);
}
