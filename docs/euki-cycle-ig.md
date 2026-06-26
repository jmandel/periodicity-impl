# Euki Cycle IG implementation

## Checklist

| Item | Euki implementation |
| --- | --- |
| Share review screen | Cycle settings now includes "Share Cycle IG data": date range, category checkboxes, sample-data loader, preview counts, and live QR/link controls. Euki has local single-user data only, so the screen states that no separate account/person selector exists. |
| Layer 0 source | `CalendarItem.hasBleeding()` plus `include_cycle_summary`. Stored bleeding with `include_cycle_summary=true` becomes menstrual bleeding `true`; stored bleeding with `include_cycle_summary=false` becomes non-menstrual bleeding `false`. Missing days and untouched defaults emit no negative facts. |
| Layer 1 source | `BleedingSize` maps to menstrual flow when Layer 0 is true. Products, clots, emotions, body symptoms, and notes map as stored facts. Euki does not store basal body temperature or numeric 0-10 pain scores. |
| Intentional omissions | Sexual activity, contraception, STI/pregnancy tests, appointments, predictions, and derived cycle summaries are omitted. |
| Live SHLink host | Public `https://shlep.exe.xyz` direct-file share. Euki encrypts locally, uploads only compact JWE ciphertext, requests 7-day expiry, 5 max opens, and audit. The app stores the manage token in memory for stop sharing. |
| QR handoff widget | The QR, copy, native share, and open-viewer controls all use the same `https://cycle.fhir.me/view#shlink:/...` string. |
| Stop-sharing behavior | "Stop sharing" deletes the shlep share. The same QR URL returned `404 {"error":"not_servable","message":"not found"}` after stop. |
| Receiver path | The app opens the reference viewer. Chrome populated the SHLink field; pressing the viewer's "Open link" button decrypted and rendered the Bundle client-side. |
| Privacy and validation evidence | Unit tests cover snapshot scoping, Bundle shape/profile invariants, omitted sensitive fields, JWE round trip, and ciphertext-only upload. Live tests cover shlep create, resolve/decrypt, revoke, and one-use exhaustion. Manual QR proof below confirms the real app QR resolves to ciphertext only and decrypts locally to the expected Bundle. |

## Evidence

- [Share scope](screenshots/euki/share-scope.png)
- [Sample loaded](screenshots/euki/sample-loaded.png)
- [Share review](screenshots/euki/share-review.png)
- [Preview counts](screenshots/euki/share-preview.png)
- [QR only](screenshots/euki/share-qr.png)
- [QR and controls](screenshots/euki/share-controls.png)
- [Viewer opened](screenshots/euki/viewer-open.png)
- [Viewer rendered](screenshots/euki/viewer-rendered.png)
- [Share stopped](screenshots/euki/share-stopped.png)

## Verification

- `ANDROID_HOME=/home/jmandel/Android/Sdk ANDROID_SDK_ROOT=/home/jmandel/Android/Sdk ./gradlew testDebugUnitTest` passed.
- `LIVE_CYCLE_IG=1 ANDROID_HOME=/home/jmandel/Android/Sdk ANDROID_SDK_ROOT=/home/jmandel/Android/Sdk ./gradlew testDebugUnitTest --tests com.kollectivemobile.euki.cycleig.CycleIgTest.liveShlepCreateResolveRevokeAndMaxUse` passed.
- `ANDROID_HOME=/home/jmandel/Android/Sdk ANDROID_SDK_ROOT=/home/jmandel/Android/Sdk ./gradlew installDebug` installed the app on `emulator-5554`.
- QR decoded to a viewer-prefixed SHLink. Fetching the host URL returned compact JWE only; the host URL did not contain the key and the ciphertext did not contain `resourceType` or the sample note.
- Local decrypt of the QR payload produced a FHIR `Bundle` with `type=collection`, profile `https://cycle.fhir.me/StructureDefinition/period-tracking-bundle`, 212 entries, 28 menstrual bleeding `true` facts, and 3 non-menstrual bleeding `false` facts.
- The reference viewer rendered `decrypted · 212 resources` and a menstrual cycle review from the same QR link.
- After using "Stop sharing", the same shlep URL returned 404 `not_servable`.

## Notes

The share preview, normalization, encryption, QR, copy/share/open, and stop controls all use one `CycleIgSnapshot` created from the selected date/category scope. The deterministic sample contains 49 ordinary Euki `CalendarItem` rows from 2026-01-02 through 2026-06-22 and is loaded through `CalendarManager.saveItem`.
