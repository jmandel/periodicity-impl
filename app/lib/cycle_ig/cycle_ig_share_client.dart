import 'dart:convert';
import 'dart:io';

import 'package:menstrudel/cycle_ig/cycle_ig_bundle_builder.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_share.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_snapshot.dart';
import 'package:menstrudel/cycle_ig/shlink_crypto.dart';
import 'package:menstrudel/cycle_ig/smart_health_link.dart';

class CycleIgHttpResponse {
  final int statusCode;
  final String body;

  const CycleIgHttpResponse(this.statusCode, this.body);

  bool get isOk => statusCode >= 200 && statusCode < 300;
}

abstract class CycleIgTransport {
  Future<CycleIgHttpResponse> postJson(
    Uri uri,
    Map<String, dynamic> body, {
    Map<String, String> headers = const {},
  });

  Future<CycleIgHttpResponse> delete(
    Uri uri, {
    Map<String, String> headers = const {},
  });

  Future<CycleIgHttpResponse> get(
    Uri uri, {
    Map<String, String> headers = const {},
  });
}

class CycleIgHttpTransport implements CycleIgTransport {
  final HttpClient _client;

  CycleIgHttpTransport({HttpClient? client}) : _client = client ?? HttpClient();

  @override
  Future<CycleIgHttpResponse> postJson(
    Uri uri,
    Map<String, dynamic> body, {
    Map<String, String> headers = const {},
  }) async {
    final request = await _client.postUrl(uri);
    request.headers.contentType = ContentType.json;
    headers.forEach(request.headers.set);
    request.write(jsonEncode(body));
    return _read(await request.close());
  }

  @override
  Future<CycleIgHttpResponse> delete(
    Uri uri, {
    Map<String, String> headers = const {},
  }) async {
    final request = await _client.deleteUrl(uri);
    headers.forEach(request.headers.set);
    return _read(await request.close());
  }

  @override
  Future<CycleIgHttpResponse> get(
    Uri uri, {
    Map<String, String> headers = const {},
  }) async {
    final request = await _client.getUrl(uri);
    headers.forEach(request.headers.set);
    return _read(await request.close());
  }

  Future<CycleIgHttpResponse> _read(HttpClientResponse response) async {
    final body = await utf8.decodeStream(response);
    return CycleIgHttpResponse(response.statusCode, body);
  }
}

class CycleIgShareClient {
  final Uri baseUri;
  final String viewerBase;
  final CycleIgTransport transport;

  CycleIgShareClient({
    String baseUrl = 'https://shlep.exe.xyz',
    this.viewerBase = 'https://cycle.fhir.me/view',
    CycleIgTransport? transport,
  }) : baseUri = Uri.parse(baseUrl),
       transport = transport ?? CycleIgHttpTransport();

  Future<CycleIgShare> createShare(
    CycleIgSnapshot snapshot, {
    int maxUses = 5,
    Duration expiresIn = const Duration(days: 7),
  }) async {
    final keyBytes = ShlinkCrypto.randomBytes(32);
    final keyBase64Url = ShlinkCrypto.base64UrlNoPadding(keyBytes);
    final ciphertext = await ShlinkCrypto.encryptFhirJson(
      CycleIgBundleBuilder.encode(snapshot),
      keyBytes,
    );
    final expiresAt = DateTime.now().toUtc().add(expiresIn);
    final exp = expiresAt.millisecondsSinceEpoch ~/ 1000;
    final response = await transport.postJson(
      baseUri.replace(path: '/shares'),
      {
        'ciphertext': ciphertext,
        'contentType': 'application/fhir+json',
        'policy': {'exp': exp, 'maxUses': maxUses},
      },
    );

    if (!response.isOk) {
      throw HttpException(
        'shlep create failed: ${response.statusCode} ${response.body}',
      );
    }

    final body = jsonDecode(response.body) as Map<String, dynamic>;
    final fileUrl = body['fileUrl'] as String;
    final shlink = SmartHealthLink(
      url: fileUrl,
      key: keyBase64Url,
      flag: 'U',
      label: 'Menstrudel SMART Link',
      exp: exp,
    );

    return CycleIgShare(
      id: body['id'] as String,
      fileUrl: fileUrl,
      manageToken: body['manageToken'] as String,
      viewerLink: shlink.toViewerLink(viewerBase: viewerBase),
      bareShlink: shlink.toBareShlink(),
      keyBase64Url: keyBase64Url,
      ciphertext: ciphertext,
      expiresAt: expiresAt,
      maxUses: maxUses,
      snapshot: snapshot,
    );
  }

  Future<void> revoke(CycleIgShare share) async {
    final response = await transport.delete(
      baseUri.replace(path: '/shares/${share.id}'),
      headers: {'authorization': 'Bearer ${share.manageToken}'},
    );
    if (!response.isOk) {
      throw HttpException(
        'shlep revoke failed: ${response.statusCode} ${response.body}',
      );
    }
  }

  Future<String> resolveCiphertext(
    String fileUrl, {
    String recipient = 'Cycle IG test',
  }) async {
    final uri = Uri.parse(
      fileUrl,
    ).replace(queryParameters: {'recipient': recipient});
    final response = await transport.get(uri);
    if (!response.isOk) {
      throw HttpException(
        'shlep resolve failed: ${response.statusCode} ${response.body}',
      );
    }
    return response.body;
  }
}
