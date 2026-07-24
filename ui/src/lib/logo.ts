import { readFileSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const logoPath = resolve(__dirname, '..', '..', '..', 'logo');

let logoContent = '';
try {
  logoContent = readFileSync(logoPath, 'utf-8');
} catch {
  logoContent = 'Jobs Telescope';
}

export const LOGO = logoContent;
