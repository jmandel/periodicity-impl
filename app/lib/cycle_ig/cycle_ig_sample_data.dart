import 'package:menstrudel/cycle_ig/cycle_ig_dates.dart';
import 'package:menstrudel/database/repositories/logs_repository.dart';
import 'package:menstrudel/database/repositories/periods_repository.dart';
import 'package:menstrudel/models/flows/flow_enum.dart';
import 'package:menstrudel/models/period_logs/log_day.dart';
import 'package:menstrudel/models/period_logs/pain_level_enum.dart';
import 'package:menstrudel/models/period_logs/symptom.dart';
import 'package:menstrudel/models/period_logs/symptom_type_enum.dart';

class CycleIgSampleData {
  static List<LogDay> syntheticLogs({DateTime? today}) {
    final cap = CycleIgDates.min(
      CycleIgDates.dateOnly(today ?? DateTime.now()),
      DateTime(2026, 6, 25),
    );
    final cycleStarts = [
      DateTime(2026, 1, 2),
      DateTime(2026, 1, 30),
      DateTime(2026, 2, 27),
      DateTime(2026, 3, 27),
      DateTime(2026, 4, 24),
      DateTime(2026, 5, 22),
      DateTime(2026, 6, 14),
    ];

    final logs = <LogDay>[];
    for (final start in cycleStarts) {
      logs.addAll([
        _log(start, FlowRate.spotting, PainLevel.mild, [
          SymptomType.cramps,
          SymptomType.backPain,
        ]),
        _log(
          start.add(const Duration(days: 1)),
          FlowRate.light,
          PainLevel.moderate,
          [SymptomType.cramps, SymptomType.fatigue],
        ),
        _log(
          start.add(const Duration(days: 2)),
          FlowRate.medium,
          PainLevel.severe,
          [SymptomType.headache, SymptomType.nausea, SymptomType.tenderBreasts],
        ),
        _log(
          start.add(const Duration(days: 3)),
          FlowRate.heavy,
          PainLevel.moderate,
          [SymptomType.bloating, SymptomType.fatigue],
        ),
        _log(start.add(const Duration(days: 8)), FlowRate.none, null, [
          SymptomType.moodSwings,
        ]),
        _log(
          start.add(const Duration(days: 10)),
          FlowRate.none,
          PainLevel.none,
          [SymptomType.acne],
        ),
        _log(
          start.add(const Duration(days: 11)),
          FlowRate.none,
          null,
          [SymptomType.insomnia],
          customSymptoms: ['sleep changes'],
        ),
      ]);
    }

    return logs
        .where((log) => !CycleIgDates.dateOnly(log.date).isAfter(cap))
        .toList()
      ..sort((a, b) => a.date.compareTo(b.date));
  }

  static Future<int> loadIntoAppStorage(
    LogsRepository logsRepository,
    PeriodsRepository periodsRepository, {
    DateTime? today,
  }) async {
    final existing = await logsRepository.readAllLogs();
    final existingByDate = {
      for (final log in existing) CycleIgDates.isoDate(log.date): log,
    };

    var written = 0;
    for (final sample in syntheticLogs(today: today)) {
      final existingLog = existingByDate[CycleIgDates.isoDate(sample.date)];
      final id = await logsRepository.upsertLog(
        sample.copyWith(id: existingLog?.id),
      );
      existingByDate[CycleIgDates.isoDate(sample.date)] = sample.copyWith(
        id: id,
      );
      written++;
    }

    final logs = await logsRepository.readAllLogs();
    final mapping = await periodsRepository.recalculateAndAssignPeriods(logs);
    await logsRepository.updateLogPeriodIds(mapping);
    return written;
  }

  static LogDay _log(
    DateTime date,
    FlowRate flow,
    PainLevel? painLevel,
    List<SymptomType> symptoms, {
    List<String> customSymptoms = const [],
  }) {
    return LogDay(
      date: date,
      flow: flow,
      painLevel: painLevel?.intValue,
      symptoms: [
        for (final type in symptoms) Symptom(type: type),
        for (final name in customSymptoms)
          Symptom(type: SymptomType.custom, customName: name),
      ],
    );
  }
}
