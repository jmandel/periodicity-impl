import { buildCycleFhirBundle } from '../lib/cycle-ig/export'
import { createCycleIgSampleCycleDays } from '../lib/cycle-ig/sample-data'
import sampleCycleDays from './fixtures/cycle-ig-sample'

const CYCLE = 'https://cycle.fhir.me/CodeSystem/cycle'

function observations(bundle) {
  return bundle.entry.map(({ resource }) => resource)
}

function observationsByCode(bundle, code) {
  return observations(bundle).filter((observation) =>
    observation.code.coding.some(
      (coding) => coding.system === CYCLE && coding.code === code
    )
  )
}

describe('Cycle IG FHIR export', () => {
  it('builds a Period Tracking collection Bundle with the bleeding core', () => {
    const bundle = buildCycleFhirBundle(sampleCycleDays, {
      exportedAt: '2026-06-25T12:00:00.000Z',
    })

    expect(bundle.resourceType).toBe('Bundle')
    expect(bundle.type).toBe('collection')
    expect(bundle.timestamp).toBe('2026-06-25T12:00:00.000Z')
    expect(bundle.meta.profile).toContain(
      'https://cycle.fhir.me/StructureDefinition/period-tracking-bundle'
    )

    const bleeding = observationsByCode(bundle, 'menstrual-bleeding')
    expect(bleeding).toHaveLength(7)
    expect(bleeding.map((observation) => observation.valueBoolean)).toEqual([
      true,
      true,
      true,
      true,
      false,
      true,
      true,
    ])
  })

  it('maps drip flow, basal temperature, selected symptoms, and notes', () => {
    const bundle = buildCycleFhirBundle(sampleCycleDays, {
      exportedAt: '2026-06-25T12:00:00.000Z',
    })
    const allObservations = observations(bundle)

    const flowCodes = observationsByCode(bundle, 'menstrual-flow').map(
      (observation) => observation.valueCodeableConcept.coding[0].code
    )
    expect(flowCodes).toEqual([
      'flow-heavy',
      'flow-moderate',
      'flow-light',
      'flow-spotting',
      'flow-moderate',
      'flow-heavy',
    ])

    const temperatures = allObservations.filter((observation) =>
      observation.meta.profile.includes(
        'https://cycle.fhir.me/StructureDefinition/basal-body-temperature'
      )
    )
    expect(temperatures.map((observation) => observation.effectiveDateTime)).toEqual(
      ['2026-01-02T06:40', '2026-01-03T06:35', '2026-03-01T06:20']
    )
    expect(
      temperatures.every(
        (observation) => observation.valueQuantity.code === 'Cel'
      )
    ).toBe(true)

    const symptoms = observationsByCode(bundle, 'symptom')
    expect(
      symptoms.some((observation) =>
        observation.valueCodeableConcept.coding.some(
          (coding) =>
            coding.system === 'http://snomed.info/sct' &&
            coding.code === '431416001'
        )
      )
    ).toBe(true)
    expect(
      allObservations.some((observation) =>
        observation.meta.profile.includes(
          'https://cycle.fhir.me/StructureDefinition/numeric-pain-severity'
        )
      )
    ).toBe(false)

    expect(
      allObservations.some(
        (observation) =>
          observation.code.coding[0].code === 'daily-note' &&
          observation.valueString === 'Synthetic sample cycle 1 day 1.'
      )
    ).toBe(true)
  })

  it('keeps the full app sample bundle internally consistent', () => {
    const bundle = buildCycleFhirBundle(createCycleIgSampleCycleDays(), {
      exportedAt: '2026-06-25T12:00:00.000Z',
    })
    const allObservations = observations(bundle)
    const fullUrls = bundle.entry.map((entry) => entry.fullUrl)
    const bleeding = observationsByCode(bundle, 'menstrual-bleeding')
    const flow = observationsByCode(bundle, 'menstrual-flow')
    const bleedingByDate = new Map(
      bleeding.map((observation) => [
        observation.effectiveDateTime.slice(0, 10),
        observation.valueBoolean,
      ])
    )

    expect(bundle.entry).toHaveLength(193)
    expect(new Set(fullUrls).size).toBe(fullUrls.length)
    expect(bleeding.filter((observation) => observation.valueBoolean)).toHaveLength(
      28
    )
    expect(
      bleeding.filter((observation) => observation.valueBoolean === false)
    ).toHaveLength(3)
    expect(flow).toHaveLength(28)
    expect(
      allObservations.every((observation) => observation.status === 'final')
    ).toBe(true)
    expect(
      allObservations.every((observation) => observation.effectiveDateTime)
    ).toBe(true)
    expect(
      allObservations.every(
        (observation) =>
          [
            'valueBoolean',
            'valueCodeableConcept',
            'valueQuantity',
            'valueString',
          ].filter((key) => key in observation).length === 1
      )
    ).toBe(true)
    expect(
      flow.every(
        (observation) =>
          bleedingByDate.get(observation.effectiveDateTime.slice(0, 10)) === true
      )
    ).toBe(true)
  })
})
