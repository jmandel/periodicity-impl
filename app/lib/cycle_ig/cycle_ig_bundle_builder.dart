import 'dart:convert';

import 'package:menstrudel/cycle_ig/cycle_ig_dates.dart';
import 'package:menstrudel/cycle_ig/cycle_ig_snapshot.dart';
import 'package:menstrudel/models/flows/flow_enum.dart';
import 'package:menstrudel/models/period_logs/pain_level_enum.dart';
import 'package:menstrudel/models/period_logs/log_day.dart';
import 'package:menstrudel/models/period_logs/symptom.dart';
import 'package:menstrudel/models/period_logs/symptom_type_enum.dart';

class CycleIgBundleBuilder {
  static const cycleSystem = 'https://cycle.fhir.me/CodeSystem/cycle';
  static const appSystem = 'https://menstrudel.app/fhir/CodeSystem/period-log';
  static const surveySystem =
      'http://terminology.hl7.org/CodeSystem/observation-category';

  static Map<String, dynamic> build(CycleIgSnapshot snapshot) {
    final entries = <Map<String, dynamic>>[];

    for (final log in snapshot.logs) {
      entries.add(_entry(_bleedingObservation(log)));

      if (snapshot.scope.includeFlow) {
        entries.add(_entry(_flowObservation(log)));
      }

      if (snapshot.scope.includeSymptoms) {
        for (final symptom in log.symptoms) {
          entries.add(_entry(_symptomObservation(log, symptom)));
        }
      }

      if (snapshot.scope.includePain && log.painLevel != null) {
        entries.add(_entry(_painLevelObservation(log)));
      }
    }

    return {
      'resourceType': 'Bundle',
      'meta': {
        'profile': [
          'https://cycle.fhir.me/StructureDefinition/period-tracking-bundle',
        ],
      },
      'type': 'collection',
      'timestamp': snapshot.generatedAt.toUtc().toIso8601String(),
      'entry': entries,
    };
  }

  static String encode(CycleIgSnapshot snapshot) => jsonEncode(build(snapshot));

  static Map<String, dynamic> _bleedingObservation(LogDay log) {
    final date = CycleIgDates.isoDate(log.date);
    return {
      'resourceType': 'Observation',
      'id': _id('menstrual-bleeding', date),
      'meta': {
        'profile': [
          'https://cycle.fhir.me/StructureDefinition/menstrual-bleeding',
        ],
      },
      'status': 'final',
      'category': [_surveyCategory()],
      'code': {
        'coding': [
          {
            'system': cycleSystem,
            'code': 'menstrual-bleeding',
            'display': 'Menstrual bleeding',
          },
        ],
      },
      'effectiveDateTime': date,
      'valueBoolean': log.flow != FlowRate.none,
    };
  }

  static Map<String, dynamic> _flowObservation(LogDay log) {
    final date = CycleIgDates.isoDate(log.date);
    final flowCode = _flowCode(log.flow);
    return {
      'resourceType': 'Observation',
      'id': _id('menstrual-flow', date),
      'meta': {
        'profile': ['https://cycle.fhir.me/StructureDefinition/menstrual-flow'],
      },
      'status': 'final',
      'category': [_surveyCategory()],
      'code': {
        'coding': [
          {
            'system': cycleSystem,
            'code': 'menstrual-flow',
            'display': 'Patient-reported menstrual flow category',
          },
        ],
      },
      'effectiveDateTime': date,
      'valueCodeableConcept': {
        'coding': [
          {
            'system': cycleSystem,
            'code': flowCode,
            'display': _flowDisplay(log.flow),
          },
        ],
      },
    };
  }

  static Map<String, dynamic> _symptomObservation(LogDay log, Symptom symptom) {
    final date = CycleIgDates.isoDate(log.date);
    final concept = _symptomConcept(symptom);
    return {
      'resourceType': 'Observation',
      'id': _id('symptom', date, concept['code'] as String),
      'meta': {
        'profile': ['https://cycle.fhir.me/StructureDefinition/symptom'],
      },
      'status': 'final',
      'category': [_surveyCategory()],
      'code': {
        'coding': [
          {'system': cycleSystem, 'code': 'symptom', 'display': 'Symptom'},
        ],
      },
      'effectiveDateTime': date,
      'valueCodeableConcept': {
        'coding': [
          {
            'system': concept['system'],
            'code': concept['code'],
            'display': concept['display'],
          },
        ],
        'text': concept['display'],
      },
    };
  }

  static Map<String, dynamic> _painLevelObservation(LogDay log) {
    final date = CycleIgDates.isoDate(log.date);
    final painLevel = PainLevel.values[log.painLevel as int];
    final code = 'pain-level-${painLevel.name}';
    return {
      'resourceType': 'Observation',
      'id': _id('pain-level', date),
      'meta': {
        'profile': [
          'https://cycle.fhir.me/StructureDefinition/period-tracking-fact',
        ],
      },
      'status': 'final',
      'category': [_surveyCategory()],
      'code': {
        'coding': [
          {
            'system': appSystem,
            'code': 'pain-level',
            'display': 'Menstrudel pain level',
          },
        ],
        'text': 'Menstrudel pain level',
      },
      'effectiveDateTime': date,
      'valueCodeableConcept': {
        'coding': [
          {
            'system': appSystem,
            'code': code,
            'display': _title(painLevel.name),
          },
        ],
        'text': _title(painLevel.name),
      },
    };
  }

  static Map<String, dynamic> _surveyCategory() {
    return {
      'coding': [
        {'system': surveySystem, 'code': 'survey', 'display': 'Survey'},
      ],
    };
  }

  static Map<String, dynamic> _entry(Map<String, dynamic> resource) {
    return {
      'fullUrl': 'urn:menstrudel:${resource['id']}',
      'resource': resource,
    };
  }

  static String _flowCode(FlowRate flow) {
    return switch (flow) {
      FlowRate.none => 'flow-none',
      FlowRate.spotting => 'flow-spotting',
      FlowRate.light => 'flow-light',
      FlowRate.medium => 'flow-moderate',
      FlowRate.heavy => 'flow-heavy',
    };
  }

  static String _flowDisplay(FlowRate flow) {
    return switch (flow) {
      FlowRate.none => 'None',
      FlowRate.spotting => 'Spotting',
      FlowRate.light => 'Light',
      FlowRate.medium => 'Moderate',
      FlowRate.heavy => 'Heavy',
    };
  }

  static Map<String, String> _symptomConcept(Symptom symptom) {
    final standard = switch (symptom.type) {
      SymptomType.cramps => ('431416001', 'Menstrual cramp'),
      SymptomType.headache => ('25064002', 'Headache'),
      SymptomType.fatigue => ('84229001', 'Fatigue'),
      SymptomType.bloating => ('116289008', 'Abdominal bloating'),
      SymptomType.depressed => ('366979004', 'Depressed mood'),
      _ => null,
    };

    if (standard != null) {
      return {
        'system': 'http://snomed.info/sct',
        'code': standard.$1,
        'display': standard.$2,
      };
    }

    final source = symptom.type == SymptomType.custom
        ? symptom.customName
        : symptom.getDbName();
    return {
      'system': appSystem,
      'code': 'symptom-${_slug(source)}',
      'display': _title(source),
    };
  }

  static String _id(String prefix, String date, [String? suffix]) {
    final id = [prefix, date, if (suffix != null) _slug(suffix)].join('-');
    return id.length <= 64 ? id : id.substring(0, 64);
  }

  static String _slug(String value) {
    final slug = value
        .toLowerCase()
        .replaceAll(RegExp(r'[^a-z0-9]+'), '-')
        .replaceAll(RegExp(r'^-+|-+$'), '');
    return slug.isEmpty ? 'value' : slug;
  }

  static String _title(String value) {
    return value
        .split(RegExp(r'[\s_-]+'))
        .where((part) => part.isNotEmpty)
        .map((part) => part[0].toUpperCase() + part.substring(1))
        .join(' ');
  }
}
