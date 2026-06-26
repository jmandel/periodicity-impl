#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { mkdirSync, readFileSync, renameSync, rmSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(new URL('../../..', import.meta.url).pathname);
const require = createRequire(import.meta.url);
const { chromium } = require(path.join(root, 'ovumcy/node_modules/playwright'));

const rawDir = path.join(root, 'docs/videos/_work/raw');
const workDir = path.join(root, 'docs/videos/_work/viewer');
const videoDir = path.join(workDir, 'playwright');
mkdirSync(rawDir, { recursive: true });
mkdirSync(workDir, { recursive: true });
mkdirSync(videoDir, { recursive: true });

function run(command, args, options = {}) {
  return execFileSync(command, args, {
    cwd: root,
    encoding: options.encoding || 'utf8',
    stdio: options.stdio || ['ignore', 'pipe', 'pipe'],
  });
}

function ffprobeDuration(file) {
  return Number(run('ffprobe', [
    '-v',
    'error',
    '-show_entries',
    'format=duration',
    '-of',
    'default=nw=1:nk=1',
    file,
  ]).trim());
}

function normalizeViewerURL(value) {
  const text = String(value || '').trim();
  if (!text) throw new Error('QR decode returned an empty value');
  if (text.startsWith('shlink:/')) {
    return `https://cycle.fhir.me/view#${text}`;
  }
  if (text.includes('#shlink:/') || text.includes('shlink:/')) {
    return text;
  }
  throw new Error(`QR did not contain an SHLink: ${text.slice(0, 120)}`);
}

function resolveViewerURL(source, appID) {
  const text = String(source || '').trim();
  if (text.startsWith('http://') || text.startsWith('https://') || text.startsWith('shlink:/')) {
    return normalizeViewerURL(text);
  }

  const sourcePath = path.resolve(root, source);
  if (path.extname(sourcePath).toLowerCase() === '.txt') {
    return normalizeViewerURL(readFileSync(sourcePath, 'utf8'));
  }

  return decodeFromVideo(sourcePath, appID);
}

function decodeFromImage(image) {
  try {
    const out = run('zbarimg', ['--quiet', '--raw', image]).trim();
    return out
      .split(/\r?\n/)
      .map((line) => line.trim())
      .find((line) => line.includes('shlink:/')) || '';
  } catch {
    return '';
  }
}

function decodeFromVideo(video, appID) {
  const duration = ffprobeDuration(video);
  const samples = [
    duration - 0.5,
    duration - 1.5,
    duration - 3,
    duration * 0.8,
    duration * 0.65,
    duration * 0.5,
  ].filter((value) => value > 0.2);

  for (const [index, timestamp] of samples.entries()) {
    const frame = path.join(workDir, `${appID}-qr-${index}.png`);
    rmSync(frame, { force: true });
    run('ffmpeg', [
      '-y',
      '-loglevel',
      'error',
      '-ss',
      timestamp.toFixed(3),
      '-i',
      video,
      '-frames:v',
      '1',
      frame,
    ]);
    const decoded = decodeFromImage(frame);
    if (decoded) return normalizeViewerURL(decoded);
  }
  throw new Error(`Could not decode an SHLink QR from ${video}`);
}

async function recordViewer(appID, viewerURL) {
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.CHROMIUM_PATH || '/usr/bin/chromium',
  });
  const context = await browser.newContext({
    viewport: { width: 1366, height: 900 },
    recordVideo: {
      dir: videoDir,
      size: { width: 1366, height: 900 },
    },
  });
  const page = await context.newPage();
  await page.goto(viewerURL, { waitUntil: 'domcontentloaded', timeout: 60000 });
  await page.waitForLoadState('networkidle', { timeout: 60000 }).catch(() => {});
  await page.getByRole('button', { name: /open link/i }).click({ timeout: 6000 }).catch(() => {});
  await page.waitForLoadState('networkidle', { timeout: 60000 }).catch(() => {});
  await page.waitForTimeout(9000);

  const video = page.video();
  await context.close();
  await browser.close();
  const webm = await video.path();
  const rawWebm = path.join(rawDir, `${appID}-viewer-chromium.webm`);
  const mp4 = path.join(rawDir, `${appID}-viewer-chromium.mp4`);
  renameSync(webm, rawWebm);
  rmSync(mp4, { force: true });
  run('ffmpeg', [
    '-y',
    '-loglevel',
    'error',
    '-i',
    rawWebm,
    '-an',
    '-vf',
    'fps=30,format=yuv420p',
    '-c:v',
    'libx264',
    '-preset',
    'medium',
    '-crf',
    '20',
    '-movflags',
    '+faststart',
    mp4,
  ]);
  return mp4;
}

async function main() {
  const [appID, sourceVideoArg] = process.argv.slice(2);
  if (!appID || !sourceVideoArg) {
    throw new Error('Usage: record-chromium-viewer.mjs <app-id> <source-video|viewer-url|url-file>');
  }
  const viewerURL = resolveViewerURL(sourceVideoArg, appID);
  writeFileSync(path.join(workDir, `${appID}-viewer-url.txt`), `${viewerURL}\n`);
  const out = await recordViewer(appID, viewerURL);
  console.log(out);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
