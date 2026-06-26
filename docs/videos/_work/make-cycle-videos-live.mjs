#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import path from 'node:path';

const root = path.resolve(new URL('../../..', import.meta.url).pathname);
const docsVideos = path.join(root, 'docs/videos');
const workDir = path.join(docsVideos, '_work');
const rawDir = path.join(workDir, 'raw');
const liveDir = path.join(workDir, 'live');
const shellDir = path.join(liveDir, 'shells');
const segmentDir = path.join(liveDir, 'segments');
const silentDir = path.join(liveDir, 'silent');
const titleDir = path.join(liveDir, 'titles');
const appVideoDir = path.join(docsVideos, 'per-app');
const finalDir = path.join(docsVideos, 'final');
const audioDir = path.join(docsVideos, 'audio');
const narrationDir = path.join(docsVideos, 'narration');

for (const dir of [liveDir, shellDir, segmentDir, silentDir, titleDir, appVideoDir, finalDir]) {
  mkdirSync(dir, { recursive: true });
}

const theme = {
  paper: '#FCFCFA',
  paperSunken: '#F1F0EA',
  ink: '#211B18',
  inkMuted: '#6E6259',
  line: '#EAE7E0',
  codeBg: '#251F1C',
  coral: '#E5484D',
  amber: '#E7902B',
  teal: '#109E83',
  plum: '#7C5BD9',
};

const apps = [
  {
    id: 'drip',
    title: 'drip',
    platform: 'Android local tracker',
    accent: theme.teal,
    tailHold: 2,
    segments: [
      {
        clip: 'drip-01-native-tour.mp4',
        label: 'Native sample data tour',
        notes: ['Sample data is preloaded before recording', 'Home, chart, and stats are ordinary drip views', 'The share snapshot comes from local app data'],
      },
      {
        clip: 'drip-02-scope-preview.mp4',
        label: 'Review the SMART Link share',
        notes: ['Date range plus category switches', 'Preview and QR use the same snapshot', 'Unsupported fields stay out'],
      },
      {
        clip: 'drip-03-create-qr.mp4',
        label: 'Create the live SMART Link QR',
        cropBottom: 170,
        notes: ['FHIR Bundle encrypted as compact JWE', 'shlep stores ciphertext only', 'QR, copy, and share carry one link'],
      },
      {
        clip: 'drip-viewer-chromium.mp4',
        label: 'Open the doctor viewer',
        notes: ['Desktop Chromium opens the same SMART Link', 'The viewer fetches ciphertext from shlep', 'Clinical review renders after local decrypt'],
      },
      {
        clip: 'drip-04-disable.mp4',
        label: 'Disable the SMART Link',
        cropBottom: 380,
        notes: ['Stop Sharing revokes the shlep share', 'The old SMART Link no longer resolves', 'Expiry and max opens remain safeguards'],
      },
    ],
  },
  {
    id: 'euki',
    title: 'Euki',
    platform: 'Android privacy-first tracker',
    accent: theme.plum,
    tailHold: 2,
    segments: [
      {
        clip: 'euki-01-native-tour.mp4',
        label: 'Native sample data tour',
        notes: ['Synthetic data is preloaded as ordinary Euki entries', 'The app starts from its normal populated UI', 'Sharing is not a detached export fixture'],
      },
      {
        clip: 'euki-02-scope-preview-create.mp4',
        label: 'Review and create SMART Link',
        notes: ['Settings exposes the SMART Link panel', 'Preview names counts and omitted fields', 'The QR is a live managed link'],
      },
      {
        clip: 'euki-viewer-chromium.mp4',
        label: 'Open the doctor viewer',
        notes: ['Desktop Chromium opens the SMART Link', 'cycle.fhir.me decrypts locally', 'The host never receives the key'],
      },
      {
        clip: 'euki-03-disable.mp4',
        label: 'Stop sharing',
        notes: ['Stop Sharing calls the shlep manage endpoint', 'The previous SMART Link no longer resolves', 'The app returns to share-ready state'],
      },
    ],
  },
  {
    id: 'menstrudel',
    title: 'Menstrudel',
    platform: 'Android Data Management flow',
    accent: theme.amber,
    tailHold: 2,
    segments: [
      {
        clip: 'menstrudel-01-native-tour.mp4',
        label: 'Native sample data tour',
        notes: ['Sample data is preloaded before recording', 'Normal app views show populated local data', 'The share path stays integrated with Data Management'],
      },
      {
        clip: 'menstrudel-02-share-create.mp4',
        label: 'Review and create SMART Link',
        notes: ['Data Management contains the SMART Link panel', 'Local profile scope; identity not included', 'Preview is built from the same snapshot'],
      },
      {
        clip: 'menstrudel-viewer-chromium.mp4',
        label: 'Open the doctor viewer',
        notes: ['Desktop Chromium opens the SMART Link', 'The viewer derives the clinical summary', 'Decryption happens client-side'],
      },
      {
        clip: 'menstrudel-03-disable.mp4',
        label: 'Stop sharing',
        notes: ['Stop calls the shlep control plane', 'The previous SMART Link returns 404', 'Expiry and max-use still apply'],
      },
    ],
  },
  {
    id: 'ovumcy',
    title: 'Ovumcy',
    platform: 'Account-backed web app',
    accent: theme.coral,
    tailHold: 4,
    segments: [
      {
        clip: 'ovumcy-01-native-tour.mp4',
        label: 'Native web sample data tour',
        notes: ['The account is preloaded with sample cycle data', 'Dashboard/calendar views show ordinary app data', 'Settings shares from that live account state'],
      },
      {
        clip: 'ovumcy-02-share-create.mp4',
        label: 'Backend creates the live SMART Link',
        notes: ['The web account supplies the source data', 'Server builds the approved snapshot', 'Only compact ciphertext is uploaded to shlep'],
      },
      {
        clip: 'ovumcy-03-viewer.mp4',
        label: 'Open the clinician viewer',
        notes: ['cycle.fhir.me fetches ciphertext', 'The key remains in the URL fragment', 'Review is rendered from decrypted granular facts'],
      },
      {
        clip: 'ovumcy-04-stop.mp4',
        label: 'Disable the SMART Link',
        notes: ['The rendered manage token is posted once', 'shlep revokes the hosted object', 'The old SMART Link no longer resolves'],
      },
    ],
  },
];

function run(command, args, options = {}) {
  try {
    return execFileSync(command, args, {
      cwd: root,
      encoding: options.encoding || 'utf8',
      stdio: options.stdio || ['ignore', 'pipe', 'pipe'],
    });
  } catch (error) {
    const stdout = error.stdout ? String(error.stdout) : '';
    const stderr = error.stderr ? String(error.stderr) : '';
    throw new Error(`${command} ${args.join(' ')} failed\n${stdout}${stderr}`);
  }
}

function ffprobeDuration(file) {
  const out = run('ffprobe', [
    '-v',
    'error',
    '-show_entries',
    'format=duration',
    '-of',
    'default=nw=1:nk=1',
    file,
  ]).trim();
  return Number(out);
}

function identifySize(file) {
  return run('ffprobe', [
    '-v',
    'error',
    '-select_streams',
    'v:0',
    '-show_entries',
    'stream=width,height',
    '-of',
    'csv=p=0:s=x',
    file,
  ]).trim().split('x').map(Number);
}

function rel(file) {
  return path.relative(root, file);
}

function relFromVideos(file) {
  return path.relative(docsVideos, file);
}

function escapeXml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

function imageDataUri(file) {
  const ext = path.extname(file).slice(1).toLowerCase() || 'png';
  const mime = ext === 'jpg' || ext === 'jpeg' ? 'image/jpeg' : 'image/png';
  return `data:${mime};base64,${readFileSync(file).toString('base64')}`;
}

function wrapText(text, chars) {
  const words = String(text).split(/\s+/).filter(Boolean);
  const lines = [];
  let line = '';
  for (const word of words) {
    if (!line) {
      line = word;
    } else if ((line + ' ' + word).length <= chars) {
      line += ' ' + word;
    } else {
      lines.push(line);
      line = word;
    }
  }
  if (line) lines.push(line);
  return lines;
}

function textBlock(lines, x, y, options = {}) {
  const {
    size = 38,
    weight = 400,
    fill = theme.ink,
    lineHeight = Math.round(size * 1.35),
    maxChars = 36,
  } = options;
  let currentY = y;
  const out = [];
  for (const line of Array.isArray(lines) ? lines : [lines]) {
    for (const wrapped of wrapText(line, maxChars)) {
      out.push(`<text x="${x}" y="${currentY}" font-size="${size}" font-weight="${weight}" fill="${fill}">${escapeXml(wrapped)}</text>`);
      currentY += lineHeight;
    }
  }
  return out.join('\n');
}

function bulletList(items, x, y, maxChars) {
  let currentY = y;
  const out = [];
  for (const item of items) {
    const wrapped = wrapText(item, maxChars);
    out.push(`<circle cx="${x}" cy="${currentY - 10}" r="7" fill="${theme.coral}"/>`);
    wrapped.forEach((line, index) => {
      out.push(`<text x="${x + 28}" y="${currentY + index * 40}" font-size="31" fill="${theme.inkMuted}">${escapeXml(line)}</text>`);
    });
    currentY += Math.max(1, wrapped.length) * 40 + 22;
  }
  return out.join('\n');
}

function frameForClip(clip) {
  const [width, height] = identifySize(clip);
  if (height / width > 1.2) {
    return { x: 92, y: 94, w: 610, h: 902, textX: 780, textChars: 43 };
  }
  return { x: 70, y: 208, w: 1040, h: 680, textX: 1180, textChars: 31 };
}

function renderShell(app, segment, index) {
  const clip = path.join(rawDir, segment.clip);
  if (!existsSync(clip)) throw new Error(`Missing raw clip: ${rel(clip)}`);
  const box = frameForClip(clip);
  const svgPath = path.join(shellDir, `${app.id}-${String(index + 1).padStart(2, '0')}.svg`);
  const pngPath = path.join(shellDir, `${app.id}-${String(index + 1).padStart(2, '0')}.png`);
  const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" viewBox="0 0 1920 1080">
  <rect width="1920" height="1080" fill="${theme.paper}"/>
  <line x1="0" y1="82" x2="1920" y2="82" stroke="${theme.line}" stroke-width="2"/>
  <text x="74" y="52" font-size="26" font-weight="700" fill="${theme.ink}">cycle<tspan fill="${theme.coral}">.fhir.me</tspan></text>
  <text x="304" y="52" font-size="23" fill="${theme.inkMuted}">SMART Link implementation flow</text>
  <rect x="1638" y="22" width="196" height="38" rx="19" fill="${app.accent}" opacity="0.14"/>
  <text x="1660" y="49" font-size="22" font-weight="700" fill="${app.accent}">${escapeXml(app.id)}</text>
  <rect x="${box.x - 14}" y="${box.y - 14}" width="${box.w + 28}" height="${box.h + 28}" rx="8" fill="${theme.paperSunken}" stroke="${theme.line}" stroke-width="2"/>
  <rect x="${box.x}" y="${box.y}" width="${box.w}" height="${box.h}" fill="${theme.paper}"/>
  <rect x="${box.x - 14}" y="${box.y - 14}" width="${box.w + 28}" height="${box.h + 28}" rx="8" fill="none" stroke="${app.accent}" stroke-width="4" opacity="0.85"/>
  <text x="${box.textX}" y="178" font-size="34" fill="${app.accent}" font-weight="700">${escapeXml(app.platform)}</text>
  ${textBlock(app.title, box.textX, 250, { size: 70, weight: 800, maxChars: 19 })}
  ${textBlock(segment.label, box.textX, 335, { size: 43, weight: 700, maxChars: box.textChars })}
  <line x1="${box.textX}" y1="388" x2="1815" y2="388" stroke="${theme.line}" stroke-width="3"/>
  ${bulletList(segment.notes, box.textX, 456, box.textChars)}
  <rect x="${box.textX}" y="902" width="630" height="70" rx="8" fill="${theme.codeBg}"/>
  <text x="${box.textX + 28}" y="946" font-size="28" font-family="monospace" fill="#F3E9DB">SMART Link -> local decrypt</text>
  <rect x="74" y="1031" width="442" height="12" rx="6" fill="${theme.coral}"/>
  <rect x="536" y="1031" width="442" height="12" rx="6" fill="${theme.amber}"/>
  <rect x="998" y="1031" width="442" height="12" rx="6" fill="${theme.teal}"/>
  <rect x="1460" y="1031" width="386" height="12" rx="6" fill="${theme.plum}"/>
</svg>`;
  writeFileSync(svgPath, svg);
  run('magick', [svgPath, pngPath]);
  return { pngPath, box, clip };
}

function renderLiveSegment(app, segment, index, speed, holdTail = 0) {
  const { pngPath, box, clip } = renderShell(app, segment, index);
  const clipDuration = ffprobeDuration(clip);
  const target = clipDuration / speed;
  const outputDuration = target + holdTail;
  const out = path.join(segmentDir, `${app.id}-${String(index + 1).padStart(2, '0')}.mp4`);
  const cropBottom = Number(segment.cropBottom ?? app.cropBottom ?? 0);
  const cropFilter = cropBottom > 0 ? `,crop=iw:ih-${cropBottom}:0:0` : '';
  rmSync(out, { force: true });
  run('ffmpeg', [
    '-y',
    '-loglevel',
    'error',
    '-i',
    clip,
    '-loop',
    '1',
    '-t',
    target.toFixed(3),
    '-i',
    pngPath,
    '-filter_complex',
    `[0:v]setpts=PTS/${speed.toFixed(6)},fps=30${cropFilter},scale=${box.w}:${box.h}:force_original_aspect_ratio=decrease,pad=${box.w}:${box.h}:(ow-iw)/2:(oh-ih)/2:color=FCFCFA,format=rgba[clip];[1:v]fps=30,format=rgba[bg];[bg][clip]overlay=${box.x}:${box.y}:format=auto,format=yuv420p,tpad=stop_mode=clone:stop_duration=${holdTail.toFixed(3)}[v]`,
    '-map',
    '[v]',
    '-an',
    '-t',
    outputDuration.toFixed(3),
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
  return { out, label: segment.label, duration: ffprobeDuration(out) };
}

function escapeConcatPath(file) {
  return file.replaceAll("'", "'\\''");
}

function concatVideos(parts, out) {
  const list = path.join(liveDir, `${path.basename(out, '.mp4')}-list.txt`);
  writeFileSync(list, parts.map((file) => `file '${escapeConcatPath(file)}'`).join('\n') + '\n');
  rmSync(out, { force: true });
  run('ffmpeg', [
    '-y',
    '-loglevel',
    'error',
    '-f',
    'concat',
    '-safe',
    '0',
    '-i',
    list,
    '-c:v',
    'libx264',
    '-preset',
    'medium',
    '-crf',
    '20',
    '-an',
    '-movflags',
    '+faststart',
    out,
  ]);
}

function muxAudio(video, audio, out) {
  rmSync(out, { force: true });
  const videoDuration = ffprobeDuration(video);
  const audioDuration = ffprobeDuration(audio);
  const padDuration = Math.max(0, videoDuration - audioDuration);
  run('ffmpeg', [
    '-y',
    '-loglevel',
    'error',
    '-i',
    video,
    '-i',
    audio,
    '-filter_complex',
    `[1:a]apad=pad_dur=${padDuration.toFixed(3)}[a]`,
    '-map',
    '0:v',
    '-map',
    '[a]',
    '-c:v',
    'copy',
    '-c:a',
    'aac',
    '-b:a',
    '160k',
    '-ar',
    '44100',
    '-ac',
    '2',
    '-t',
    videoDuration.toFixed(3),
    '-movflags',
    '+faststart',
    out,
  ]);
}

function renderAppVideo(app) {
  const audio = path.join(audioDir, `${app.id}.mp3`);
  if (!existsSync(audio)) throw new Error(`Missing audio: ${rel(audio)}`);
  const audioDuration = ffprobeDuration(audio);
  if (audioDuration > 120) throw new Error(`${app.id} narration is over 120 seconds`);
  const rawTotal = app.segments
    .map((segment) => ffprobeDuration(path.join(rawDir, segment.clip)))
    .reduce((sum, duration) => sum + duration, 0);
  const speed = rawTotal / audioDuration;
  const rendered = app.segments.map((segment, index) => renderLiveSegment(
    app,
    segment,
    index,
    speed,
    index === app.segments.length - 1 ? app.tailHold || 0 : 0,
  ));
  const silent = path.join(silentDir, `${app.id}-live-silent.mp4`);
  concatVideos(rendered.map((segment) => segment.out), silent);
  const out = path.join(appVideoDir, `${app.id}-smart-link-flow.mp4`);
  muxAudio(silent, audio, out);

  const starts = [];
  let cursor = 0;
  for (const segment of rendered) {
    starts.push({ label: segment.label, time: cursor });
    cursor += segment.duration;
  }

  return {
    out,
    audio,
    duration: ffprobeDuration(out),
    rawTotal,
    speed,
    starts,
  };
}

function renderStillVideo(image, duration, out) {
  rmSync(out, { force: true });
  run('ffmpeg', [
    '-y',
    '-loglevel',
    'error',
    '-loop',
    '1',
    '-t',
    duration.toFixed(3),
    '-i',
    image,
    '-f',
    'lavfi',
    '-t',
    duration.toFixed(3),
    '-i',
    'anullsrc=channel_layout=stereo:sample_rate=44100',
    '-vf',
    'fps=30,format=yuv420p',
    '-c:v',
    'libx264',
    '-preset',
    'medium',
    '-crf',
    '20',
    '-c:a',
    'aac',
    '-b:a',
    '160k',
    '-ar',
    '44100',
    '-ac',
    '2',
    '-shortest',
    '-movflags',
    '+faststart',
    out,
  ]);
}

function renderOpeningTitle() {
  const screenshot = path.join(workDir, 'smart-link-home.png');
  const image = existsSync(screenshot)
    ? `<image href="${imageDataUri(screenshot)}" x="96" y="190" width="1110" height="694" preserveAspectRatio="xMidYMid slice" opacity="0.95"/>
  <rect x="96" y="190" width="1110" height="694" rx="8" fill="none" stroke="${theme.line}" stroke-width="2"/>`
    : '';
  const svgPath = path.join(titleDir, 'opening.svg');
  const pngPath = path.join(titleDir, 'opening.png');
  const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" viewBox="0 0 1920 1080">
  <rect width="1920" height="1080" fill="${theme.paper}"/>
  ${image}
  <rect x="1248" y="190" width="560" height="694" rx="8" fill="${theme.paperSunken}" stroke="${theme.line}" stroke-width="2"/>
  <text x="1290" y="310" font-size="48" font-weight="800" fill="${theme.ink}">Period Tracking</text>
  <text x="1290" y="370" font-size="44" font-weight="800" fill="${theme.ink}">SMART Link Demo</text>
  <text x="1290" y="482" font-size="76" font-weight="900" fill="${theme.coral}">cycle.fhir.me</text>
  <text x="1290" y="592" font-size="32" fill="${theme.inkMuted}">Live app recordings embedded</text>
  <text x="1290" y="638" font-size="32" fill="${theme.inkMuted}">in a SMART Link walkthrough.</text>
  <text x="1290" y="710" font-size="32" fill="${theme.inkMuted}">Each demo shows native data,</text>
  <text x="1290" y="756" font-size="32" fill="${theme.inkMuted}">share review, QR handoff, viewer,</text>
  <text x="1290" y="802" font-size="32" fill="${theme.inkMuted}">and disable/revoke.</text>
  <rect x="1290" y="842" width="354" height="70" rx="8" fill="${theme.codeBg}"/>
  <text x="1320" y="888" font-size="30" font-family="monospace" fill="#F3E9DB">#shlink:/...</text>
</svg>`;
  writeFileSync(svgPath, svg);
  run('magick', [svgPath, pngPath]);
  return pngPath;
}

function renderAppTitle(app) {
  const svgPath = path.join(titleDir, `title-${app.id}.svg`);
  const pngPath = path.join(titleDir, `title-${app.id}.png`);
  const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" viewBox="0 0 1920 1080">
  <rect width="1920" height="1080" fill="${theme.paper}"/>
  <rect x="112" y="116" width="1696" height="848" rx="8" fill="${theme.paperSunken}" stroke="${theme.line}" stroke-width="2"/>
  <circle cx="250" cy="248" r="42" fill="${theme.coral}"/>
  <circle cx="352" cy="248" r="42" fill="${theme.amber}"/>
  <circle cx="454" cy="248" r="42" fill="${theme.teal}"/>
  <circle cx="556" cy="248" r="42" fill="${theme.plum}"/>
  <text x="226" y="420" font-size="150" font-weight="800" fill="${theme.ink}">${escapeXml(app.title)}</text>
  <text x="236" y="502" font-size="46" font-weight="700" fill="${app.accent}">${escapeXml(app.platform)}</text>
  <text x="238" y="652" font-size="40" fill="${theme.inkMuted}">Native data -> SMART Link -> viewer -> stop sharing</text>
  <text x="238" y="742" font-size="34" fill="${theme.inkMuted}">The following slides embed live screen recordings, not screenshot sequences.</text>
  <text x="236" y="874" font-size="42" font-weight="800" fill="${theme.ink}">cycle<tspan fill="${theme.coral}">.fhir.me</tspan></text>
</svg>`;
  writeFileSync(svgPath, svg);
  run('magick', [svgPath, pngPath]);
  return pngPath;
}

function buildCombined(appResults) {
  const openingVideo = path.join(silentDir, '00-opening.mp4');
  renderStillVideo(renderOpeningTitle(), 4, openingVideo);
  const parts = [openingVideo];

  for (const app of apps) {
    const titleVideo = path.join(silentDir, `title-${app.id}.mp4`);
    renderStillVideo(renderAppTitle(app), 2.75, titleVideo);
    parts.push(titleVideo, appResults.get(app.id).out);
  }

  const out = path.join(finalDir, 'smart-link-implementation-reel.mp4');
  const list = path.join(liveDir, 'combined-list.txt');
  writeFileSync(list, parts.map((file) => `file '${escapeConcatPath(file)}'`).join('\n') + '\n');
  rmSync(out, { force: true });
  run('ffmpeg', [
    '-y',
    '-loglevel',
    'error',
    '-f',
    'concat',
    '-safe',
    '0',
    '-i',
    list,
    '-c:v',
    'libx264',
    '-preset',
    'medium',
    '-crf',
    '20',
    '-c:a',
    'aac',
    '-b:a',
    '160k',
    '-ar',
    '44100',
    '-ac',
    '2',
    '-movflags',
    '+faststart',
    out,
  ]);

  const starts = [];
  let cursor = 0;
  starts.push({ label: 'SMART Link opening: cycle.fhir.me', time: cursor });
  cursor += 4;
  for (const app of apps) {
    starts.push({ label: `${app.title} title`, time: cursor });
    cursor += 2.75;
    starts.push({ label: `${app.title} live flow`, time: cursor });
    cursor += appResults.get(app.id).duration;
  }
  return { out, duration: ffprobeDuration(out), starts };
}

function formatTime(seconds) {
  const rounded = Math.max(0, Math.round(seconds));
  const mins = Math.floor(rounded / 60);
  const secs = String(rounded % 60).padStart(2, '0');
  return `${mins}:${secs}`;
}

function writeIndex(appResults, combined) {
  const lines = [
    '# SMART Link implementation videos',
    '',
    'Generated 2026-06-26 from live Android `screenrecord` captures, Playwright browser video capture for Ovumcy, and the narrated SMART Link audio tracks.',
    '',
    'Each per-app video embeds real app video inside the slide-style presentation frame. The live clips are speed-adjusted only enough to align with the measured narration audio; they are not screenshot sequences.',
    '',
    '## Files',
    '',
    '| App | Video | Duration | Raw live input | Audio | Narration |',
    '|---|---:|---:|---:|---:|---:|',
  ];
  for (const app of apps) {
    const result = appResults.get(app.id);
    lines.push(`| ${app.title} | [${path.basename(result.out)}](${relFromVideos(result.out)}) | ${formatTime(result.duration)} | ${formatTime(result.rawTotal)} at ${result.speed.toFixed(2)}x | [${app.id}.mp3](${relFromVideos(path.join(audioDir, `${app.id}.mp3`))}) | [${app.id}.txt](${relFromVideos(path.join(narrationDir, `${app.id}.txt`))}) |`);
  }
  lines.push(`| Combined reel | [${path.basename(combined.out)}](${relFromVideos(combined.out)}) | ${formatTime(combined.duration)} | all raw live clips | mixed | title slides plus all app audio |`);
  lines.push('', '## Per-app key moments', '');
  for (const app of apps) {
    const result = appResults.get(app.id);
    lines.push(`### ${app.title} (${formatTime(result.duration)})`);
    for (const start of result.starts) {
      lines.push(`- ${formatTime(start.time)} - ${start.label}`);
    }
    lines.push('');
  }
  lines.push('## Combined reel sequence', '');
  for (const start of combined.starts) {
    lines.push(`- ${formatTime(start.time)} - ${start.label}`);
  }
  lines.push('');
  writeFileSync(path.join(docsVideos, 'video-index.md'), lines.join('\n'));
}

function main() {
  const appResults = new Map();
  for (const app of apps) {
    console.log(`== ${app.title} ==`);
    const result = renderAppVideo(app);
    appResults.set(app.id, result);
    console.log(`${rel(result.out)} ${result.duration.toFixed(2)}s, raw ${result.rawTotal.toFixed(2)}s, speed ${result.speed.toFixed(2)}x`);
  }
  const combined = buildCombined(appResults);
  writeIndex(appResults, combined);
  console.log(`${rel(combined.out)} ${combined.duration.toFixed(2)}s`);
  console.log(rel(path.join(docsVideos, 'video-index.md')));
}

main();
