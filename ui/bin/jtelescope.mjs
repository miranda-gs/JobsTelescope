#!/usr/bin/env node
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';

const __dirname = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(__dirname, '..', '..');
const bashWrapper = resolve(projectRoot, 'jtelescope');

const child = spawn(
  '/usr/bin/env',
  ['bash', bashWrapper, ...process.argv.slice(2)],
  { stdio: 'inherit', windowsHide: true }
);

child.on('exit', (code) => process.exit(code ?? 1));
