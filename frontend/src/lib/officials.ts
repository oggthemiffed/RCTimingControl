import { z } from 'zod';

import type { OfficialRole } from '@/lib/adminApi';

/** Matches OfficialService.MIN_PASSWORD_LENGTH on the server. */
export const MIN_PASSWORD_LENGTH = 8;

export const PASSWORDS_DIFFER = "The passwords don't match.";

const ROLES = ['ADMIN', 'RACE_DIRECTOR', 'REFEREE'] as const satisfies readonly OfficialRole[];

export const passwordRule = z
  .string()
  .min(MIN_PASSWORD_LENGTH, `Password must be at least ${MIN_PASSWORD_LENGTH} characters`)
  .refine(p => p.trim().length > 0, 'Password cannot be only spaces');

/** A new official's name, email, password and roles, as the officials page and the setup wizard both take them. */
export const officialSchema = z.object({
  firstName: z.string().trim().min(1, 'First name required').max(100),
  lastName: z.string().trim().min(1, 'Last name required').max(100),
  // The server accepts any address with an @, including club-network ones such as ref@club
  email: z.string().trim().regex(/^[^\s@]+@[^\s@]+$/, 'Valid email required'),
  password: passwordRule,
  roles: z.array(z.enum(ROLES)).min(1, 'Select at least one role'),
});

export type OfficialFormValues = z.infer<typeof officialSchema>;
