import type { ComponentProps } from 'react';
import type { Control, FieldPath, FieldValues } from 'react-hook-form';

import { FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';

type TextFieldProps<T extends FieldValues> = {
  control: Control<T>;
  name: FieldPath<T>;
  label: string;
  /** A hint under the input, such as a minimum length. */
  description?: string;
} & Omit<
  ComponentProps<typeof Input>,
  // FormControl sets the id and aria attributes that tie the input to its label and message
  'name' | 'value' | 'onChange' | 'onBlur' | 'ref' | 'id' | 'aria-describedby' | 'aria-invalid'
>;

/**
 * A labelled text input bound to a React Hook Form field, with its validation message underneath. The field's own
 * props go last, so a form or field set disabled in React Hook Form wins over a disabled prop here.
 */
export function TextField<T extends FieldValues>({ control, name, label, description, ...inputProps }: TextFieldProps<T>) {
  return (
    <FormField
      control={control}
      name={name}
      render={({ field }) => (
        <FormItem>
          <FormLabel>{label}</FormLabel>
          <FormControl>
            <Input {...inputProps} {...field} />
          </FormControl>
          {description && <FormDescription className="text-xs">{description}</FormDescription>}
          <FormMessage />
        </FormItem>
      )}
    />
  );
}
