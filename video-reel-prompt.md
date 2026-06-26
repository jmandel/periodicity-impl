# Video reel handoff prompt

You are working in a repo where SHLink-based SMART Link sharing has already been implemented across several apps. Your job is to produce a narrated video reel that demonstrates the feature end to end. Do not make a screenshot sequence. Record real app/browser videos, embed those clips in a consistent slide-like presentation shell, synthesize or reuse narration audio, and verify that the final videos show the complete flow.

## Goal

Create per-app videos and one combined reel that show, for each implemented app:

1. App launch or entry into the relevant area.
2. Native sample-data tour: start from sample data already loaded for demo purposes, then show how the data looks in the app's normal native UI, not just in the export/share panel.
3. SMART Link share setup: scope controls, preview/counts/omissions, and live QR/link creation.
4. Doctor/clinician viewer: open the exact live SHLink in desktop Chromium between creation and disable, click through to the decrypted viewer if needed, and show the rendered clinical review.
5. Disable/revoke: return to the app and stop sharing, proving the old QR/link is no longer live or that the UI reports the share as stopped.

The output should be polished enough to hand to a reviewer who wants evidence that the SMART Link feature is live, integrated into the real app, and using a live SHLink backend/viewer path.

## Required outputs

Use this structure unless the repo already has a better local convention:

- `docs/videos/narration/<app>.txt`: final narration text for each app.
- `docs/videos/audio/<app>.mp3`: synthesized narration audio for each app.
- `docs/videos/_work/raw/*.mp4`: raw Android screenrecord, Playwright, or browser clips.
- `docs/videos/_work/*.mjs`: recording/rendering scripts used to reproduce the reel.
- `docs/videos/per-app/<app>-smart-link-flow.mp4`: narrated per-app videos.
- `docs/videos/final/smart-link-implementation-reel.mp4`: combined reel.
- `docs/videos/video-index.md`: generated index with durations and key timestamps.
- `video-reel-prompt.md`: this handoff prompt, updated if the process changes.

## Non-negotiable visual requirements

- The final videos must contain real videos, not still screenshots animated as slides.
- The native app tour must show preloaded sample data in normal app views before the SHLink workflow. Examples: calendar, chart, cycle summary, tracking/log views, symptom/history views, or the app's equivalent native visualizations.
- Do not spend the tour showing the act of loading sample data unless it is unavoidable. Preload sample data before recording, then begin the tour with the app in a populated state.
- The share workflow must show the implemented app UI, not a standalone FHIR export divorced from the SMART Link QR.
- The Chromium viewer segment is required for every app. Record desktop Chromium after enabling and before disabling the share.
- The viewer segment must reach the decrypted clinical/doctor view, not stop at a generic "open link" page. If the viewer has an "Open link" button, click it.
- The QR must be fully visible when shown. If a raw clip partially cuts it off, re-record or use a segment-specific crop/positioning fix that keeps the whole QR visible.
- If the app incorporates the SMART logo or mark near the share/link UI, keep it small and tasteful. It should read as a brand cue beside the SMART Link label or active link header, not compete with the QR or push the QR out of view.
- Avoid visible demo artifacts such as Android/React Native "Refreshing..." banners, dev reload overlays, notification toasts, or dangling loading states. Re-record when practical; otherwise crop only the affected segment, not the whole app.
- Disable/revoke clips must not cut off mid-demo. Hold the final stopped/share-ready state long enough to be visible in the rendered video.
- Audio must be present on every per-app video and the combined reel, and the last sentence must not be truncated.

## Before recording

1. Read the current implementation requirements and checklist, especially `cycle/input/pagecontent/implementation.md`.
2. Confirm the viewer and host are live enough for the demo:
   - `cycle.fhir.me` or the local/current viewer URL can open an SHLink.
   - the public SHLink storage host, usually `shlep.exe.xyz`, is reachable.
   - any web app with server-side source data has a local backend running for recording.
3. Build/install each app as needed and verify the SHLink implementation works manually once.
4. Preload deterministic sample data in each app before the native tour recording. Use app-provided sample-data buttons, test fixtures, database seeds, or setup scripts, but keep that setup outside the final tour unless the user explicitly wants to see it.
5. Decide the segment list per app before recording. A good default is:
   - launch/entry
   - native sample-data tour
   - share scope and preview
   - create QR/link
   - Chromium viewer
   - stop sharing

## Recording Android app clips

Use `adb shell screenrecord` for native Android clips. Prefer separate short clips over one long fragile capture.

Recommended capture settings:

```sh
adb shell screenrecord --bit-rate 8000000 --size 720x1280 /sdcard/<segment>.mp4
adb pull /sdcard/<segment>.mp4 docs/videos/_work/raw/<segment>.mp4
```

Build helper scripts around these primitives so the workflow is reproducible. The helper should:

- start the target app from a known state;
- wait for labels rather than relying only on fixed sleeps;
- tap only visible UI nodes when using accessibility bounds;
- pull partial clips even if the automation fails, so failures can be inspected;
- record short named segments that map directly to the final slide labels.

For the native sample-data tour, start after demo data is already loaded. Show normal app surfaces such as:

- Drip: calendar/chart/stats or the populated chart after sample loading, then settings/data management.
- Euki: cycle summary, daily summary, calendar, tracking/history, or other native populated views, then settings/share.
- Menstrudel: calendar/data views or populated tracking/log views, then Data Management.
- Web apps such as Ovumcy: dashboard/calendar/insights or equivalent populated account views, then settings/share.

The point is to demonstrate that the SHLink snapshot comes from real native app data, not a detached export fixture.

## Recording the Chromium viewer

After creating a live QR/link and before revoking it, record a desktop Chromium viewer clip for that exact link.

Preferred approach:

1. Decode the SHLink from the QR video or screenshot with `zbarimg --raw`, or extract the viewer URL from the app UI if the QR is not decodable.
2. Normalize bare `shlink:/...` payloads into the viewer URL, for example `https://cycle.fhir.me/view#shlink:/...`.
3. Use Playwright to launch Chromium with video recording enabled.
4. `page.goto(viewerURL)`, wait for the page, click an "Open link" button if present, then wait until the decrypted clinical review is visible.
5. Hold the clinical review view for about 8-10 seconds.
6. Transcode Playwright WebM to MP4 for the raw segment.

Do not revoke the app share until the viewer recording is complete. If decoding from video fails because the QR is partly hidden or too dense, use a direct URL copied from the app UI, or re-record with the QR fully visible.

## Recording web-app flows

For account-backed web apps, run a real backend. Do not fake the flow with a static FHIR export. The demo should show:

- registration/login or a prepared account;
- sample data already present in native web views;
- settings/share screen using that account data;
- backend-created live QR/link;
- Chromium viewer opening the resulting SHLink;
- stop/revoke using the app UI.

Use a disposable local database for recording so the flow starts cleanly. Record with Playwright video. If the web app is the source app and the viewer is also browser-based, use separate pages or contexts so the viewer segment is visually distinct.

## Narration and TTS

Write narration first, then render video to match it. Keep each app under 120 seconds unless the user asks for a longer reel.

Narration should cover:

- where the feature lives in the app;
- that demo/sample data has been preloaded and shown natively;
- what data is included and omitted;
- that preview/counts come from the same snapshot used for encryption;
- that only compact encrypted ciphertext is uploaded to shlep;
- that the key stays in the URL fragment;
- that the Chromium viewer decrypts locally;
- that stop/revoke invalidates the previous QR/link.

If using the existing Mistral TTS pattern, synthesize MP3 with:

- environment: `MISTRAL_API_KEY`;
- endpoint: `https://api.mistral.ai/v1/audio/speech`;
- model: `voxtral-mini-tts-latest`;
- voice: the repo's existing neutral/Paul voice ID if present;
- response format: `mp3`.

If `MISTRAL_API_KEY` is unavailable, use a temporary local virtualenv with `edge-tts` rather than committing a new dependency. The June 26, 2026 SMART Link reel used `en-US-BrianNeural` at `--rate=-2%` and wrote directly to `docs/videos/audio/<app>.mp3`.

Store both the text and MP3 in `docs/videos/narration/` and `docs/videos/audio/`. Measure audio duration with `ffprobe`; do not guess.

## Rendering the reel

Render a consistent 1920x1080 presentation shell with live clips embedded inside it. The shell should include:

- app name and platform;
- segment label;
- short bullets explaining the moment;
- a small consistent `SMART Link -> local decrypt` style callout;
- a progress/accent system that is readable but not visually dominant.

Use real clips as inputs. Speed-adjust clips only enough to align with narration timing. Do not turn screenshots into fake videos. If a segment needs a final state to remain visible, add a cloned tail hold with `tpad` or equivalent.

Cropping rules:

- Prefer re-recording over cropping when the important UI is missing.
- If cropping is needed to hide an artifact, make it segment-specific.
- Do not apply app-wide crops unless every segment has been checked. App-wide crops can accidentally cut off launch/native-tour content.

Audio muxing rules:

- If video is longer than narration, pad audio with silence to the full video duration.
- If narration is longer than video, extend the final frame or add holds; do not use `-shortest` in a way that cuts off the final sentence or final state.
- Verify every output has an audio stream.

## Verification checklist

Before reporting completion, verify the rendered outputs, not just the raw clips.

Use `ffprobe` to check duration and audio:

```sh
ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 docs/videos/final/smart-link-implementation-reel.mp4
ffprobe -v error -select_streams a:0 -show_entries stream=codec_name,channels,sample_rate -of csv=p=0 docs/videos/per-app/<app>-smart-link-flow.mp4
```

Extract final-render frames or contact sheets for each app:

- native sample-data tour frame;
- share scope/preview frame;
- full QR frame;
- Chromium clinical viewer frame;
- stopped/revoked/share-ready frame.

Inspect those images manually. Confirm:

- native sample data is visibly shown before the share flow;
- QR is complete and scannable-looking;
- SMART logo placement is aligned, consistently sized, and does not crowd the QR or action buttons;
- viewer is the decrypted clinical/doctor review, not just the landing page;
- stop/revoke reaches a final state;
- no Android "Refreshing..." or dev artifacts are visible in the final render;
- no text or UI is vertically cut off by a crop;
- the combined reel does not cut off mid-sentence or mid-demo.

Regenerate `docs/videos/video-index.md` with final durations and key timestamps after rendering.

## Common failure modes to avoid

- Screenshot sequence instead of live clips.
- Showing only the FHIR export/share panel and never touring the native sample data.
- Loading sample data in the final tour but not showing what it looks like natively afterward.
- Opening the viewer before the link is live or after it has already been revoked.
- Recording a QR that is partially off screen.
- Letting a React Native dev refresh banner or loading toast appear in the final output.
- Applying a crop globally and cutting off unrelated segments.
- Ending the final web-app clip before the revoke/stopped message is visible.
- Using `-shortest` or similar muxing behavior that truncates narration.

## Suggested working order

1. Read implementation docs and app-specific SHLink code.
2. Create/update recording scripts under `docs/videos/_work/`.
3. Preload sample data in every app.
4. Record native sample-data tour clips.
5. Record share scope/preview/create clips.
6. Record Chromium viewer clips from the exact live SHLinks.
7. Record stop/revoke clips.
8. Write narration and synthesize/verify TTS audio.
9. Render per-app videos and combined reel.
10. Generate verification contact sheets from final renders.
11. Fix any visual/audio issues and re-render.
12. Update `docs/videos/video-index.md` and summarize exact output paths.

Do not declare the reel done until the final rendered videos satisfy the checklist above.
