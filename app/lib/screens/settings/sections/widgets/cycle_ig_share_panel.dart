import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:menstrudel/coordinators/data_refresh_coordinator.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_dates.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_sample_data.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_scope.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_share.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_share_client.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_snapshot.dart';
import 'package:menstrudel/database/repositories/logs_repository.dart';
import 'package:menstrudel/database/repositories/periods_repository.dart';
import 'package:provider/provider.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

class CycleIgSharePanel extends StatefulWidget {
  const CycleIgSharePanel({super.key});

  @override
  State<CycleIgSharePanel> createState() => _CycleIgSharePanelState();
}

class _CycleIgSharePanelState extends State<CycleIgSharePanel> {
  final CycleIgShareClient _shareClient = CycleIgShareClient();

  CycleIgScope? _scope;
  CycleIgSnapshot? _snapshot;
  CycleIgShare? _share;
  bool _includeFlow = true;
  bool _includeSymptoms = true;
  bool _includePain = true;
  bool _isBusy = false;
  String? _message;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(top: 8, bottom: 12),
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: Theme.of(context).dividerColor.withValues(alpha: 0.1),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const _SmartLinkMark(),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Share with SMART Link',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                if (_isBusy)
                  const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            _buildPersonScope(context),
            const SizedBox(height: 8),
            _buildDateControls(context),
            const SizedBox(height: 8),
            _buildCategoryControls(),
            const SizedBox(height: 12),
            _buildPrimaryActions(context),
            if (_message != null) ...[
              const SizedBox(height: 12),
              _buildMessage(context),
            ],
            if (_snapshot != null) ...[
              const SizedBox(height: 12),
              _buildPreview(context, _snapshot!),
            ],
            if (_share != null) ...[
              const SizedBox(height: 12),
              _buildShareControls(context, _share!),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildPersonScope(BuildContext context) {
    return _InfoRow(
      icon: Icons.person_outline_rounded,
      label: 'Person/account',
      value: 'Current local profile, identity not included',
    );
  }

  Widget _buildDateControls(BuildContext context) {
    final scope = _scope;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Date range', style: Theme.of(context).textTheme.labelLarge),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            OutlinedButton.icon(
              key: const Key('cycle-ig-start-date'),
              icon: const Icon(Icons.event_rounded),
              label: Text(
                scope == null
                    ? 'Start'
                    : CycleIgDates.displayDate(scope.startDate),
              ),
              onPressed: _isBusy ? null : () => _pickDate(isStart: true),
            ),
            OutlinedButton.icon(
              key: const Key('cycle-ig-end-date'),
              icon: const Icon(Icons.event_available_rounded),
              label: Text(
                scope == null ? 'End' : CycleIgDates.displayDate(scope.endDate),
              ),
              onPressed: _isBusy ? null : () => _pickDate(isStart: false),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildCategoryControls() {
    return Column(
      children: [
        CheckboxListTile(
          dense: true,
          contentPadding: EdgeInsets.zero,
          value: true,
          onChanged: null,
          title: const Text('Menstrual bleeding core facts'),
        ),
        CheckboxListTile(
          dense: true,
          contentPadding: EdgeInsets.zero,
          value: _includeFlow,
          onChanged: _isBusy
              ? null
              : (value) => setState(() {
                  _includeFlow = value ?? true;
                  _share = null;
                }),
          title: const Text('Flow categories'),
        ),
        CheckboxListTile(
          dense: true,
          contentPadding: EdgeInsets.zero,
          value: _includeSymptoms,
          onChanged: _isBusy
              ? null
              : (value) => setState(() {
                  _includeSymptoms = value ?? true;
                  _share = null;
                }),
          title: const Text('Symptoms'),
        ),
        CheckboxListTile(
          dense: true,
          contentPadding: EdgeInsets.zero,
          value: _includePain,
          onChanged: _isBusy
              ? null
              : (value) => setState(() {
                  _includePain = value ?? true;
                  _share = null;
                }),
          title: const Text('Menstrudel pain-level enum'),
        ),
      ],
    );
  }

  Widget _buildPrimaryActions(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        OutlinedButton.icon(
          key: const Key('cycle-ig-load-sample'),
          icon: const Icon(Icons.science_outlined),
          label: const Text('Load sample'),
          onPressed: _isBusy ? null : _loadSample,
        ),
        OutlinedButton.icon(
          key: const Key('cycle-ig-review'),
          icon: const Icon(Icons.fact_check_outlined),
          label: const Text('Review'),
          onPressed: _isBusy ? null : () => _refreshSnapshot(showMessage: true),
        ),
        FilledButton.icon(
          key: const Key('cycle-ig-create-share'),
          icon: const Icon(Icons.qr_code_rounded),
          label: const Text('Share with SMART Link'),
          onPressed: _isBusy ? null : _createShare,
        ),
      ],
    );
  }

  Widget _buildMessage(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Theme.of(
          context,
        ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.55),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(_message!, style: Theme.of(context).textTheme.bodySmall),
    );
  }

  Widget _buildPreview(BuildContext context, CycleIgSnapshot snapshot) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        border: Border.all(
          color: Theme.of(context).dividerColor.withValues(alpha: 0.25),
        ),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'SMART Link preview',
            style: Theme.of(context).textTheme.titleSmall,
          ),
          const SizedBox(height: 8),
          _InfoRow(
            icon: Icons.date_range_rounded,
            label: 'Stored dates',
            value: snapshot.dateRangeLabel,
          ),
          _InfoRow(
            icon: Icons.water_drop_outlined,
            label: 'Bleeding facts',
            value:
                '${snapshot.bleedingFactCount} logged days (${snapshot.bleedingTrueCount} true, ${snapshot.bleedingFalseCount} false)',
          ),
          _InfoRow(
            icon: Icons.stacked_bar_chart_rounded,
            label: 'Layer 1 facts',
            value:
                '${snapshot.flowFactCount} flow, ${snapshot.symptomFactCount} symptoms, ${snapshot.painFactCount} pain levels',
          ),
          _InfoRow(
            icon: Icons.visibility_off_outlined,
            label: 'Omitted',
            value:
                'Predictions, period summaries, contraception, sexual activity, sanitary products, profile identity, and missing days',
          ),
          _InfoRow(
            icon: Icons.lock_outline_rounded,
            label: 'Who can open it',
            value:
                'Anyone with the link can open it until you stop sharing, it expires, or 5 opens are used',
          ),
          _InfoRow(
            icon: Icons.cloud_off_outlined,
            label: 'Live host',
            value:
                'shlep.exe.xyz stores ciphertext only; expires in 7 days or after 5 opens',
          ),
        ],
      ),
    );
  }

  Widget _buildShareControls(BuildContext context, CycleIgShare share) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        border: Border.all(
          color: Theme.of(context).colorScheme.primary.withValues(alpha: 0.4),
        ),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const _SmartLinkMark(),
              const SizedBox(width: 10),
              Text(
                'Active SMART Link',
                style: Theme.of(context).textTheme.titleSmall,
              ),
            ],
          ),
          const SizedBox(height: 8),
          Center(
            child: Container(
              color: Colors.white,
              padding: const EdgeInsets.all(8),
              child: QrImageView(
                key: const Key('cycle-ig-qr'),
                data: share.viewerLink,
                version: QrVersions.auto,
                size: 220,
                backgroundColor: Colors.white,
              ),
            ),
          ),
          const SizedBox(height: 8),
          _InfoRow(
            icon: Icons.timer_outlined,
            label: 'Stops automatically',
            value:
                '${CycleIgDates.displayDate(share.expiresAt.toLocal())} or after ${share.maxUses} opens',
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              OutlinedButton.icon(
                key: const Key('cycle-ig-copy-share'),
                icon: const Icon(Icons.copy_rounded),
                label: const Text('Copy link'),
                onPressed: _isBusy ? null : () => _copyShare(share),
              ),
              OutlinedButton.icon(
                key: const Key('cycle-ig-native-share'),
                icon: const Icon(Icons.ios_share_rounded),
                label: const Text('Share'),
                onPressed: _isBusy ? null : () => _nativeShare(share),
              ),
              OutlinedButton.icon(
                key: const Key('cycle-ig-open-share'),
                icon: const Icon(Icons.open_in_new_rounded),
                label: const Text('Open viewer'),
                onPressed: _isBusy ? null : () => _openShare(share),
              ),
              FilledButton.tonalIcon(
                key: const Key('cycle-ig-stop-share'),
                icon: const Icon(Icons.link_off_rounded),
                label: const Text('Stop sharing'),
                onPressed: _isBusy ? null : () => _stopShare(share),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _pickDate({required bool isStart}) async {
    final logs = await context.read<LogsRepository>().readAllLogs();
    final baseScope = _scope ?? CycleIgScope.forLogs(logs);
    if (!mounted) return;

    final picked = await showDatePicker(
      context: context,
      initialDate: isStart ? baseScope.startDate : baseScope.endDate,
      firstDate: DateTime(2000),
      lastDate: DateTime.now(),
    );
    if (picked == null) return;

    setState(() {
      _scope = isStart
          ? baseScope.copyWith(startDate: CycleIgDates.dateOnly(picked))
          : baseScope.copyWith(endDate: CycleIgDates.dateOnly(picked));
      _share = null;
    });
  }

  Future<CycleIgSnapshot?> _refreshSnapshot({bool showMessage = false}) async {
    return _runBusy(() async {
      final logs = await context.read<LogsRepository>().readAllLogs();
      final scope = (_scope ?? CycleIgScope.forLogs(logs)).copyWith(
        includeFlow: _includeFlow,
        includeSymptoms: _includeSymptoms,
        includePain: _includePain,
      );

      if (scope.endDate.isBefore(scope.startDate)) {
        throw StateError('End date must be on or after start date.');
      }

      final snapshot = CycleIgSnapshot.fromLogs(logs, scope);
      setState(() {
        _scope = scope;
        _snapshot = snapshot;
        _share = null;
        if (showMessage) {
          _message = snapshot.logs.isEmpty
              ? 'No stored Menstrudel logs are inside this date range.'
              : 'Review is ready from ${snapshot.observationCount} observations.';
        }
      });
      return snapshot;
    });
  }

  Future<void> _loadSample() async {
    await _runBusy(() async {
      final logsRepo = context.read<LogsRepository>();
      final periodsRepo = context.read<PeriodsRepository>();
      final coordinator = context.read<DataRefreshCoordinator>();
      final written = await CycleIgSampleData.loadIntoAppStorage(
        logsRepo,
        periodsRepo,
      );

      try {
        await coordinator.resetAllData();
      } catch (_) {
        // Some tests build this widget without the full app refresh coordinator.
      }

      final logs = await logsRepo.readAllLogs();
      final scope = CycleIgScope.forLogs(
        logs,
        includeFlow: _includeFlow,
        includeSymptoms: _includeSymptoms,
        includePain: _includePain,
      );
      final snapshot = CycleIgSnapshot.fromLogs(logs, scope);
      setState(() {
        _scope = scope;
        _snapshot = snapshot;
        _share = null;
        _message =
            '$written synthetic cycle logs loaded as ordinary Menstrudel entries.';
      });
    });
  }

  Future<void> _createShare() async {
    final snapshot = await _refreshSnapshot();
    if (snapshot == null) return;
    if (snapshot.bleedingFactCount == 0) {
      setState(
        () =>
            _message = 'A SMART Link needs at least one stored bleeding fact.',
      );
      return;
    }

    await _runBusy(() async {
      final share = await _shareClient.createShare(snapshot);
      setState(() {
        _share = share;
        _message =
            'SMART Link active. The QR, copy, share, open, and stop controls all use the same encrypted link.';
      });
    });
  }

  Future<void> _copyShare(CycleIgShare share) async {
    await Clipboard.setData(ClipboardData(text: share.viewerLink));
    if (mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('SMART Link copied.')));
    }
  }

  Future<void> _nativeShare(CycleIgShare share) async {
    final box = context.findRenderObject() as RenderBox?;
    await SharePlus.instance.share(
      ShareParams(
        text: share.viewerLink,
        sharePositionOrigin: box == null
            ? null
            : box.localToGlobal(Offset.zero) & box.size,
      ),
    );
  }

  Future<void> _openShare(CycleIgShare share) async {
    await launchUrl(
      Uri.parse(share.viewerLink),
      mode: LaunchMode.externalApplication,
    );
  }

  Future<void> _stopShare(CycleIgShare share) async {
    await _runBusy(() async {
      await _shareClient.revoke(share);
      setState(() {
        _share = null;
        _message =
            'Sharing stopped. The previous QR/link now returns 404 from shlep.';
      });
    });
  }

  Future<T?> _runBusy<T>(Future<T> Function() work) async {
    if (_isBusy) return null;
    setState(() {
      _isBusy = true;
      _message = null;
    });
    try {
      return await work();
    } catch (e) {
      if (mounted) {
        setState(() => _message = e.toString());
      }
      return null;
    } finally {
      if (mounted) {
        setState(() => _isBusy = false);
      }
    }
  }
}

class _SmartLinkMark extends StatelessWidget {
  const _SmartLinkMark();

  @override
  Widget build(BuildContext context) {
    return const SizedBox(
      width: 31,
      height: 25,
      child: CustomPaint(painter: _SmartLogoPainter()),
    );
  }
}

class _SmartLogoPainter extends CustomPainter {
  const _SmartLogoPainter();

  @override
  void paint(Canvas canvas, Size size) {
    canvas.save();
    canvas.scale(size.width / 49, size.height / 40);
    _draw(
      canvas,
      const Color(0xFF722772),
      'M12.9297 0H18.2012L24.416 10.1238L30.7417 0H35.9022L24.416 18.652L12.9297 0Z',
    );
    _draw(
      canvas,
      const Color(0xFFE24A31),
      'M0 19.6422L2.66348 15.4607H15.0931L8.93377 5.22682L11.4863 0.990234L22.9171 19.6422H0Z',
    );
    _draw(
      canvas,
      const Color(0xFF89BF44),
      'M48.8858 21.293L46.2778 25.4745H33.8482L40.0075 35.8184L37.3995 40L25.9688 21.293H48.8858Z',
    );
    _draw(
      canvas,
      const Color(0xFFE77D26),
      'M37.3995 0.935547L40.063 5.22716L33.7927 15.461H46.3333L48.8858 19.6426H25.9688L37.3995 0.935547Z',
    );
    _draw(
      canvas,
      const Color(0xFFF1B42A),
      'M11.4863 40L8.82279 35.7084L15.0931 25.4745H2.55251L0 21.293H22.9171L11.4863 40Z',
    );
    canvas.restore();
  }

  void _draw(Canvas canvas, Color color, String pathData) {
    final path = Path();
    final tokens = pathData
        .replaceAll('M', ' M ')
        .replaceAll('H', ' H ')
        .replaceAll('L', ' L ')
        .replaceAll('Z', ' Z ')
        .trim()
        .split(RegExp(r'\s+'));
    var index = 0;
    var currentX = 0.0;
    var currentY = 0.0;
    while (index < tokens.length) {
      final command = tokens[index++];
      switch (command) {
        case 'M':
          currentX = double.parse(tokens[index++]);
          currentY = double.parse(tokens[index++]);
          path.moveTo(currentX, currentY);
          break;
        case 'H':
          currentX = double.parse(tokens[index++]);
          path.lineTo(currentX, currentY);
          break;
        case 'L':
          currentX = double.parse(tokens[index++]);
          currentY = double.parse(tokens[index++]);
          path.lineTo(currentX, currentY);
          break;
        case 'Z':
          path.close();
          break;
      }
    }
    canvas.drawPath(path, Paint()..color = color);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class _InfoRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;

  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: Theme.of(context).colorScheme.primary),
          const SizedBox(width: 8),
          Expanded(
            child: RichText(
              text: TextSpan(
                style: Theme.of(context).textTheme.bodySmall,
                children: [
                  TextSpan(
                    text: '$label: ',
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                  TextSpan(text: value),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
