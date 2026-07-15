import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process';
import { createInterface } from 'node:readline';
import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { execSync } from 'node:child_process';
import type { CompletedEvent, CoreEvent, ErrorEvent, ProgressEvent } from '../lib/types.ts';

const PROJECT_ROOT = resolve(__dirname, '../../..');
const JAR_FILE = resolve(PROJECT_ROOT, 'target/JobsTelescope-0.0.1-SNAPSHOT.jar');
const STARTUP_TIMEOUT_MS = 30_000;
const EVENT_TIMEOUT_MS = 15_000;

function buildJarIfNeeded(): boolean {
  if (existsSync(JAR_FILE)) return true;

  try {
    const mvnwPath = resolve(PROJECT_ROOT, 'mvnw');
    if (existsSync(mvnwPath)) {
      execSync(`${mvnwPath} clean package -DskipTests -q`, {
        cwd: PROJECT_ROOT,
        timeout: 180_000,
      });
    } else {
      execSync('mvn clean package -DskipTests -q', {
        cwd: PROJECT_ROOT,
        timeout: 180_000,
      });
    }
    return existsSync(JAR_FILE);
  } catch {
    return false;
  }
}

function waitForEvent(
  events: CoreEvent[],
  predicate: (e: CoreEvent) => boolean,
  timeoutMs: number
): Promise<CoreEvent | null> {
  const start = Date.now();
  return new Promise((resolve) => {
    function check() {
      const match = events.find(predicate);
      if (match) {
        resolve(match);
        return;
      }
      if (Date.now() - start > timeoutMs) {
        resolve(null);
        return;
      }
      setImmediate(check);
    }
    check();
  });
}

function sendCommand(
  process: ChildProcessWithoutNullStreams,
  command: Record<string, unknown>
) {
  process.stdin!.write(`${JSON.stringify(command)}\n`);
}

describe('Core Integration', () => {
  let javaProcess: ChildProcessWithoutNullStreams | null = null;
  let events: CoreEvent[] = [];
  let jarAvailable = false;

  beforeAll(async () => {
    jarAvailable = buildJarIfNeeded();
    if (!jarAvailable) return;

    javaProcess = spawn('java', ['-jar', JAR_FILE], {
      stdio: ['pipe', 'pipe', 'pipe'],
      cwd: PROJECT_ROOT,
    });

    const rl = createInterface({ input: javaProcess.stdout! });

    rl.on('line', (line: string) => {
      try {
        const event = JSON.parse(line.trim()) as CoreEvent;
        if (
          event.type === 'progress' ||
          event.type === 'completed' ||
          event.type === 'error'
        ) {
          events.push(event);
        }
      } catch {
        // skip non-JSON lines (Spring Boot logs)
      }
    });

    javaProcess.on('exit', (code: number | null) => {
      if (code !== 0) {
        events.push({
          type: 'error',
          message: `Core exited with code ${code}`,
        });
      }
    });

    const ready = await new Promise<boolean>((resolve) => {
      const timer = setTimeout(() => resolve(false), STARTUP_TIMEOUT_MS);
      const check = (line: string) => {
        if (line.includes('Waiting for commands on STDIN')) {
          clearTimeout(timer);
          resolve(true);
        }
      };
      const startupRl = createInterface({ input: javaProcess.stdout! });
      startupRl.on('line', (line) => {
        check(line);
      });
    });

    if (!ready) {
      javaProcess.kill();
      javaProcess = null;
    }
  }, STARTUP_TIMEOUT_MS + 10_000);

  afterAll(() => {
    if (javaProcess) {
      javaProcess.stdin?.end();
      javaProcess.kill();
    }
  });

  it('should build or find the JAR', () => {
    expect(jarAvailable).toBe(true);
  });

  it('should start the Core and wait for commands', () => {
    expect(javaProcess).not.toBeNull();
    expect(javaProcess?.exitCode).toBeNull();
    expect(javaProcess?.stdin).not.toBeNull();
    expect(javaProcess?.stdout).not.toBeNull();
  });

  it('should process search commands and return progress events', async () => {
    expect(javaProcess).not.toBeNull();

    sendCommand(javaProcess!, {
      command: 'search',
      query: 'Java',
      region: 'BRAZIL',
    });

    const progress = await waitForEvent(
      events,
      (e) => e.type === 'progress',
      EVENT_TIMEOUT_MS
    );
    expect(progress).not.toBeNull();

    const progressEvent = progress as ProgressEvent;
    expect(typeof progressEvent.platform).toBe('string');
    expect(progressEvent.platform.length).toBeGreaterThan(0);
    expect(typeof progressEvent.percentage).toBe('number');
    expect(progressEvent.percentage).toBeGreaterThan(0);
    expect(progressEvent.percentage).toBeLessThanOrEqual(100);
  });

  it('should return a completed event with jobs found after search', async () => {
    expect(javaProcess).not.toBeNull();

    const existingCompleted = events.find((e) => e.type === 'completed');

    if (!existingCompleted) {
      sendCommand(javaProcess!, {
        command: 'search',
        query: 'Engineer',
        region: 'INTERNATIONAL',
      });
    }

    const completed = await waitForEvent(
      events,
      (e) => e.type === 'completed',
      EVENT_TIMEOUT_MS
    );

    expect(completed).not.toBeNull();

    const completedEvent = completed as CompletedEvent;
    expect(typeof completedEvent.jobsFound).toBe('number');
    expect(completedEvent.jobsFound).toBeGreaterThanOrEqual(0);
    expect(typeof completedEvent.output).toBe('string');
    expect(completedEvent.output.length).toBeGreaterThan(0);
  });

  it('should get an error event for invalid region', async () => {
    expect(javaProcess).not.toBeNull();

    sendCommand(javaProcess!, {
      command: 'search',
      query: 'Dev',
      region: 'INVALID_REGION',
    });

    const error = await waitForEvent(
      events,
      (e) => e.type === 'error',
      EVENT_TIMEOUT_MS
    );

    expect(error).not.toBeNull();

    const errorEvent = error as ErrorEvent;
    expect(typeof errorEvent.message).toBe('string');
    expect(errorEvent.message.length).toBeGreaterThan(0);
  });

  it('should handle multiple searches sequentially', async () => {
    expect(javaProcess).not.toBeNull();

    const initialEventCount = events.length;

    sendCommand(javaProcess!, {
      command: 'search',
      query: 'Python',
      region: 'BRAZIL',
    });

    const completed = await waitForEvent(
      events,
      (e) => e.type === 'completed' && events.indexOf(e) >= initialEventCount,
      EVENT_TIMEOUT_MS
    );

    expect(completed).not.toBeNull();

    const progressCount = events.filter(
      (e) => e.type === 'progress' && events.indexOf(e) >= initialEventCount
    ).length;
    expect(progressCount).toBeGreaterThan(0);
  });
});
