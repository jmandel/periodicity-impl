import 'package:menstrudel/cycle_ig/cycle_ig_snapshot.dart';

class CycleIgShare {
  final String id;
  final String fileUrl;
  final String manageToken;
  final String viewerLink;
  final String bareShlink;
  final String keyBase64Url;
  final String ciphertext;
  final DateTime expiresAt;
  final int maxUses;
  final CycleIgSnapshot snapshot;

  const CycleIgShare({
    required this.id,
    required this.fileUrl,
    required this.manageToken,
    required this.viewerLink,
    required this.bareShlink,
    required this.keyBase64Url,
    required this.ciphertext,
    required this.expiresAt,
    required this.maxUses,
    required this.snapshot,
  });
}
