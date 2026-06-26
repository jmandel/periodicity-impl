function encodeUtf8(value) {
  const binary = unescape(encodeURIComponent(value))
  const bytes = new Uint8Array(binary.length)

  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }

  return bytes
}

function decodeUtf8(bytes) {
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })

  return decodeURIComponent(escape(binary))
}

if (typeof global.TextEncoder === 'undefined') {
  global.TextEncoder = class TextEncoder {
    encode(value = '') {
      return encodeUtf8(String(value))
    }
  }
}

if (typeof global.TextDecoder === 'undefined') {
  global.TextDecoder = class TextDecoder {
    decode(value = new Uint8Array()) {
      return decodeUtf8(value)
    }
  }
}
