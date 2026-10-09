import { z } from 'zod';

/** A whole number above zero, or null when the field is left empty. */
export const optionalPositiveInt = z.preprocess(
  (v) => (v === '' || v == null) ? null : Number(v),
  z.number().int().positive().nullable()
);

/** Best X from Y needs both numbers or neither, and cannot count more rounds than there are. */
export function refineBestXFromY(
  data: { bestXFromYX: number | null; bestXFromYY: number | null },
  ctx: z.RefinementCtx
) {
  const bothSet = data.bestXFromYX !== null && data.bestXFromYY !== null;
  const neitherSet = data.bestXFromYX === null && data.bestXFromYY === null;
  if (!bothSet && !neitherSet) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: 'Set both fields or leave both empty',
      path: ['bestXFromYX'],
    });
  }
  if (bothSet && data.bestXFromYX! > data.bestXFromYY!) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: 'Rounds to count cannot exceed total rounds',
      path: ['bestXFromYX'],
    });
  }
}
