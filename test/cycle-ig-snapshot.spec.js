import {
  DEFAULT_CYCLE_IG_SCOPE,
  createCycleIgSnapshot,
} from '../lib/cycle-ig/snapshot'
import sampleCycleDays from './fixtures/cycle-ig-sample'

describe('Cycle IG source snapshot', () => {
  it('creates a scoped immutable snapshot with preview counts', () => {
    const snapshot = createCycleIgSnapshot(sampleCycleDays, {
      startDate: '2026-01-02',
      endDate: '2026-02-20',
      scope: {
        ...DEFAULT_CYCLE_IG_SCOPE,
        temperature: false,
        notes: false,
      },
    })

    expect(snapshot.startDate).toBe('2026-01-02')
    expect(snapshot.endDate).toBe('2026-02-20')
    expect(snapshot.cycleDays).toHaveLength(5)
    expect(snapshot.cycleDays.every((day) => day.temperature === null)).toBe(
      true
    )
    expect(snapshot.cycleDays.every((day) => day.note === null)).toBe(true)
    expect(snapshot.cycleDays.every((day) => day.sex === null)).toBe(true)
    expect(snapshot.cycleDays.every((day) => day.desire === null)).toBe(true)

    expect(snapshot.preview).toEqual({
      dayCount: 5,
      menstrualBleedingFacts: 4,
      nonMenstrualBleedingFacts: 1,
      flowFacts: 4,
      temperatureFacts: 0,
      symptomFacts: 6,
      fertilitySignFacts: 3,
      noteFacts: 0,
    })
  })

  it('requires at least one bleeding value in range', () => {
    expect(() =>
      createCycleIgSnapshot(sampleCycleDays, {
        startDate: '2026-01-05',
        endDate: '2026-01-28',
      })
    ).toThrow('at least one bleeding value')
  })
})
