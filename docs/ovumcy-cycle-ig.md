# Ovumcy Cycle IG implementation

## Checklist

| Item | Ovumcy implementation |
| --- | --- |
| Share review screen | Settings > Data now includes "Cycle IG SMART Health Link" with the current authenticated account scope, shared export date range inputs, category checkboxes, a sample-data loader, preview counts, and live QR/link controls. Ovumcy is an account-backed web app, so the share is for the logged-in owner account. |
| Layer 0 source | Stored `DailyLog.IsPeriod` is the source. A stored row with `IsPeriod=true` becomes menstrual bleeding `true`; a stored row with `IsPeriod=false` becomes menstrual bleeding `false`. Missing dates and absent rows emit no negative facts. |
| Layer 1 source | Period-day `DailyLog.Flow` maps to Menstrual Flow. Stored `SymptomIDs` map to Symptom observations with exact SNOMED coding where appropriate plus app-native coding. Stored BBT maps to Basal Body Temperature. Cervical mucus, mood rating, cycle factors, and notes map as app-native Period Tracking Facts. |
| Intentional omissions | Sex activity, pregnancy tests, cycle-start flags, uncertainty flags, predictions, derived dashboard/statistics values, account identity, settings defaults, and missing days are omitted. Flow on `IsPeriod=false` rows is omitted because Ovumcy normalization forces `FlowNone` for non-period rows. |
| Live SHLink host | The Ovumcy backend creates a public `https://shlep.exe.xyz` direct-file share. It uploads only compact JWE ciphertext, requests 7-day expiry, 5 max opens, and audit, and keeps the manage token only in the rendered Stop form. |
| QR handoff widget | The QR, copy, native share, and open controls all use the same `https://cycle.fhir.me/view#shlink:/...` string. |
| Stop-sharing behavior | "Stop sharing" deletes the shlep share with the manage token. The same decoded QR URL returned 404 after Stop. Expiry and max-use exhaustion are also enforced by shlep. |
| Receiver path | The Open control targets the reference viewer. Chrome populated the SHLink field; pressing the viewer's "Open link" button decrypted and rendered the Bundle client-side. |
| Privacy and validation evidence | Service tests cover Bundle mapping, unsupported-field omissions, JWE round trip, SHLink parsing, fake-host create/resolve/revoke/max-use, and a gated public shlep test. An API regression test covers the server-rendered QR data URL. Manual QR proof below confirms the real app QR resolves to ciphertext only and decrypts locally to the expected Bundle. |

## Data Flow

The share flow is backend-mediated because Ovumcy is a server-rendered web app with the database on the server. On Create, the server builds one immutable `CycleIGSnapshot` from `DailyLog`, symptoms, settings, date range, and category scope. That same snapshot feeds the share summary, Bundle construction, encryption, shlep upload, QR rendering, copy/share/open link, and Stop control.

The Preview button is advisory. The created share card shows the summary from the exact snapshot that was encrypted and uploaded.

Sample data is loaded through `DayService.UpsertDayEntry`, not by bypassing app storage. The verified sample contains 49 ordinary `daily_logs` rows from 2026-01-02 through 2026-06-25: 28 bleeding days and 21 explicit non-period logged days.

## Evidence

- [Share scope](screenshots/ovumcy/share-scope.png)
- [Sample loaded](screenshots/ovumcy/sample-loaded.png)
- [Preview counts](screenshots/ovumcy/share-preview.png)
- [Live QR](screenshots/ovumcy/share-qr.png)
- [Copy/share/open/stop controls](screenshots/ovumcy/share-controls.png)
- [Viewer opened](screenshots/ovumcy/viewer-open.png)
- [Viewer rendered](screenshots/ovumcy/viewer-rendered.png)
- [Share stopped](screenshots/ovumcy/share-stopped.png)

Real app QR verification from the screenshot:

- Decoded QR target was viewer-prefixed: `https://cycle.fhir.me/view#shlink:/...`.
- Decoded SHLink file host was `shlep.exe.xyz`; decoded `flag` was `U`; decoded payload included `v=1`, a 43-character key, and an `exp`.
- `GET <shlep file URL>?recipient=Ovumcy%20manual%20proof` returned 200 with `application/jose`.
- The shlep file URL did not contain the decryption key.
- The fetched ciphertext did not contain `resourceType`, `menstrual-bleeding`, `Synthetic Cycle IG sample`, or the test account email.
- Local decryption from the QR fragment produced a FHIR `Bundle` with 350 Observation resources: 28 menstrual bleeding `true` facts, 21 menstrual bleeding `false` facts, 28 flow facts, 119 symptom facts, and 21 BBT facts.
- The reference viewer rendered `decrypted · 350 resources` and a menstrual cycle review from the same QR link.
- After pressing Stop sharing in Ovumcy, the same shlep URL returned 404.

## Verification

```sh
cd ovumcy
npm run build:js
npm run build:css
go test ./internal/services -run CycleIG -count=1
go test ./internal/api -run TestCycleIGShareResponseRendersQRDataURL -count=1
LIVE_CYCLE_IG=1 go test ./internal/services -run TestCycleIGLiveShlepCreateResolveDecryptRevokeAndMaxUse -count=1
go test ./...
npm run lint
npm run test:unit
```

Results:

- Focused Cycle IG service tests passed.
- Focused API QR rendering regression passed.
- Live public shlep test passed with create, resolve, decrypt, revoke, and max-use checks.
- Full Go test suite passed.
- JS build, CSS build, ESLint, and JS unit tests passed.
- Browser proof against the local Ovumcy backend and public shlep passed; QR scan/decode, ciphertext fetch, local decrypt, viewer render, and revoke all succeeded.

## Limitations

- No Patient resource is emitted; account identity is intentionally omitted.
- No passcode or access-log UI is shown. shlep supports those controls, but this flow only advertises expiry, max-use, and explicit revoke.
- The Stop manage token is not persisted in Ovumcy. If the user reloads after minting a share, the explicit Stop control for that share is gone, while shlep still enforces expiry and max-use exhaustion.
- Ovumcy's own backend necessarily sees its source database and plaintext Bundle during share creation. The privacy boundary verified here is the SHLink host: shlep receives only ciphertext, and the decryption key stays in the viewer link fragment.
