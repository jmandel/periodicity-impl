import { createCycleIgShare, revokeCycleIgShare } from '../lib/cycle-ig/share'
import { bytesFromB64u, decryptCompact, parseShlink } from '../lib/cycle-ig/shlink'
import { createCycleIgSnapshot } from '../lib/cycle-ig/snapshot'
import sampleCycleDays from './fixtures/cycle-ig-sample'

const describeLive =
  process.env.LIVE_CYCLE_IG === '1' ? describe : describe.skip

describeLive('Cycle IG live shlep smoke', () => {
  it('creates, resolves, decrypts, and revokes a live direct-file SHLink', async () => {
    const snapshot = createCycleIgSnapshot(sampleCycleDays, {
      startDate: '2026-01-02',
      endDate: '2026-03-02',
    })
    const share = await createCycleIgShare(snapshot.cycleDays, {
      exportedAt: '2026-06-25T12:00:00.000Z',
      maxUses: 5,
    })

    try {
      const payload = parseShlink(share.viewerLink)
      const response = await fetch(`${payload.url}?recipient=drip-live-smoke`)
      expect(response.ok).toBe(true)
      expect(response.headers.get('content-type')).toContain('application/jose')

      const plaintext = decryptCompact(
        (await response.text()).trim(),
        bytesFromB64u(payload.key)
      )
      const bundle = JSON.parse(plaintext)
      expect(bundle.resourceType).toBe('Bundle')
      expect(bundle.meta.profile).toContain(
        'https://cycle.fhir.me/StructureDefinition/period-tracking-bundle'
      )
      expect(plaintext).toContain('Synthetic sample cycle 1 day 1.')
    } finally {
      await revokeCycleIgShare(share)
    }

    const revokedPayload = parseShlink(share.viewerLink)
    const revokedResponse = await fetch(
      `${revokedPayload.url}?recipient=drip-live-smoke-after-revoke`
    )
    expect(revokedResponse.status).toBe(404)
  }, 20000)

  it('enforces the advertised max-use policy', async () => {
    const snapshot = createCycleIgSnapshot(sampleCycleDays, {
      startDate: '2026-01-02',
      endDate: '2026-03-02',
    })
    const share = await createCycleIgShare(snapshot.cycleDays, {
      exportedAt: '2026-06-25T12:00:00.000Z',
      maxUses: 1,
    })

    try {
      const payload = parseShlink(share.viewerLink)
      const firstResponse = await fetch(
        `${payload.url}?recipient=drip-live-smoke-max-use-1`
      )
      expect(firstResponse.ok).toBe(true)

      const secondResponse = await fetch(
        `${payload.url}?recipient=drip-live-smoke-max-use-2`
      )
      expect(secondResponse.status).toBe(404)
    } finally {
      await revokeCycleIgShare(share)
    }
  }, 20000)
})
