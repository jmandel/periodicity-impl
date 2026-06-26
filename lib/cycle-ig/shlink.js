import 'react-native-get-random-values'
import { AES } from '@stablelib/aes'
import { GCM } from '@stablelib/gcm'
import { encode as b64Encode, decode as b64Decode } from 'base-64'

const JWE_CONTENT_TYPE = 'application/fhir+json'

export function randomBytes(length) {
  const bytes = new Uint8Array(length)
  global.crypto.getRandomValues(bytes)
  return bytes
}

export function b64uFromBytes(bytes) {
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })

  return b64Encode(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}

export function bytesFromB64u(value) {
  const b64 = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = b64.padEnd(b64.length + ((4 - (b64.length % 4)) % 4), '=')
  const binary = b64Decode(padded)
  const bytes = new Uint8Array(binary.length)

  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }

  return bytes
}

export function encryptBundle(plaintext) {
  const key = randomBytes(32)

  return {
    jwe: encryptCompact(plaintext, key),
    key,
    keyB64: b64uFromBytes(key),
  }
}

export function encryptCompact(plaintext, keyBytes) {
  if (keyBytes.length !== 32) {
    throw new Error(`A256GCM key must be 32 bytes, got ${keyBytes.length}`)
  }

  const header = {
    alg: 'dir',
    enc: 'A256GCM',
    cty: JWE_CONTENT_TYPE,
  }
  const protectedB64 = b64uFromBytes(utf8Encode(JSON.stringify(header)))
  const nonce = randomBytes(12)
  const gcm = new GCM(new AES(keyBytes))
  const sealed = gcm.seal(
    nonce,
    utf8Encode(plaintext),
    utf8Encode(protectedB64)
  )
  const cipherText = sealed.slice(0, sealed.length - 16)
  const tag = sealed.slice(sealed.length - 16)

  return [
    protectedB64,
    '',
    b64uFromBytes(nonce),
    b64uFromBytes(cipherText),
    b64uFromBytes(tag),
  ].join('.')
}

export function decryptCompact(jwe, keyBytes) {
  const [protectedB64, , nonceB64, cipherTextB64, tagB64] = jwe.trim().split('.')
  if (!protectedB64 || !nonceB64 || !cipherTextB64 || tagB64 == null) {
    throw new Error('malformed compact JWE')
  }

  const sealed = concatBytes(bytesFromB64u(cipherTextB64), bytesFromB64u(tagB64))
  const gcm = new GCM(new AES(keyBytes))
  const plaintext = gcm.open(
    bytesFromB64u(nonceB64),
    sealed,
    utf8Encode(protectedB64)
  )

  if (!plaintext) throw new Error('JWE authentication failed')

  return utf8Decode(plaintext)
}

export function composeShlink(fileUrl, keyB64, options = {}) {
  const payload = {
    url: fileUrl,
    key: keyB64,
  }

  if (options.flag) payload.flag = normalizeFlag(options.flag)
  if (options.label) payload.label = options.label
  if (options.exp) payload.exp = options.exp
  payload.v = 1

  return `shlink:/${b64uFromBytes(utf8Encode(JSON.stringify(payload)))}`
}

export function composeViewerLink(viewerBase, fileUrl, keyB64, options = {}) {
  return `${viewerBase.replace(/\/+$/, '')}#${composeShlink(
    fileUrl,
    keyB64,
    options
  )}`
}

export function parseShlink(input) {
  const index = input.indexOf('shlink:/')
  if (index < 0) throw new Error('no shlink:/ found')

  const payload = input.slice(index + 'shlink:/'.length)
  return JSON.parse(utf8Decode(bytesFromB64u(payload)))
}

function utf8Encode(value) {
  const binary = unescape(encodeURIComponent(value))
  const bytes = new Uint8Array(binary.length)

  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }

  return bytes
}

function utf8Decode(bytes) {
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })

  return decodeURIComponent(escape(binary))
}

function concatBytes(first, second) {
  const bytes = new Uint8Array(first.length + second.length)
  bytes.set(first)
  bytes.set(second, first.length)
  return bytes
}

function normalizeFlag(flag) {
  const allowed = ['L', 'P', 'U']
  const selected = new Set()

  flag.split('').forEach((char) => {
    if (!allowed.includes(char)) {
      throw new Error(`invalid SHL flag "${char}"`)
    }
    selected.add(char)
  })

  if (selected.has('U') && selected.has('P')) {
    throw new Error('SHL flags U and P are mutually exclusive')
  }

  return allowed.filter((char) => selected.has(char)).join('')
}
