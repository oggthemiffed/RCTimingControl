import { describe, it, expect } from 'vitest';
import { z } from 'zod';

import { optionalPositiveInt, refineBestXFromY } from './bestXFromY';

const schema = z.object({ bestXFromYX: optionalPositiveInt, bestXFromYY: optionalPositiveInt })
  .superRefine(refineBestXFromY);

function errorFor(x: unknown, y: unknown) {
  const result = schema.safeParse({ bestXFromYX: x, bestXFromYY: y });
  return result.success ? null : result.error.issues[0].message;
}

describe('best X from Y', () => {
  it('reads empty fields as null', () => {
    expect(schema.parse({ bestXFromYX: '', bestXFromYY: undefined })).toEqual({ bestXFromYX: null, bestXFromYY: null });
  });

  it('accepts both numbers when X is at most Y', () => {
    expect(schema.parse({ bestXFromYX: '4', bestXFromYY: '6' })).toEqual({ bestXFromYX: 4, bestXFromYY: 6 });
  });

  it('needs both numbers or neither', () => {
    expect(errorFor('4', '')).toBe('Set both fields or leave both empty');
  });

  it('cannot count more rounds than there are', () => {
    expect(errorFor('7', '6')).toBe('Rounds to count cannot exceed total rounds');
  });

  it('refuses zero', () => {
    expect(errorFor('0', '6')).not.toBeNull();
  });
});
