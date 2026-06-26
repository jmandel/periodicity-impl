import 'dart:convert';
import 'dart:math';

import 'package:cryptography/cryptography.dart';

class ShlinkCrypto {
  static final Random _secureRandom = Random.secure();

  static List<int> randomBytes(int length) {
    return List<int>.generate(length, (_) => _secureRandom.nextInt(256));
  }

  static String base64UrlNoPadding(List<int> bytes) {
    return base64Url.encode(bytes).replaceAll('=', '');
  }

  static List<int> decodeBase64UrlNoPadding(String value) {
    final padding = '=' * ((4 - value.length % 4) % 4);
    return base64Url.decode('$value$padding');
  }

  static Future<String> encryptFhirJson(
    String plaintext,
    List<int> keyBytes,
  ) async {
    if (keyBytes.length != 32) {
      throw ArgumentError('A256GCM requires a 32-byte key.');
    }

    final protectedHeader = base64UrlNoPadding(
      utf8.encode(
        jsonEncode({
          'alg': 'dir',
          'enc': 'A256GCM',
          'cty': 'application/fhir+json',
        }),
      ),
    );
    final nonce = randomBytes(12);
    final secretKey = SecretKey(keyBytes);
    final box = await AesGcm.with256bits().encrypt(
      utf8.encode(plaintext),
      secretKey: secretKey,
      nonce: nonce,
      aad: ascii.encode(protectedHeader),
    );

    return [
      protectedHeader,
      '',
      base64UrlNoPadding(nonce),
      base64UrlNoPadding(box.cipherText),
      base64UrlNoPadding(box.mac.bytes),
    ].join('.');
  }

  static Future<String> decryptFhirJson(
    String compactJwe,
    List<int> keyBytes,
  ) async {
    final parts = compactJwe.split('.');
    if (parts.length != 5) {
      throw FormatException('Expected a five-part compact JWE.');
    }

    final plaintext = await AesGcm.with256bits().decrypt(
      SecretBox(
        decodeBase64UrlNoPadding(parts[3]),
        nonce: decodeBase64UrlNoPadding(parts[2]),
        mac: Mac(decodeBase64UrlNoPadding(parts[4])),
      ),
      secretKey: SecretKey(keyBytes),
      aad: ascii.encode(parts[0]),
    );
    return utf8.decode(plaintext);
  }
}
