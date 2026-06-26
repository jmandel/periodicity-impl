# Cycle IG Implementation Plan

The Cycle FHIR Implementation Guide is the target data model for period tracking data in this workspace. The `cycle` submodule includes the IG source, documentation, an AI skill, and pointers to related specifications and repositories that should guide implementation. The `shlep` submodule provides supporting implementation tooling/reference code, and `https://shlep.exe.xyz/llms.txt` exposes a live server that can be used for integration work when desired. Agents working in this repo should start with these resources before changing any app.

## Goal

Work through each open source menstrual tracking app one by one and implement support for the Cycle IG. Each implementation should be tested until the feature works in the app, documented thoroughly, and reflected in this repository's `README.md`.

## Workflow

1. Study the Cycle IG materials in `cycle`, including the AI skill, documentation, examples, related specs, and linked repositories. Review `shlep` and the live `https://shlep.exe.xyz/llms.txt` endpoint when integration support would help.
2. Pick one app from the status table in `README.md`.
3. Inspect the app's existing data model, import/export flows, sharing flows, and test setup.
4. Implement Cycle IG support using the smallest changes that fit the app's architecture.
5. Test the implementation until it works:
   - Android apps: use the Android emulator and the app's existing automated test tooling where available.
   - Web apps: use headless Chromium and the app's existing automated test tooling where available.
6. Capture screenshots showing the implemented functionality in the running app.
7. Add a thorough implementation writeup for the app in this repo.
8. Update `README.md` with the app's implementation, testing, screenshots, and writeup status.
9. Move to the next app only after the current app has working support, evidence, and documentation.

## App Order

The initial app order is:

1. `drip`
2. `euki`
3. `menstrudel`
4. `ovumcy`

This order can change if setup or dependency issues make another app a better first target, but only one app should be active at a time.

## Writeups

For each completed app, add an implementation writeup to this repository that covers:

- Relevant app architecture and data model findings.
- How Cycle IG support was implemented.
- Any mappings between app-specific period tracking concepts and Cycle IG resources.
- Test commands, emulator/browser configuration, and results.
- Screenshot locations and what each screenshot demonstrates.
- Known limitations or follow-up work.
