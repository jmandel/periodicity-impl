#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { copyFileSync, mkdirSync, renameSync, rmSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(new URL('../../..', import.meta.url).pathname);
const require = createRequire(import.meta.url);
const { chromium } = require(path.join(root, 'ovumcy/node_modules/playwright'));

const baseURL = process.env.OVUMCY_BASE_URL || 'http://127.0.0.1:18080';
const rawDir = path.join(root, 'docs/videos/_work/raw');
const workDir = path.join(root, 'docs/videos/_work/ovumcy-live');
const videoDir = path.join(workDir, 'playwright');
mkdirSync(rawDir, { recursive: true });
mkdirSync(workDir, { recursive: true });
mkdirSync(videoDir, { recursive: true });

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function run(command, args) {
  execFileSync(command, args, {
    cwd: root,
    stdio: 'inherit',
  });
}

function ffprobeDuration(file) {
  const out = execFileSync('ffprobe', [
    '-v',
    'error',
    '-show_entries',
    'format=duration',
    '-of',
    'default=nw=1:nk=1',
    file,
  ], { encoding: 'utf8' }).trim();
  return Number(out);
}

function splitISODate(isoDate) {
  const [year, month, day] = isoDate.split('-');
  return { year, month, day };
}

async function requestSubmit(locator) {
  await locator.evaluate((element) => {
    if (!(element instanceof HTMLFormElement)) {
      throw new Error('Expected a form');
    }
    element.requestSubmit();
  });
}

async function fillDateField(page, selector, isoDate) {
  const { year, month, day } = splitISODate(isoDate);
  const field = page.locator(selector);
  const rootField = field.locator('xpath=ancestor-or-self::*[@data-date-field][1]');
  await rootField.locator('[data-date-field-part="day"]').fill(day);
  await rootField.locator('[data-date-field-part="month"]').fill(month);
  await rootField.locator('[data-date-field-part="year"]').fill(year);
  await rootField.locator('[data-date-field-part="year"]').blur();
}

async function registerAndOnboard(page) {
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
  const email = `cycle-video-${suffix}@example.com`;

  await page.goto(`${baseURL}/register`, { waitUntil: 'domcontentloaded' });
  await page.locator('#register-email').fill(email);
  await page.locator('#register-password').fill('StrongPass1');
  await page.locator('#register-confirm-password').fill('StrongPass1');
  await page.locator('#register-consent').check();
  await requestSubmit(page.locator('form[action="/api/v1/users"]'));

  await page.locator('[data-auth-inline-recovery]').waitFor({ timeout: 15000 });
  await page.locator('[data-recovery-code-checkbox]').check();
  await page.locator('[data-recovery-code-submit]').click();
  await page.waitForURL((url) => ['/onboarding', '/dashboard'].includes(url.pathname), { timeout: 15000 });

  if (new URL(page.url()).pathname !== '/onboarding') {
    return;
  }

  await fillDateField(page, '#last-period-start', '2026-06-22');
  await page.locator('form[hx-post="/api/v1/onboarding/steps/1"] button[type="submit"]').click();
  await page.locator('form[hx-post="/api/v1/onboarding/steps/2"]').waitFor({
    state: 'visible',
    timeout: 15000,
  });
  await Promise.all([
    page.waitForURL(/\/dashboard(?:\?.*)?$/, { timeout: 15000 }),
    page.locator('form[hx-post="/api/v1/onboarding/steps/2"] button[type="submit"]').click(),
  ]);
}

async function waitForHTMXResponse(page, fragment, action) {
  await Promise.all([
    page.waitForResponse((response) => response.url().includes(fragment), { timeout: 30000 }),
    action(),
  ]);
}

function trimSegment(source, start, end, out) {
  rmSync(out, { force: true });
  run('ffmpeg', [
    '-y',
    '-ss',
    Math.max(0, start).toFixed(3),
    '-i',
    source,
    '-t',
    Math.max(0.5, end - Math.max(0, start)).toFixed(3),
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
    out,
  ]);
}

function transcodeFull(source, out) {
  rmSync(out, { force: true });
  run('ffmpeg', [
    '-y',
    '-i',
    source,
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
    out,
  ]);
}

async function main() {
  const marks = {};
  let shareID = '';
  let manageToken = '';
  let csrfToken = '';
  let stopped = false;
  let viewerVideoPath = '';

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
  const startedAt = Date.now();
  const elapsed = () => (Date.now() - startedAt) / 1000;

  try {
    await registerAndOnboard(page);

    await page.goto(`${baseURL}/settings`, { waitUntil: 'domcontentloaded' });
    await page.locator('#cycle-ig-share-panel').scrollIntoViewIfNeeded();
    await waitForHTMXResponse(page, '/api/v1/cycle-ig/sample', async () => {
      await page.getByRole('button', { name: 'Load synthetic sample' }).click();
    });
    await delay(1000);

    marks.nativeStart = elapsed();
    await page.goto(`${baseURL}/dashboard`, { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {});
    await delay(3000);
    await page.goto(`${baseURL}/calendar`, { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {});
    await delay(3000);

    marks.shareStart = elapsed();
    await page.goto(`${baseURL}/settings`, { waitUntil: 'domcontentloaded' });
    await page.locator('#cycle-ig-share-panel').scrollIntoViewIfNeeded();
    await delay(1200);

    await waitForHTMXResponse(page, '/api/v1/cycle-ig/preview', async () => {
      await page.getByRole('button', { name: 'Review SMART Link' }).click();
    });
    await page.locator('[data-cycle-ig-result="preview"]').waitFor({ timeout: 15000 });
    await delay(2500);

    await waitForHTMXResponse(page, '/api/v1/cycle-ig/shares', async () => {
      await page.getByRole('button', { name: 'Share with SMART Link' }).click();
    });
    await page.locator('[data-cycle-ig-result="share"]').waitFor({ timeout: 30000 });
    await page.locator('[data-cycle-ig-share-card]').scrollIntoViewIfNeeded();
    marks.shareVisible = elapsed();
    await delay(2600);

    const revokeForm = page.locator('form[hx-post*="/api/v1/cycle-ig/shares/"][hx-post*="/revoke"]');
    const revokePath = await revokeForm.getAttribute('hx-post');
    const idMatch = /\/shares\/([^/]+)\/revoke/.exec(revokePath || '');
    shareID = idMatch?.[1] || '';
    manageToken = await revokeForm.locator('input[name="manage_token"]').inputValue();
    csrfToken = await revokeForm.locator('input[name="csrf_token"]').inputValue();
    const viewerLink = await page.locator('#cycle-ig-viewer-link').inputValue();

    marks.viewerStart = elapsed();
    const viewer = await context.newPage();
    await viewer.goto(viewerLink, { waitUntil: 'domcontentloaded', timeout: 60000 });
    await viewer.waitForLoadState('networkidle', { timeout: 60000 }).catch(() => {});
    await viewer.getByRole('button', { name: /open link/i }).click({ timeout: 6000 }).catch(() => {});
    await viewer.waitForLoadState('networkidle', { timeout: 60000 }).catch(() => {});
    await viewer.locator('body').waitFor({ timeout: 30000 });
    await delay(9000);
    const viewerVideo = viewer.video();
    await viewer.close();
    viewerVideoPath = await viewerVideo.path();
    marks.viewerEnd = elapsed();

    await page.bringToFront();
    await page.locator('[data-cycle-ig-share-card]').scrollIntoViewIfNeeded();
    await delay(1000);
    marks.stopStart = elapsed();
    await waitForHTMXResponse(page, `/api/v1/cycle-ig/shares/${shareID}/revoke`, async () => {
      await page.getByRole('button', { name: 'Stop sharing' }).click();
    });
    await page.locator('[data-cycle-ig-result="stopped"]').waitFor({ timeout: 15000 });
    stopped = true;
    await delay(2500);
    marks.end = elapsed();
  } finally {
    if (shareID && manageToken && csrfToken && !stopped) {
      await page.request.post(`${baseURL}/api/v1/cycle-ig/shares/${shareID}/revoke`, {
        form: {
          csrf_token: csrfToken,
          manage_token: manageToken,
        },
        headers: {
          'HX-Request': 'true',
        },
      }).catch(() => {});
    }
    const originalVideo = page.video();
    await context.close();
    await browser.close();

    const originalPath = await originalVideo.path();
    const originalFull = path.join(rawDir, 'ovumcy-original-full.webm');
    const viewerFull = path.join(rawDir, 'ovumcy-viewer-full.webm');
    renameSync(originalPath, originalFull);
    if (viewerVideoPath) {
      renameSync(viewerVideoPath, viewerFull);
    }

    const marksPath = path.join(workDir, 'recording-marks.json');
    writeFileSync(marksPath, JSON.stringify({
      ...marks,
      originalDuration: ffprobeDuration(originalFull),
      viewerDuration: viewerVideoPath ? ffprobeDuration(viewerFull) : 0,
      shareID,
      stopped,
    }, null, 2));

    trimSegment(originalFull, marks.nativeStart || 0, marks.shareStart || (marks.shareVisible || 0), path.join(rawDir, 'ovumcy-01-native-tour.mp4'));
    trimSegment(originalFull, marks.shareStart || 0, (marks.shareVisible || 0) + 2.5, path.join(rawDir, 'ovumcy-02-share-create.mp4'));
    if (viewerVideoPath) {
      transcodeFull(viewerFull, path.join(rawDir, 'ovumcy-03-viewer.mp4'));
    } else {
      copyFileSync(originalFull, path.join(rawDir, 'ovumcy-03-viewer.mp4'));
    }
    trimSegment(originalFull, (marks.stopStart || marks.end || 1) - 1.2, (marks.end || marks.stopStart || 3) + 0.4, path.join(rawDir, 'ovumcy-04-stop.mp4'));

    console.log(path.join(rawDir, 'ovumcy-01-native-tour.mp4'));
    console.log(path.join(rawDir, 'ovumcy-02-share-create.mp4'));
    console.log(path.join(rawDir, 'ovumcy-03-viewer.mp4'));
    console.log(path.join(rawDir, 'ovumcy-04-stop.mp4'));
    console.log(marksPath);
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
