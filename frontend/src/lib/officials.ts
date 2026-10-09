import { z } from 'zod';

export const MIN_PASSWORD_LENGTH = 8;

/** A new official's name, email, password and roles, as the officials page and the setup wizard both take them. */
export const officialSchema = z.object({
  firstName: z.string().trim().min(1, 'First name required').max(100),
  lastName: z.string().trim().min(1, 'Last name required').max(100),
  email: z.string().trim().email('Valid email required'),
  password: z.string().min(MIN_PASSWORD_LENGTH, `Password must be at least ${MIN_PASSWORD_LENGTH} characters`),
  roles: z.array(z.enum(['ADMIN', 'RACE_DIRECTOR', 'REFEREE'])).min(1, 'Select at least one role'),
});

export type OfficialFormValues = z.infer<typeof officialSchema>;
