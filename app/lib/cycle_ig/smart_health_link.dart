import 'dart:convert';

import 'package:menstrudel/cycle_ig/shlink_crypto.dart';

class SmartHealthLink {
  final String url;
  final String key;
  final String flag;
  final String? label;
  final int? exp;
  final int v;

  const SmartHealthLink({
    required this.url,
    required this.key,
    this.flag = 'U',
    this.label,
    this.exp,
    this.v = 1,
  });

  Map<String, dynamic> toPayload() {
    return {
      'url': url,
      'key': key,
      'flag': flag,
      if (label != null) 'label': label,
      if (exp != null) 'exp': exp,
      'v': v,
    };
  }

  String toBareShlink() {
    final encoded = ShlinkCrypto.base64UrlNoPadding(
      utf8.encode(jsonEncode(toPayload())),
    );
    return 'shlink:/$encoded';
  }

  String toViewerLink({String viewerBase = 'https://cycle.fhir.me/view'}) {
    return '$viewerBase#${toBareShlink()}';
  }

  static SmartHealthLink parse(String input) {
    final marker = input.indexOf('shlink:/');
    if (marker < 0) {
      throw FormatException('No shlink:/ fragment found.');
    }
    final encoded = input.substring(marker + 'shlink:/'.length);
    final payload =
        jsonDecode(utf8.decode(ShlinkCrypto.decodeBase64UrlNoPadding(encoded)))
            as Map<String, dynamic>;

    return SmartHealthLink(
      url: payload['url'] as String,
      key: payload['key'] as String,
      flag: payload['flag'] as String? ?? '',
      label: payload['label'] as String?,
      exp: payload['exp'] as int?,
      v: payload['v'] as int? ?? 1,
    );
  }
}
