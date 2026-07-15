import type { ChildProcessWithoutNullStreams } from 'node:child_process';
import { createInterface, type ReadLine } from 'node:readline';
import type { CoreEvent, SearchCommand } from './types.ts';

export class CoreClient {
  private process: ChildProcessWithoutNullStreams | null = null;
  private rl: ReadLine | null = null;
  private onEvent: ((event: CoreEvent) => void) | null = null;

  async start(jarPath: string): Promise<void> {
    const { spawn } = await import('node:child_process');
    this.process = spawn('java', ['-jar', jarPath], {
      stdio: ['pipe', 'pipe', 'pipe'],
    });

    if (!this.process?.stdout) {
      throw new Error('Core process has no stdout');
    }
    this.rl = createInterface({ input: this.process.stdout });

    this.rl.on('line', (line: string) => {
      try {
        const event = JSON.parse(line.trim()) as CoreEvent;
        this.onEvent?.(event);
      } catch {
        // ignore malformed lines
      }
    });

    this.process.stderr?.on('data', (data: Buffer) => {
      // redirect logs from Core to stderr of UI for debugging
      process.stderr.write(data);
    });

    this.process.on('exit', (code: number | null) => {
      if (code !== 0) {
        this.onEvent?.({ type: 'error', message: `Core exited with code ${code}` });
      }
    });
  }

  sendCommand(command: SearchCommand): void {
    if (!this.process?.stdin) {
      this.onEvent?.({ type: 'error', message: 'Core process not available' });
      return;
    }
    this.process.stdin.write(`${JSON.stringify(command)}\n`);
  }

  setEventListener(callback: (event: CoreEvent) => void): void {
    this.onEvent = callback;
  }

  close(): void {
    this.rl?.close();
    this.process?.stdin?.end();
    this.process?.kill();
  }
}
