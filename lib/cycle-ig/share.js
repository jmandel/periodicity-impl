import transformToCycleFhirJson from './export'
import { composeViewerLink, encryptBundle } from './shlink'

export const SHLEP_BASE_URL = 'https://shlep.exe.xyz'
export const CYCLE_VIEWER_URL = 'https://cycle.fhir.me/view'
export const CYCLE_SHARE_MAX_USES = 5
export const CYCLE_SHARE_DAYS = 7

export async function createCycleIgShare(cycleDays, options = {}) {
  const shlepBaseUrl = options.shlepBaseUrl || SHLEP_BASE_URL
  const viewerUrl = options.viewerUrl || CYCLE_VIEWER_URL
  const fetchImpl = options.fetchImpl || fetch
  const now = options.now || new Date()
  const exp =
    options.exp ||
    Math.floor(now.getTime() / 1000) + CYCLE_SHARE_DAYS * 24 * 60 * 60
  const bundleJson =
    options.bundleJson ||
    transformToCycleFhirJson(cycleDays, {
      exportedAt: options.exportedAt,
      ...(options.bundle || {}),
    })
  const sealed = options.sealed || encryptBundle(bundleJson)

  const response = await fetchImpl(`${shlepBaseUrl.replace(/\/+$/, '')}/shares`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
    },
    body: JSON.stringify({
      ciphertext: sealed.jwe,
      contentType: 'application/fhir+json',
      policy: {
        exp,
        maxUses: options.maxUses || CYCLE_SHARE_MAX_USES,
        audit: true,
      },
    }),
  })

  if (!response.ok) {
    throw new Error(`shlep share creation failed: ${response.status}`)
  }

  const share = await response.json()
  const viewerLink = composeViewerLink(viewerUrl, share.fileUrl, sealed.keyB64, {
    flag: 'U',
    label: 'drip SMART Link',
    exp,
  })

  return {
    id: share.id,
    fileUrl: share.fileUrl,
    manageToken: share.manageToken,
    viewerLink,
    shlinkKey: sealed.keyB64,
    exp,
    maxUses: options.maxUses || CYCLE_SHARE_MAX_USES,
    bundleJson,
    jwe: sealed.jwe,
  }
}

export async function revokeCycleIgShare(share, options = {}) {
  const shlepBaseUrl = options.shlepBaseUrl || SHLEP_BASE_URL
  const fetchImpl = options.fetchImpl || fetch
  const response = await fetchImpl(
    `${shlepBaseUrl.replace(/\/+$/, '')}/shares/${share.id}`,
    {
      method: 'DELETE',
      headers: {
        authorization: `Bearer ${share.manageToken}`,
      },
    }
  )

  if (!response.ok) {
    throw new Error(`shlep share revocation failed: ${response.status}`)
  }

  return response.json()
}

export async function getCycleIgShareStatus(share, options = {}) {
  const shlepBaseUrl = options.shlepBaseUrl || SHLEP_BASE_URL
  const fetchImpl = options.fetchImpl || fetch
  const response = await fetchImpl(
    `${shlepBaseUrl.replace(/\/+$/, '')}/shares/${share.id}`,
    {
      method: 'GET',
      headers: {
        authorization: `Bearer ${share.manageToken}`,
      },
    }
  )

  if (!response.ok) {
    throw new Error(`shlep share status failed: ${response.status}`)
  }

  return response.json()
}
