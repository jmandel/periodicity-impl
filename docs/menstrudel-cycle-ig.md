# Menstrudel Cycle IG implementation

## Checklist

| Item | Menstrudel implementation |
| --- | --- |
| Share review screen | Data Management now includes a "Cycle FHIR share" panel with the current local profile scope, date range, category checkboxes, sample-data loader, preview counts, and live QR/link controls. Menstrudel has one local profile, so no separate person/account selector is available. |
| Layer 0 source | Stored `LogDay.flow` is the source. A stored log with `flow != FlowRate.none` becomes menstrual bleeding `true`; a stored log with `flow == FlowRate.none` becomes menstrual bleeding `false`. Missing days and absent rows emit no negative facts. |
| Layer 1 source | All stored flow categories map to the Menstrual Flow profile, including explicit `flow-none`. Stored symptoms map to Symptom observations, using exact SNOMED codes for cramps, headache, fatigue, bloating, and depressed mood, and app-native codes otherwise. Menstrudel's five-step pain enum maps as an app-native Period Tracking Fact, not Numeric Pain Severity, because it is not a 0-10 score. |
| Intentional omissions | The derived `periods` table, cycle predictions, pill/larc/sanitary/sexual-activity tables, profile identity, missing days, and untouched defaults are omitted. Menstrudel does not store basal body temperature or notes on `LogDay`. |
| Live SHLink host | Public `https://shlep.exe.xyz` direct-file share. Menstrudel encrypts locally, uploads only compact JWE ciphertext, requests seven-day expiry and five max opens, and keeps the manage token in memory for Stop. No passcode or access-log UI is shown. |
| QR handoff widget | The QR, copy, native share, and open controls all use the same `https://cycle.fhir.me/view#shlink:/...` string. |
| Stop-sharing behavior | "Stop" deletes the shlep share with the manage token. The same decoded QR URL returned `404 {"error":"not_servable","message":"not found"}` after Stop. Expiry and max-use exhaustion are also enforced by shlep. |
| Receiver path | The app opens the reference viewer. Chrome populated the SHLink field; pressing the viewer's "Open link" button decrypted and rendered the Bundle client-side. |
| Privacy and validation evidence | Unit tests cover sample scoping, Bundle shape/profile invariants, omitted unrelated fields, JWE round trip, viewer-prefixed SHLink parsing, and ciphertext-only upload. A gated live test covers shlep create, resolve/decrypt, revoke, and one-use exhaustion. Manual QR proof below confirms the real app QR resolves to ciphertext only and decrypts locally to the expected Bundle. |

## Data Flow

The share panel creates one immutable `CycleIgSnapshot` from `LogsRepository.readAllLogs()`, the selected date range, and the checked categories. The same snapshot feeds preview counts, Bundle construction, encryption, shlep upload, QR rendering, copy/share/open, and Stop.

Sample data is loaded through normal Menstrudel storage with `LogsRepository.upsertLog`, keyed by date so rerunning the loader updates the synthetic dates instead of duplicating them. After inserts, `PeriodsRepository.recalculateAndAssignPeriods` rebuilds Menstrudel's own derived period rows. The verified sample contains 49 ordinary `period_logs` rows from 2026-01-02 through 2026-06-25: 28 bleeding days and 21 explicit no-flow logged days.

## Evidence

- [Share scope](screenshots/menstrudel/share-scope.png)
- [Sample loaded](screenshots/menstrudel/sample-loaded.png)
- [Preview counts](screenshots/menstrudel/share-preview.png)
- [Live QR](screenshots/menstrudel/share-qr.png)
- [Copy/share/open/stop controls](screenshots/menstrudel/share-controls.png)
- [Viewer opened](screenshots/menstrudel/viewer-open.png)
- [Viewer rendered](screenshots/menstrudel/viewer-rendered.png)
- [Share stopped](screenshots/menstrudel/share-stopped.png)

Real app QR verification from the screenshot:

- Decoded QR target was viewer-prefixed: `https://cycle.fhir.me/view#shlink:/...`.
- Decoded SHLink file host was `shlep.exe.xyz`; decoded `flag` was `U`; decoded payload included an `exp`.
- `GET <shlep file URL>?recipient=Menstrudel%20manual%20proof` returned 200 and a five-part compact JWE.
- The shlep file URL did not contain the decryption key.
- The fetched ciphertext did not contain `resourceType`, `menstrual-bleeding`, `sleep changes`, or the synthetic profile name.
- Local decryption from the QR fragment produced a FHIR `Bundle` with `type=collection`, 224 Observation entries, 28 menstrual bleeding `true` facts, 21 menstrual bleeding `false` facts, 49 flow facts, 91 symptom facts, and 35 app-native pain-level facts.
- The reference viewer rendered `decrypted · 224 resources` and a menstrual cycle review from the same QR link.
- After tapping Stop in Menstrudel, the stopped QR URL returned 404 `not_servable`.

## Verification

```sh
cd menstrudel/app
/home/jmandel/.cache/codex-flutter/bin/flutter pub get
/home/jmandel/.cache/codex-flutter/bin/flutter analyze --no-fatal-infos --no-fatal-warnings
/home/jmandel/.cache/codex-flutter/bin/flutter test
LIVE_CYCLE_IG=1 /home/jmandel/.cache/codex-flutter/bin/flutter test test/cycle_ig/cycle_ig_test.dart --plain-name 'live shlep create, resolve, decrypt, revoke, and max-use behavior'
/home/jmandel/.cache/codex-flutter/bin/flutter build apk --debug
adb install -r build/app/outputs/flutter-apk/app-debug.apk
```

Results:

- Focused Cycle IG tests passed: 4 non-live tests, with the live shlep test skipped by default.
- Full Flutter test suite passed: 42 tests passed, 1 gated live test skipped.
- Live shlep test passed with `LIVE_CYCLE_IG=1`.
- Analyzer had no errors; it still reports existing unrelated warnings/deprecations when warnings are non-fatal.
- Debug APK build passed and installed on `emulator-5554`.
- Android build needed a Gradle resolution pin for `androidx.glance:glance-appwidget:1.1.1` because `home_widget` declares `androidx.glance:glance-appwidget:1.+`, which currently floats to an alpha requiring compile SDK 37 and AGP 9.1.

## Limitations

- No Patient resource is emitted. Menstrudel stores a local profile name, but this share intentionally omits profile identity.
- No passcode or access-log UI is configured. shlep supports those controls, but this flow only advertises expiry, max-use, and explicit revoke.
- The active share manage token is kept in widget state, so the Stop control is available while the Data Management panel stays alive. If the user leaves that screen, shlep still enforces expiry and max-use exhaustion.
- The reference viewer did not auto-render immediately from the fragment in Chrome during the emulator run; it rendered after pressing the viewer's "Open link" button.
