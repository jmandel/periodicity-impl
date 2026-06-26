import { createCycleIgShare } from '../lib/cycle-ig/share'
import { bytesFromB64u, decryptCompact, parseShlink } from '../lib/cycle-ig/shlink'
import sampleCycleDays from './fixtures/cycle-ig-sample'

describe('Cycle IG shlep share creation', () => {
  it('uploads only ciphertext and returns a viewer-prefixed SHLink', async () => {
    const fetchImpl = jest.fn().mockResolvedValue({
      ok: true,
      json: () =>
        Promise.resolve({
          id: 'share-id',
          fileUrl: 'https://shlep.exe.xyz/shl/share-id',
          manageToken: 'manage-token',
        }),
    })

    const result = await createCycleIgShare(sampleCycleDays, {
      fetchImpl,
      now: new Date('2026-06-25T12:00:00.000Z'),
    })
    const [, request] = fetchImpl.mock.calls[0]
    const body = JSON.parse(request.body)

    expect(fetchImpl).toHaveBeenCalledWith(
      'https://shlep.exe.xyz/shares',
      expect.objectContaining({ method: 'POST' })
    )
    expect(body.contentType).toBe('application/fhir+json')
    expect(body.policy).toEqual({
      exp: 1782993600,
      maxUses: 5,
      audit: true,
    })
    expect(body.ciphertext).not.toContain('Synthetic sample cycle 1 day 1')
    expect(result.viewerLink.startsWith('https://cycle.fhir.me/view#shlink:/')).toBe(
      true
    )

    const payload = parseShlink(result.viewerLink)
    expect(payload.url).toBe('https://shlep.exe.xyz/shl/share-id')
    expect(payload.flag).toBe('U')
    expect(payload.key).toBe(result.shlinkKey)

    const plaintext = decryptCompact(result.jwe, bytesFromB64u(result.shlinkKey))
    expect(JSON.parse(plaintext).resourceType).toBe('Bundle')
  })
})
