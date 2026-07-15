#!/usr/bin/env node
import { render } from 'ink';
import { App } from './components/App.tsx';

if (!process.stdin.isTTY) {
  console.error('ERROR: Jobs Telescope requires an interactive terminal (TTY).');
  console.error('Please run this command from a terminal, not a pipe or redirect.');
  process.exit(1);
}

const jarPath = process.argv[2] || 'core/target/JobsTelescope-0.0.1-SNAPSHOT.jar';

render(<App jarPath={jarPath} />);
