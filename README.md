# Periodicity Implementation Workspace

This repository tracks work to add support for the Cycle FHIR Implementation Guide to open source menstrual tracking apps.

## Submodules

| Path | Repository | Branch | Role |
| --- | --- | --- | --- |
| `cycle` | `https://github.com/jmandel/cycle.git` | `main` | Cycle FHIR IG, documentation, AI skill, and implementation references |
| `shlep` | `https://github.com/jmandel/shlep.git` | `main` | Supporting implementation tooling/reference code |
| `drip` | `https://gitlab.com/bloodyhealth/drip.git` | `main` | Android app |
| `euki` | `https://github.com/Euki-Inc/Euki-Android.git` | `main` | Android app |
| `menstrudel` | `https://github.com/J-shw/Menstrudel.git` | `dev` | Flutter app |
| `ovumcy` | `https://github.com/ovumcy/ovumcy-web.git` | `main` | Web app |

## Status

| Project | Type | Cycle IG support | Tests | Screenshots | Writeup |
| --- | --- | --- | --- | --- | --- |
| `drip` | Android | Implemented in Settings/Data management with live shlep QR | Focused Jest, live shlep smoke, lint, Cycle IG check, Android build | [docs/screenshots/drip](docs/screenshots/drip/) | [docs/drip-cycle-ig.md](docs/drip-cycle-ig.md) |
| `euki` | Android | Implemented in Cycle settings with live shlep QR | JVM unit tests, live shlep smoke, Android install | [docs/screenshots/euki](docs/screenshots/euki/) | [docs/euki-cycle-ig.md](docs/euki-cycle-ig.md) |
| `menstrudel` | Flutter/Android | Implemented in Data Management with live shlep QR | Flutter tests, live shlep smoke, analyze, Android debug build | [docs/screenshots/menstrudel](docs/screenshots/menstrudel/) | [docs/menstrudel-cycle-ig.md](docs/menstrudel-cycle-ig.md) |
| `ovumcy` | Web | Implemented in Settings/Data with live shlep QR | Go tests, frontend lint/unit/build, live shlep smoke, browser proof | [docs/screenshots/ovumcy](docs/screenshots/ovumcy/) | [docs/ovumcy-cycle-ig.md](docs/ovumcy-cycle-ig.md) |

See `plan.md` for the implementation workflow.

## Landing page

A React (Vite) landing page in [`site/`](site/) showcases the per-app demo videos and articulates the implementation choices, techniques, and tips, with deep links into this repository and `cycle.fhir.me`. It is built and deployed to GitHub Pages by the [`Deploy site`](.github/workflows/deploy-site.yml) workflow.

```sh
cd site
npm install
npm run dev    # local preview; syncs videos/screenshots from docs/
npm run build  # production build into site/dist/
```

The heavy media (videos and screenshots) is synced from `docs/` at build time by `site/scripts/copy-assets.mjs`, so the canonical sources stay under `docs/`.
