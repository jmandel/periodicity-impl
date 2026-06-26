import 'package:menstrudel/cycle_ig/cycle_ig_dates.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_scope.dart';
import 'package:menstrudel/models/flows/flow_enum.dart';
import 'package:menstrudel/models/period_logs/log_day.dart';

class CycleIgSnapshot {
  final CycleIgScope scope;
  final List<LogDay> logs;
  final DateTime generatedAt;

  const CycleIgSnapshot({
    required this.scope,
    required this.logs,
    required this.generatedAt,
  });

  factory CycleIgSnapshot.fromLogs(
    List<LogDay> allLogs,
    CycleIgScope scope, {
    DateTime? generatedAt,
  }) {
    final scopedLogs =
        allLogs
            .where(
              (log) => CycleIgDates.isWithin(
                log.date,
                scope.startDate,
                scope.endDate,
              ),
            )
            .toList()
          ..sort((a, b) => a.date.compareTo(b.date));

    return CycleIgSnapshot(
      scope: scope,
      logs: scopedLogs,
      generatedAt: generatedAt ?? DateTime.now().toUtc(),
    );
  }

  int get bleedingTrueCount =>
      logs.where((log) => log.flow != FlowRate.none).length;

  int get bleedingFalseCount =>
      logs.where((log) => log.flow == FlowRate.none).length;

  int get bleedingFactCount => logs.length;

  int get flowFactCount => scope.includeFlow ? logs.length : 0;

  int get symptomFactCount {
    if (!scope.includeSymptoms) return 0;
    return logs.fold<int>(0, (sum, log) => sum + log.symptoms.length);
  }

  int get painFactCount {
    if (!scope.includePain) return 0;
    return logs.where((log) => log.painLevel != null).length;
  }

  int get observationCount =>
      bleedingFactCount + flowFactCount + symptomFactCount + painFactCount;

  DateTime? get firstDate =>
      logs.isEmpty ? null : CycleIgDates.dateOnly(logs.first.date);

  DateTime? get lastDate =>
      logs.isEmpty ? null : CycleIgDates.dateOnly(logs.last.date);

  String get dateRangeLabel {
    final first = firstDate;
    final last = lastDate;
    if (first == null || last == null) return 'No stored logs in scope';
    return '${CycleIgDates.displayDate(first)} - ${CycleIgDates.displayDate(last)}';
  }
}
