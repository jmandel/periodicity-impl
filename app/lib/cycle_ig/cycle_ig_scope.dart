import 'package:menstrudel/cycle_ig/cycle_ig_dates.dart';
import 'package:menstrudel/models/period_logs/log_day.dart';

class CycleIgScope {
  final DateTime startDate;
  final DateTime endDate;
  final bool includeFlow;
  final bool includeSymptoms;
  final bool includePain;

  const CycleIgScope({
    required this.startDate,
    required this.endDate,
    this.includeFlow = true,
    this.includeSymptoms = true,
    this.includePain = true,
  });

  factory CycleIgScope.forLogs(
    List<LogDay> logs, {
    bool includeFlow = true,
    bool includeSymptoms = true,
    bool includePain = true,
  }) {
    if (logs.isEmpty) {
      final today = CycleIgDates.dateOnly(DateTime.now());
      return CycleIgScope(
        startDate: today.subtract(const Duration(days: 180)),
        endDate: today,
        includeFlow: includeFlow,
        includeSymptoms: includeSymptoms,
        includePain: includePain,
      );
    }

    final sorted = [...logs]..sort((a, b) => a.date.compareTo(b.date));
    return CycleIgScope(
      startDate: CycleIgDates.dateOnly(sorted.first.date),
      endDate: CycleIgDates.dateOnly(sorted.last.date),
      includeFlow: includeFlow,
      includeSymptoms: includeSymptoms,
      includePain: includePain,
    );
  }

  CycleIgScope copyWith({
    DateTime? startDate,
    DateTime? endDate,
    bool? includeFlow,
    bool? includeSymptoms,
    bool? includePain,
  }) {
    return CycleIgScope(
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      includeFlow: includeFlow ?? this.includeFlow,
      includeSymptoms: includeSymptoms ?? this.includeSymptoms,
      includePain: includePain ?? this.includePain,
    );
  }
}
