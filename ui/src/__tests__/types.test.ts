import { describe, expect, it } from 'vitest';
import type { CoreEvent } from '../lib/types.ts';

describe('CoreEvent types', () => {
  it('should match progress event format', () => {
    const event: CoreEvent = {
      type: 'progress',
      platform: 'gupy',
      percentage: 50,
    };
    expect(event.type).toBe('progress');
    expect(event.percentage).toBe(50);
  });

  it('should match completed event format', () => {
    const event: CoreEvent = {
      type: 'completed',
      jobsFound: 42,
      output: 'output/2026-07-14',
    };
    expect(event.type).toBe('completed');
  });

  it('should match error event format', () => {
    const event: CoreEvent = {
      type: 'error',
      message: 'Something failed',
    };
    expect(event.type).toBe('error');
    expect(event.message).toBe('Something failed');
  });
});
