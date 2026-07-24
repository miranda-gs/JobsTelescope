#!/usr/bin/env node
import { render } from 'ink';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { App } from './components/App.tsx';

if (!process.stdin.isTTY) {
  console.error('ERROR: Jobs Telescope requires an interactive terminal (TTY).');
  console.error('Please run this command from a terminal, not a pipe or redirect.');
  process.exit(1);
}

const __dirname = dirname(fileURLToPath(import.meta.url));
const defaultJar = resolve(__dirname, '..', '..', 'target', 'JobsTelescope-0.0.1-SNAPSHOT.jar');
const jarPath = process.argv[2] ? resolve(process.cwd(), process.argv[2]) : defaultJar;

render(<App jarPath={jarPath} />);
