import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_bundle_builder.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_sample_data.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_scope.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_share.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_share_client.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_snapshot.dart';
import 'package:menstrudel/cycle_ig/shlink_crypto.dart';
import 'package:menstrudel/cycle_ig/smart_health_link.dart';

void main() {
  CycleIgSnapshot sampleSnapshot() {
    final logs = CycleIgSampleData.syntheticLogs(today: DateTime(2026, 6, 25));
    final scope = CycleIgScope.forLogs(logs);
    return CycleIgSnapshot.fromLogs(
      logs,
      scope,
      generatedAt: DateTime.utc(2026, 6, 25, 12),
    );
  }

  group('Cycle IG sample and mapping', () {
    test('creates a scoped synthetic sample from stored Menstrudel facts', () {
      final snapshot = sampleSnapshot();

      expect(snapshot.logs.length, 49);
      expect(snapshot.bleedingTrueCount, 28);
      expect(snapshot.bleedingFalseCount, 21);
      expect(snapshot.flowFactCount, 49);
      expect(snapshot.symptomFactCount, 91);
      expect(snapshot.painFactCount, 35);
      expect(snapshot.observationCount, 224);
    });

    test(
      'builds a Period Tracking Bundle without predictions or unrelated data',
      () {
        final bundle = CycleIgBundleBuilder.build(sampleSnapshot());
        final entries = bundle['entry'] as List;
        final json = jsonEncode(bundle);

        expect(bundle['resourceType'], 'Bundle');
        expect(bundle['type'], 'collection');
        expect(entries.length, 224);
        expect(
          json,
          contains(
            'https://cycle.fhir.me/StructureDefinition/period-tracking-bundle',
          ),
        );
        expect(json, contains('"code":"menstrual-bleeding"'));
        expect(json, contains('"valueBoolean":true'));
        expect(json, contains('"valueBoolean":false'));
        expect(json, contains('"code":"flow-none"'));
        expect(json, contains('"code":"flow-heavy"'));
        expect(json, contains('"code":"431416001"'));
        expect(json, contains('"code":"pain-level-severe"'));

        expect(json, isNot(contains('72514-3')));
        expect(json, isNot(contains('period_id')));
        expect(json, isNot(contains('prediction')));
        expect(json, isNot(contains('sexual')));
        expect(json, isNot(contains('sanitary')));
        expect(json, isNot(contains('contraceptive')));
      },
    );
  });

  group('SMART Health Link crypto', () {
    test('round-trips compact JWE and viewer-prefixed SHLink', () async {
      final plaintext = CycleIgBundleBuilder.encode(sampleSnapshot());
      final keyBytes = List<int>.filled(32, 7);
      final compactJwe = await ShlinkCrypto.encryptFhirJson(
        plaintext,
        keyBytes,
      );

      expect(compactJwe.split('.'), hasLength(5));
      expect(
        await ShlinkCrypto.decryptFhirJson(compactJwe, keyBytes),
        plaintext,
      );

      final shlink = SmartHealthLink(
        url: 'https://shlep.example/shl/abc',
        key: ShlinkCrypto.base64UrlNoPadding(keyBytes),
        flag: 'U',
        label: 'Menstrudel SMART Link',
        exp: 1782446400,
      );
      final viewerLink = shlink.toViewerLink();
      final parsed = SmartHealthLink.parse(viewerLink);

      expect(viewerLink, startsWith('https://cycle.fhir.me/view#shlink:/'));
      expect(parsed.url, shlink.url);
      expect(parsed.key, shlink.key);
      expect(parsed.flag, 'U');
      expect(parsed.exp, 1782446400);
    });
  });

  group('shlep client', () {
    test(
      'uploads only ciphertext and uses the same viewer-prefixed link for QR/share',
      () async {
        final fakeTransport = _FakeTransport();
        final client = CycleIgShareClient(
          baseUrl: 'https://shlep.example',
          transport: fakeTransport,
        );

        final share = await client.createShare(sampleSnapshot());
        final createdBody = fakeTransport.createdBody!;
        final createdJson = jsonEncode(createdBody);
        final parsed = SmartHealthLink.parse(share.viewerLink);

        expect(createdBody['ciphertext'], share.ciphertext);
        expect(createdBody['contentType'], 'application/fhir+json');
        expect(createdBody['policy']['maxUses'], 5);
        expect(createdBody['policy']['exp'], isA<int>());
        expect(createdJson, isNot(contains('resourceType')));
        expect(createdJson, isNot(contains('menstrual-bleeding')));
        expect(
          share.viewerLink,
          startsWith('https://cycle.fhir.me/view#shlink:/'),
        );
        expect(share.bareShlink, startsWith('shlink:/'));
        expect(parsed.url, 'https://shlep.example/shl/test-share');
        expect(parsed.key, share.keyBase64Url);

        await client.revoke(share);
        expect(
          fakeTransport.deletedUri?.toString(),
          'https://shlep.example/shares/test-share',
        );
        expect(
          fakeTransport.deletedHeaders['authorization'],
          'Bearer test-manage-token',
        );
      },
    );

    test(
      'live shlep create, resolve, decrypt, revoke, and max-use behavior',
      () async {
        final client = CycleIgShareClient();
        final snapshot = sampleSnapshot();
        CycleIgShare? share;
        CycleIgShare? oneUseShare;

        try {
          share = await client.createShare(snapshot);
          final ciphertext = await client.resolveCiphertext(
            share.fileUrl,
            recipient: 'Menstrudel automated test',
          );
          final decrypted = await ShlinkCrypto.decryptFhirJson(
            ciphertext,
            ShlinkCrypto.decodeBase64UrlNoPadding(share.keyBase64Url),
          );
          final bundle = jsonDecode(decrypted) as Map<String, dynamic>;
          expect(bundle['resourceType'], 'Bundle');
          expect((bundle['entry'] as List).length, 224);

          await client.revoke(share);
          await expectLater(
            client.resolveCiphertext(
              share.fileUrl,
              recipient: 'Menstrudel automated test',
            ),
            throwsA(isA<HttpException>()),
          );
          share = null;

          oneUseShare = await client.createShare(snapshot, maxUses: 1);
          expect(
            await client.resolveCiphertext(
              oneUseShare.fileUrl,
              recipient: 'Menstrudel automated test',
            ),
            isNotEmpty,
          );
          await expectLater(
            client.resolveCiphertext(
              oneUseShare.fileUrl,
              recipient: 'Menstrudel automated test',
            ),
            throwsA(isA<HttpException>()),
          );
        } finally {
          if (share != null) {
            await client.revoke(share).catchError((_) {});
          }
          if (oneUseShare != null) {
            await client.revoke(oneUseShare).catchError((_) {});
          }
        }
      },
      skip: Platform.environment['LIVE_CYCLE_IG'] == '1'
          ? false
          : 'Set LIVE_CYCLE_IG=1 to exercise https://shlep.exe.xyz.',
    );
  });
}

class _FakeTransport implements CycleIgTransport {
  Map<String, dynamic>? createdBody;
  Uri? deletedUri;
  Map<String, String> deletedHeaders = const {};

  @override
  Future<CycleIgHttpResponse> postJson(
    Uri uri,
    Map<String, dynamic> body, {
    Map<String, String> headers = const {},
  }) async {
    createdBody = body;
    return const CycleIgHttpResponse(
      201,
      '{"id":"test-share","status":"active","fileUrl":"https://shlep.example/shl/test-share","fileIds":["file-a"],"manageToken":"test-manage-token"}',
    );
  }

  @override
  Future<CycleIgHttpResponse> delete(
    Uri uri, {
    Map<String, String> headers = const {},
  }) async {
    deletedUri = uri;
    deletedHeaders = headers;
    return const CycleIgHttpResponse(200, '{"status":"revoked"}');
  }

  @override
  Future<CycleIgHttpResponse> get(
    Uri uri, {
    Map<String, String> headers = const {},
  }) async {
    return CycleIgHttpResponse(
      200,
      createdBody?['ciphertext'] as String? ?? '',
    );
  }
}
