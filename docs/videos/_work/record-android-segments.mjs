import { spawn } from 'node:child_process';
import { mkdir, rm } from 'node:fs/promises';
import path from 'node:path';

const root = path.resolve(new URL('../../..', import.meta.url).pathname);
const rawDir = path.join(root, 'docs', 'videos', '_work', 'raw');

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: root,
      stdio: options.stdio || 'pipe',
    });
    let stdout = '';
    let stderr = '';
    child.stdout?.on('data', (chunk) => {
      stdout += chunk;
    });
    child.stderr?.on('data', (chunk) => {
      stderr += chunk;
    });
    child.on('error', reject);
    child.on('close', (code) => {
      if (code === 0) {
        resolve({ stdout, stderr });
        return;
      }
      reject(new Error(`${command} ${args.join(' ')} exited ${code}\n${stdout}\n${stderr}`));
    });
  });
}

async function adb(...args) {
  return run('adb', args);
}

function parseBounds(bounds) {
  const match = /^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$/.exec(bounds || '');
  if (!match) return null;
  return {
    x1: Number(match[1]),
    y1: Number(match[2]),
    x2: Number(match[3]),
    y2: Number(match[4]),
  };
}

function decodeXML(value) {
  return String(value || '')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&#10;/g, '\n')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>');
}

async function uiNodes() {
  await adb('shell', 'uiautomator', 'dump', '/sdcard/window.xml');
  const { stdout } = await adb('exec-out', 'cat', '/sdcard/window.xml');
  const nodes = [];
  for (const node of stdout.matchAll(/<node\b[^>]*>/g)) {
    const raw = node[0];
    const text = / text="([^"]*)"/.exec(raw)?.[1] || '';
    const desc = / content-desc="([^"]*)"/.exec(raw)?.[1] || '';
    const bounds = parseBounds(/ bounds="([^"]*)"/.exec(raw)?.[1] || '');
    if (!bounds) continue;
    nodes.push({
      label: decodeXML(text || desc).trim(),
      bounds,
    });
  }
  return nodes;
}

async function tapLabel(pattern, waitMs = 650) {
  const nodes = await uiNodes();
  const matcher = pattern instanceof RegExp ? pattern : new RegExp(String(pattern), 'i');
  const node = nodes.find((candidate) => matcher.test(candidate.label) && isVisible(candidate))
    || nodes.find((candidate) => matcher.test(candidate.label));
  if (!node) {
    throw new Error(`No UI label matching ${matcher}`);
  }
  const x = Math.round((node.bounds.x1 + node.bounds.x2) / 2);
  const y = Math.round((node.bounds.y1 + node.bounds.y2) / 2);
  await tap(x, y, waitMs);
}

async function tapLabelAfterScroll(pattern, waitMs = 650) {
  await scrollToLabel(pattern);
  await tapLabel(pattern, waitMs);
}

function isVisible(node) {
  const { bounds } = node;
  if (bounds.x2 <= bounds.x1 || bounds.y2 <= bounds.y1) {
    return false;
  }
  const centerX = (bounds.x1 + bounds.x2) / 2;
  const centerY = (bounds.y1 + bounds.y2) / 2;
  return centerX >= 0 && centerX <= 1080 && centerY >= 0 && centerY <= 1840;
}

async function hasVisibleLabel(pattern) {
  const matcher = pattern instanceof RegExp ? pattern : new RegExp(String(pattern), 'i');
  const nodes = await uiNodes();
  return nodes.some((candidate) => matcher.test(candidate.label) && isVisible(candidate));
}

async function waitForLabel(pattern, timeoutMs = 10000) {
  const matcher = pattern instanceof RegExp ? pattern : new RegExp(String(pattern), 'i');
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeoutMs) {
    const nodes = await uiNodes();
    if (nodes.some((candidate) => matcher.test(candidate.label))) {
      return;
    }
    await wait(500);
  }
  throw new Error(`No UI label matching ${matcher} after ${timeoutMs}ms`);
}

async function scrollToLabel(pattern) {
  for (let attempt = 0; attempt < 6; attempt += 1) {
    const nodes = await uiNodes();
    const matcher = pattern instanceof RegExp ? pattern : new RegExp(String(pattern), 'i');
    if (nodes.some((candidate) => matcher.test(candidate.label) && isVisible(candidate))) {
      return;
    }
    await swipe(540, 1480, 540, 360, 900, 500);
  }
  throw new Error(`No UI label matching ${pattern} after scrolling`);
}

async function tap(x, y, waitMs = 650) {
  await adb('shell', 'input', 'tap', String(x), String(y));
  await delay(waitMs);
}

async function swipe(x1, y1, x2, y2, duration = 700, waitMs = 650) {
  await adb('shell', 'input', 'swipe', String(x1), String(y1), String(x2), String(y2), String(duration));
  await delay(waitMs);
}

async function wait(ms) {
  await delay(ms);
}

async function forceStop(pkg) {
  await adb('shell', 'am', 'force-stop', pkg);
  await delay(500);
}

async function startActivity(component, waitMs = 2500) {
  await adb('shell', 'am', 'start', '-n', component);
  await delay(waitMs);
}

async function recordSegment(name, actions) {
  await mkdir(rawDir, { recursive: true });
  const remote = `/sdcard/${name}.mp4`;
  const local = path.join(rawDir, `${name}.mp4`);
  await adb('shell', 'rm', '-f', remote).catch(() => {});
  await rm(local, { force: true }).catch(() => {});

  const recorder = spawn('adb', [
    'shell',
    'screenrecord',
    '--bit-rate',
    '8000000',
    '--size',
    '720x1280',
    remote,
  ], { cwd: root, stdio: 'ignore' });

  await delay(1100);
  const recorderPid = (await adb('shell', 'pidof', 'screenrecord').catch(() => ({ stdout: '' }))).stdout.trim().split(/\s+/)[0];
  let actionError = null;
  try {
    await actions();
    await delay(800);
  } catch (error) {
    actionError = error;
  } finally {
    const stopped = new Promise((resolve) => recorder.once('close', resolve));
    if (recorderPid) {
      await adb('shell', 'kill', '-2', recorderPid).catch(async () => {
        await adb('shell', 'pkill', '-2', 'screenrecord').catch(() => {});
      });
    } else {
      await adb('shell', 'pkill', '-2', 'screenrecord').catch(async () => {
        await adb('shell', 'killall', '-2', 'screenrecord').catch(() => {});
      });
    }
    await stopped;
  }
  await delay(1500);
  await adb('pull', remote, local);
  await adb('shell', 'rm', '-f', remote).catch(() => {});
  console.log(local);
  if (actionError) {
    throw actionError;
  }
}

async function drip() {
  await dripCreateOnly();
  await dripStopOnly();
}

async function dripCreateOnly() {
  await preloadDripSampleData();

  await recordSegment('drip-01-native-tour', async () => {
    await forceStop('com.drip');
    await adb('shell', 'input', 'keyevent', 'HOME');
    await startActivity('com.drip/.MainActivity', 3000);
    await tapLabel(/^OK$/, 900).catch(() => {});
    await wait(1800);
    await tapLabel(/^CHART$/, 1800).catch(() => {});
    await tapLabel(/^STATS$/, 1800).catch(() => {});
    await tapLabel(/^CALENDAR$/, 1200).catch(() => {});
  });

  await recordSegment('drip-02-scope-preview', async () => {
    await openDripData();
    await scrollToLabel(/^REVIEW SMART LINK$/);
    await tapLabel(/^REVIEW SMART LINK$/, 2200);
    await wait(1000);
  });

  await recordSegment('drip-03-create-qr', async () => {
    await scrollToLabel(/^SHARE WITH SMART LINK$/);
    await tapLabel(/^SHARE WITH SMART LINK$/, 1000);
    await waitForLabel(/SMART Link active/i, 22000);
    await wait(1000);
    await scrollToLabel(/^STOP SHARING$/);
    await swipe(540, 650, 540, 1450, 500, 800);
    await swipe(540, 750, 540, 1150, 450, 1200);
    await swipe(540, 760, 540, 1260, 450, 1200);
    await wait(3200);
  });
}

async function preloadDripSampleData() {
  await adb('shell', 'pm', 'clear', 'com.drip').catch(() => {});
  await adb('shell', 'pm', 'grant', 'com.drip', 'android.permission.POST_NOTIFICATIONS').catch(() => {});
  await startActivity('com.drip/.MainActivity', 3500);
  await waitForLabel(/^OK$/, 8000).then(() => tapLabel(/^OK$/, 900)).catch(() => {});
  await openDripDataFromCurrent();
  await scrollToLabel(/^LOAD SAMPLE DATA$/);
  await tapLabel(/^LOAD SAMPLE DATA$/, 1800);
  await tapLabel(/^OK$/, 900).catch(() => {});
  await wait(1000);
  await forceStop('com.drip');
}

async function openDripDataFromCurrent() {
  await tap(996, 165);
  await tapLabel(/^Settings$/);
  await tapLabel(/^Data$/);
  await wait(1200);
}

async function openDripData() {
  await forceStop('com.drip');
  await adb('shell', 'input', 'keyevent', 'HOME');
  await startActivity('com.drip/.MainActivity', 3000);
  await tapLabel(/^OK$/, 900).catch(() => {});
  await openDripDataFromCurrent();
}

async function prepareDripActiveShareForStop() {
  await openDripData();
  if (await hasVisibleLabel(/^STOP SHARING$/)) {
    return;
  }
  await tapLabel(/^LOAD SAMPLE DATA$/, 1500).catch(() => {});
  await tapLabel(/^OK$/, 900).catch(() => {});
  await scrollToLabel(/^REVIEW SMART LINK$/);
  await tapLabel(/^REVIEW SMART LINK$/, 1700);
  await scrollToLabel(/^SHARE WITH SMART LINK$/);
  await tapLabel(/^SHARE WITH SMART LINK$/, 6500);
  await scrollToLabel(/^STOP SHARING$/);
  await wait(1200);
}

async function dripStopOnly() {
  let currentShareReady = false;
  try {
    await scrollToLabel(/^STOP SHARING$/);
    currentShareReady = true;
  } catch (_) {
    currentShareReady = false;
  }
  if (!currentShareReady) {
    await prepareDripActiveShareForStop();
  }
  await recordSegment('drip-04-disable', async () => {
    await tapLabel(/^STOP SHARING$/, 3000);
    await waitForLabel(/Sharing stopped|previous QR\/link/i, 8000).catch(() => {});
    await wait(2500);
  });
}

async function euki() {
  await eukiCreateOnly();
  await eukiStopOnly();
}

async function eukiCreateOnly() {
  await preloadEukiSampleData();

  await recordSegment('euki-01-native-tour', async () => {
    await forceStop('com.kollectivemobile.euki');
    await adb('shell', 'input', 'keyevent', 'HOME');
    await startActivity('com.kollectivemobile.euki/.ui.SplashActivity', 3200);
    await waitForLabel(/^SETTINGS$/, 10000);
    await wait(4500);
  });

  await recordSegment('euki-02-scope-preview-create', async () => {
    await forceStop('com.kollectivemobile.euki');
    await adb('shell', 'input', 'keyevent', 'HOME');
    await startActivity('com.kollectivemobile.euki/.ui.SplashActivity', 3200);
    await waitForLabel(/^SETTINGS$/, 10000);
    await tapLabel(/^SETTINGS$/, 1100);
    await scrollToLabel(/^REVIEW SMART LINK$/);
    await tapLabel(/^REVIEW SMART LINK$/, 1000);
    await waitForLabel(/^SMART Link preview:/i, 15000).catch(async () => {
      await tapLabel(/^REVIEW SMART LINK$/, 1000);
      await waitForLabel(/^SMART Link preview:/i, 15000);
    });
    await scrollToLabel(/^SHARE WITH SMART LINK$/);
    await tapLabel(/^SHARE WITH SMART LINK$/, 8000);
    await swipe(540, 1650, 540, 600, 800, 1400);
    await wait(2000);
  });
}

async function preloadEukiSampleData() {
  await forceStop('com.kollectivemobile.euki');
  await startActivity('com.kollectivemobile.euki/.ui.SplashActivity', 3200);
  await waitForLabel(/^SETTINGS$/, 10000);
  await tapLabel(/^SETTINGS$/, 1100);
  await scrollToLabel(/^LOAD SAMPLE DATA$/);
  await tapLabel(/^LOAD SAMPLE DATA$/, 5200);
  await forceStop('com.kollectivemobile.euki');
}

async function eukiStopOnly() {
  await recordSegment('euki-03-disable', async () => {
    await swipe(540, 1500, 540, 700, 600, 1000);
    await tapLabelAfterScroll(/^STOP SHARING$/, 4000);
    await wait(2500);
  });
}

async function menstrudel() {
  await menstrudelCreateOnly();
  await menstrudelStopOnly();
}

async function menstrudelCreateOnly() {
  await preloadMenstrudelSampleData();

  await recordSegment('menstrudel-01-native-tour', async () => {
    await forceStop('com.whitticase.menstrudel');
    await adb('shell', 'input', 'keyevent', 'HOME');
    await startActivity('com.whitticase.menstrudel/.MainActivity', 3200);
    await waitForLabel(/Settings/, 10000);
    await wait(4500);
  });

  await recordSegment('menstrudel-02-share-create', async () => {
    await forceStop('com.whitticase.menstrudel');
    await adb('shell', 'input', 'keyevent', 'HOME');
    await startActivity('com.whitticase.menstrudel/.MainActivity', 3200);
    await waitForLabel(/Settings/, 10000);
    await tapLabel(/Settings/, 1000);
    await tapLabel(/^Data Management$/, 1100);
    await scrollToLabel(/^Review$/);
    await tapLabel(/^Review$/, 4500);
    await scrollToLabel(/^Share with SMART Link$/);
    await tapLabel(/^Share with SMART Link$/, 1000);
    await waitForLabel(/Active SMART Link/i, 18000);
    await wait(1000);
    await swipe(540, 1650, 540, 600, 600, 1400);
    await swipe(540, 1550, 540, 1250, 400, 900);
    await swipe(540, 1550, 540, 800, 500, 1200);
    await wait(6500);
  });
}

async function preloadMenstrudelSampleData() {
  await forceStop('com.whitticase.menstrudel');
  await startActivity('com.whitticase.menstrudel/.MainActivity', 3200);
  await waitForLabel(/Settings/, 10000);
  await tapLabel(/Settings/, 1000);
  await tapLabel(/^Data Management$/, 1100);
  await scrollToLabel(/^Load sample$/);
  await tapLabel(/^Load sample$/, 3500);
  await forceStop('com.whitticase.menstrudel');
}

async function menstrudelStopOnly() {
  await recordSegment('menstrudel-03-disable', async () => {
    await swipe(540, 1650, 540, 900, 500, 900);
    await tapLabelAfterScroll(/^Stop sharing$/, 4000);
    await wait(2500);
  });
}

async function main() {
  const app = process.argv[2];
  if (app === 'drip') {
    await drip();
    return;
  }
  if (app === 'drip-create-only') {
    await dripCreateOnly();
    return;
  }
  if (app === 'drip-stop-only') {
    await dripStopOnly();
    return;
  }
  if (app === 'euki') {
    await euki();
    return;
  }
  if (app === 'euki-create-only') {
    await eukiCreateOnly();
    return;
  }
  if (app === 'euki-stop-only') {
    await eukiStopOnly();
    return;
  }
  if (app === 'menstrudel') {
    await menstrudel();
    return;
  }
  if (app === 'menstrudel-create-only') {
    await menstrudelCreateOnly();
    return;
  }
  if (app === 'menstrudel-stop-only') {
    await menstrudelStopOnly();
    return;
  }
  throw new Error(`Unknown app: ${app}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
