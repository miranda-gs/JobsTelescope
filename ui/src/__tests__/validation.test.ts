import { describe, expect, it } from 'vitest';
import { validateSearchInput } from '../lib/validation.ts';

describe('validateSearchInput', () => {
  it('should accept valid search input', () => {
    const input = { query: 'Java Backend', region: 'BRAZIL' as const };
    const result = validateSearchInput(input);
    expect(result.query).toBe('Java Backend');
    expect(result.region).toBe('BRAZIL');
  });

  it('should accept input with platforms', () => {
    const input = {
      query: 'Python Dev',
      region: 'INTERNATIONAL' as const,
      platforms: ['REMOTE_OK' as const, 'WELLFOUND' as const],
    };
    const result = validateSearchInput(input);
    expect(result.platforms).toEqual(['REMOTE_OK', 'WELLFOUND']);
  });

  it('should reject empty query', () => {
    expect(() => validateSearchInput({ query: '', region: 'BRAZIL' })).toThrow();
  });

  it('should reject invalid region', () => {
    expect(() => validateSearchInput({ query: 'Test', region: 'INVALID' })).toThrow();
  });
});
