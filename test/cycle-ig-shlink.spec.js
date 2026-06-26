import {
  bytesFromB64u,
  composeViewerLink,
  decryptCompact,
  encryptBundle,
  parseShlink,
} from '../lib/cycle-ig/shlink'

describe('Cycle IG SMART Health Link helpers', () => {
  it('encrypts a FHIR Bundle as compact JWE and decrypts it locally', () => {
    const plaintext = JSON.stringify({
      resourceType: 'Bundle',
      type: 'collection',
      entry: [],
    })

    const sealed = encryptBundle(plaintext)
    const [protectedHeader] = sealed.jwe.split('.')
    const header = JSON.parse(
      Buffer.from(protectedHeader, 'base64url').toString('utf8')
    )

    expect(header).toEqual({
      alg: 'dir',
      enc: 'A256GCM',
      cty: 'application/fhir+json',
    })
    expect(decryptCompact(sealed.jwe, sealed.key)).toBe(plaintext)
    expect(bytesFromB64u(sealed.keyB64)).toHaveLength(32)
  })

  it('composes a viewer-prefixed direct-file SHLink', () => {
    const link = composeViewerLink(
      'https://cycle.fhir.me/view/',
      'https://shlep.exe.xyz/shl/abc',
      'test-key',
      { flag: 'U', label: 'drip SMART Link', exp: 1782518400 }
    )
    const payload = parseShlink(link)

    expect(link.startsWith('https://cycle.fhir.me/view#shlink:/')).toBe(true)
    expect(payload).toEqual({
      url: 'https://shlep.exe.xyz/shl/abc',
      key: 'test-key',
      flag: 'U',
      label: 'drip SMART Link',
      exp: 1782518400,
      v: 1,
    })
  })
})
