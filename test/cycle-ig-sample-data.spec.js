import { createCycleIgSampleCycleDays } from '../lib/cycle-ig/sample-data'
import { createCycleIgSnapshot } from '../lib/cycle-ig/snapshot'

describe('Cycle IG sample data', () => {
  it('creates a deterministic multi-cycle sample for app screenshots', () => {
    const cycleDays = createCycleIgSampleCycleDays()
    const snapshot = createCycleIgSnapshot(cycleDays)

    expect(cycleDays[0].date).toBe('2026-01-02')
    expect(cycleDays[cycleDays.length - 1].date).toBe('2026-06-22')
    expect(cycleDays).toHaveLength(55)
    expect(snapshot.preview.menstrualBleedingFacts).toBe(28)
    expect(snapshot.preview.nonMenstrualBleedingFacts).toBe(3)
    expect(snapshot.preview.temperatureFacts).toBe(38)
    expect(snapshot.preview.symptomFacts).toBeGreaterThan(20)
    expect(snapshot.preview.fertilitySignFacts).toBe(24)
  })
})
