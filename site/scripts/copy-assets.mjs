// Copies the heavy media (videos + screenshots) from the repository docs folder
// into the Vite `public/` directory so a single build embeds them. The copied
// folders are git-ignored; the canonical sources stay in /docs.
import { cp, rm, mkdir } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '..', '..');
const publicDir = resolve(here, '..', 'public');

const assets = [
  { from: resolve(repoRoot, 'docs/videos/per-app'), to: resolve(publicDir, 'videos/per-app') },
  { from: resolve(repoRoot, 'docs/videos/final'), to: resolve(publicDir, 'videos/final') },
  { from: resolve(repoRoot, 'docs/screenshots'), to: resolve(publicDir, 'screenshots') },
];

await mkdir(publicDir, { recursive: true });

for (const { from, to } of assets) {
  if (!existsSync(from)) {
    console.warn(`[sync-assets] missing source, skipping: ${from}`);
    continue;
  }
  await rm(to, { recursive: true, force: true });
  await mkdir(dirname(to), { recursive: true });
  await cp(from, to, { recursive: true });
  console.log(`[sync-assets] copied ${from} -> ${to}`);
}
