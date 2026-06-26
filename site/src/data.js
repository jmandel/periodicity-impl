// Central content model for the landing page. Keeping copy here keeps the
// components presentational and makes it easy to keep facts in sync with the
// per-app writeups under /docs.

export const REPO_URL = 'https://github.com/jmandel/periodicity-impl';
export const CYCLE_VIEWER_URL = 'https://cycle.fhir.me/view';
export const CYCLE_IG_URL = 'https://cycle.fhir.me';
export const SHLEP_HOST = 'https://shlep.exe.xyz';

const docUrl = (file) => `${REPO_URL}/blob/main/docs/${file}`;
const branchUrl = (branch) => `${REPO_URL}/tree/${branch}`;

export const HERO = {
  eyebrow: 'Cycle FHIR IG · SMART Link',
  title: 'What it takes to implement the Cycle FHIR IG in four open source apps.',
  lede:
    'The Cycle FHIR Implementation Guide is a specification that gives menstrual tracking apps a shared, privacy-respecting way to package and share cycle data. This page is a record of adding Cycle IG support to four existing open source apps — two native Android, one Flutter, and one server-rendered web app. It explains what each implementation involved and collects what we learned, so you can do the same in your own app.',
  // Primary points at the specification (cycle.fhir.me); secondary points at
  // the implementation code (this GitHub repo). The labels spell out which is
  // which so the two destinations are never confused.
  primary: { label: 'Read the Cycle IG spec (cycle.fhir.me)', href: CYCLE_IG_URL },
  secondary: { label: 'Browse the implementation code (GitHub)', href: REPO_URL },
};

// First-time visitors confuse the specification, the viewer, and this
// implementation repository. This section names each one explicitly and links
// to it, so it is always clear which is which.
export const ORIENTATION = [
  {
    heading: 'The Cycle IG (the specification)',
    body:
      'The Cycle FHIR Implementation Guide is the spec that defines how an app packages cycle data into a FHIR Bundle and shares it as an encrypted SMART Link. It is published at cycle.fhir.me and is the thing each app on this page implements.',
    link: { label: 'Open cycle.fhir.me', href: CYCLE_IG_URL },
  },
  {
    heading: 'The viewer (where a link opens)',
    body:
      'When someone opens a shared SMART Link, it lands in the clinician viewer at cycle.fhir.me/view. The viewer fetches the ciphertext and decrypts it locally in the browser to render a clinical review of the shared cycle data.',
    link: { label: 'Open cycle.fhir.me/view', href: CYCLE_VIEWER_URL },
  },
  {
    heading: 'This repository (the implementations)',
    body:
      'This GitHub repository holds the four implementations described below: the code branches, the demo videos, and a writeup for each app. It is the implementation work, not the specification — the two are separate projects.',
    link: { label: 'Open the periodicity-impl repo', href: REPO_URL },
  },
];

// The cross-cutting privacy/sharing technique that every app shares.
export const PIPELINE = [
  {
    title: 'Review, not blind export',
    body:
      'The user picks a date range and category switches, then sees preview counts. Nothing leaves the device until they mint a link.',
  },
  {
    title: 'One immutable snapshot',
    body:
      'A single CycleIgSnapshot feeds the preview, the Bundle, encryption, the QR, copy/share/open, and stop. Preview and payload can never drift apart.',
  },
  {
    title: 'Encrypt locally',
    body:
      'The app builds the FHIR period-tracking Bundle and encrypts it client-side into a compact JWE. Plaintext never touches the sharing host.',
  },
  {
    title: 'Ciphertext-only upload',
    body:
      `Only the JWE ciphertext is uploaded to the shlep direct-file host (${SHLEP_HOST}). The host enforces expiry, max-use, audit, and revoke.`,
  },
  {
    title: 'Key stays in the fragment',
    body:
      'The decryption key lives in the SMART Link URL fragment (after #), which is never sent to the server. The QR encodes the whole viewer link.',
  },
  {
    title: 'Viewer decrypts in the browser',
    body:
      'A clinician opens the link in cycle.fhir.me/view, which fetches the ciphertext and decrypts locally to render the clinical review.',
  },
  {
    title: 'Stop means stop',
    body:
      'Stop Sharing deletes the share with its manage token; the old QR resolves to 404. Expiry and max-use exhaustion enforce the same end state.',
  },
];

// Choices that recur across every app and are the real story of the work.
export const CHOICES = [
  {
    heading: 'Layer 0 / Layer 1 mapping',
    body:
      'Each app maps its native model in two layers: Layer 0 is the core "menstrual bleeding true/false" fact; Layer 1 adds flow, temperature, symptoms, mucus, and notes. Stored values become facts; missing days stay silent.',
  },
  {
    heading: 'Explicit negatives, never inferred',
    body:
      'A logged non-bleeding day becomes bleeding=false. Untouched defaults and absent rows emit no negative facts, so the Bundle never invents data the user did not record.',
  },
  {
    heading: 'Intentional omissions',
    body:
      'Sex, contraception, tests, predictions, and derived cycle phases are deliberately left out of the share. Each writeup lists exactly what is omitted and why.',
  },
  {
    heading: 'Identity left out by design',
    body:
      'No Patient resource is emitted. The share carries cycle facts, not who the person is — a smaller, safer payload that still renders a useful clinical review.',
  },
  {
    heading: 'Sample data through native storage',
    body:
      'Synthetic demo data is loaded through each app\u2019s real import/save path, not by bypassing storage. The same code paths a user exercises produce the shared snapshot.',
  },
  {
    heading: 'Smallest change that fits',
    body:
      'The feature lives where users already manage data — Settings, Data Management — reusing each app\u2019s existing UI conventions instead of bolting on a new surface.',
  },
];

export const APPS = [
  {
    id: 'drip',
    name: 'drip',
    platform: 'Android · React Native',
    accent: '#e5484d',
    video: 'videos/per-app/drip-smart-link-flow.mp4',
    poster: 'screenshots/drip/sample-loaded.png',
    duration: '1:09',
    summary:
      'A local, single-profile tracker. SMART Link sits in Settings → Data management next to import and backup.',
    links: [
      { label: 'Implementation branch', href: branchUrl('drip-implemented-work') },
      { label: 'Writeup', href: docUrl('drip-cycle-ig.md') },
      { label: 'Upstream (drip)', href: 'https://gitlab.com/bloodyhealth/drip' },
    ],
    moments: [
      ['0:00', 'Native sample data tour'],
      ['0:13', 'Review the SMART Link share'],
      ['0:32', 'Create the live SMART Link QR'],
      ['0:50', 'Open the doctor viewer'],
      ['0:56', 'Disable the SMART Link'],
    ],
    highlights: [
      'Bleeding values 0–3 map to spotting/light/moderate/heavy; basal temperature carries an optional time.',
      'No numeric 0–10 pain score exists, so Numeric Pain Severity is intentionally not emitted.',
      'Verified Bundle: 193 observations across 6 cycles from one immutable snapshot.',
    ],
    shots: ['share-scope.png', 'share-qr.png', 'viewer-rendered.png'],
  },
  {
    id: 'euki',
    name: 'Euki',
    platform: 'Android · Java',
    accent: '#7c5cff',
    video: 'videos/per-app/euki-smart-link-flow.mp4',
    poster: 'screenshots/euki/sample-loaded.png',
    duration: '1:16',
    summary:
      'A privacy-first local tracker. SMART Link adds a controlled share review inside Cycle settings.',
    links: [
      { label: 'Implementation branch', href: branchUrl('euki-implemented-work') },
      { label: 'Writeup', href: docUrl('euki-cycle-ig.md') },
      { label: 'Upstream (Euki)', href: 'https://github.com/Euki-Inc/Euki-Android' },
    ],
    moments: [
      ['0:00', 'Native sample data tour'],
      ['0:10', 'Review and create SMART Link'],
      ['0:54', 'Open the doctor viewer'],
      ['1:04', 'Stop sharing'],
    ],
    highlights: [
      'include_cycle_summary disambiguates menstrual vs. non-menstrual bleeding at Layer 0.',
      'Products, clots, emotions, and body symptoms map as stored facts; no BBT or numeric pain exist.',
      'Verified Bundle: 212 resources; the stopped QR returns 404 not_servable.',
    ],
    shots: ['share-review.png', 'share-qr.png', 'viewer-rendered.png'],
  },
  {
    id: 'menstrudel',
    name: 'Menstrudel',
    platform: 'Flutter · Android',
    accent: '#0ea5a4',
    video: 'videos/per-app/menstrudel-smart-link-flow.mp4',
    poster: 'screenshots/menstrudel/sample-loaded.png',
    duration: '1:13',
    summary:
      'A Flutter tracker with a derived periods table. The Cycle FHIR share panel lives in Data Management.',
    links: [
      { label: 'Implementation branch', href: branchUrl('menstrudel-implemented-work') },
      { label: 'Writeup', href: docUrl('menstrudel-cycle-ig.md') },
      { label: 'Upstream (Menstrudel)', href: 'https://github.com/J-shw/Menstrudel' },
    ],
    moments: [
      ['0:00', 'Native sample data tour'],
      ['0:07', 'Review and create SMART Link'],
      ['0:48', 'Open the doctor viewer'],
      ['1:03', 'Stop sharing'],
    ],
    highlights: [
      'Exact SNOMED codes for cramps, headache, fatigue, bloating, and depressed mood; app-native codes otherwise.',
      'The five-step pain enum maps as an app-native fact, not Numeric Pain Severity, because it is not a 0–10 score.',
      'Loader is idempotent: sample logs are keyed by date so reruns update rather than duplicate.',
    ],
    shots: ['share-scope.png', 'share-qr.png', 'viewer-rendered.png'],
  },
  {
    id: 'ovumcy',
    name: 'Ovumcy',
    platform: 'Web · Go + JS',
    accent: '#f59e0b',
    video: 'videos/per-app/ovumcy-smart-link-flow.mp4',
    poster: 'screenshots/ovumcy/sample-loaded.png',
    duration: '1:05',
    summary:
      'An account-backed web app. The share is backend-mediated, so the demo runs against a real Go backend.',
    links: [
      { label: 'Implementation branch', href: branchUrl('ovumcy-implemented-work') },
      { label: 'Writeup', href: docUrl('ovumcy-cycle-ig.md') },
      { label: 'Upstream (Ovumcy)', href: 'https://github.com/ovumcy/ovumcy-web' },
    ],
    moments: [
      ['0:00', 'Native web sample data tour'],
      ['0:16', 'Backend creates the live SMART Link'],
      ['0:31', 'Open the clinician viewer'],
      ['0:52', 'Disable the SMART Link'],
    ],
    highlights: [
      'The server reads only approved rows for the authenticated account and builds one snapshot.',
      'The privacy boundary is the SHLink host: shlep sees only ciphertext; the key stays in the link fragment.',
      'Verified Bundle: 350 observations, including basal body temperature facts.',
    ],
    shots: ['share-scope.png', 'share-qr.png', 'viewer-rendered.png'],
  },
];

export const REEL = {
  video: 'videos/final/smart-link-implementation-reel.mp4',
  duration: '4:58',
  caption:
    'One reel showing the same sharing flow across all four apps, opening on the cycle.fhir.me viewer.',
};

export const TIPS = [
  'Build the snapshot once and reuse it everywhere — the preview a user approves must be the exact bytes you encrypt.',
  'Map values you stored, stay silent on values you did not. Inferred negatives are the easiest way to leak a guess as a fact.',
  'Keep the decryption key in the URL fragment. The sharing host should only ever hold opaque ciphertext.',
  'Reuse the same viewer link for the QR, copy, native share, and open — one string means one source of truth.',
  'Make stop real: delete the share with its manage token and confirm the old link returns 404, then lean on expiry and max-use as backstops.',
  'Put the feature where data already lives (Settings, Data Management) so it reads as a natural extension, not a new app.',
  'Load demo data through the app\u2019s real save/import path so the shared Bundle exercises production code.',
];
