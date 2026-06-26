const CYCLE_STARTS = [
  '2026-01-02',
  '2026-01-30',
  '2026-02-27',
  '2026-03-27',
  '2026-04-24',
  '2026-05-22',
  '2026-06-19',
]
const LAST_SAMPLE_DATE = '2026-06-25'

export function createCycleIgSampleCycleDays() {
  return CYCLE_STARTS.flatMap((startDate, cycleIndex) =>
    createSampleCycle(startDate, cycleIndex)
  )
    .filter((day) => day.date <= LAST_SAMPLE_DATE)
    .sort((a, b) => a.date.localeCompare(b.date))
}

function createSampleCycle(startDate, cycleIndex) {
  const cycleNumber = cycleIndex + 1
  const tempBase = 36.36 + (cycleIndex % 3) * 0.03
  const days = [
    {
      date: addDays(startDate, 0),
      bleeding: { value: 3, exclude: false },
      temperature: temperature(tempBase + 0.06, '06:40'),
      pain: {
        cramps: true,
        ...(cycleIndex % 2 === 0 ? { headache: true } : {}),
      },
      mood: { fatigue: true },
      note: { value: `Synthetic sample cycle ${cycleNumber} day 1.` },
    },
    {
      date: addDays(startDate, 1),
      bleeding: { value: 2, exclude: false },
      temperature: temperature(tempBase + 0.04, '06:35'),
      ...(cycleIndex % 2 === 1 ? { pain: { backache: true } } : {}),
    },
    {
      date: addDays(startDate, 2),
      bleeding: { value: 1, exclude: false },
      mood: { fine: true },
    },
    {
      date: addDays(startDate, 3),
      bleeding: { value: 0, exclude: false },
      note: { value: `Synthetic tapering flow in cycle ${cycleNumber}.` },
    },
    {
      date: addDays(startDate, 10),
      temperature: temperature(tempBase - 0.02, '06:45'),
      mucus: { feeling: 2, texture: 1, value: 2, exclude: false },
      cervix: { opening: 1, firmness: 1, position: 1, exclude: false },
      mood: { energetic: true },
    },
    {
      date: addDays(startDate, 12),
      temperature: temperature(tempBase + 0.01, '06:45'),
      mucus: { feeling: 3, texture: 2, value: 4, exclude: false },
      cervix: { opening: 2, firmness: 1, position: 2, exclude: false },
      pain: { ovulationPain: true },
    },
    {
      date: addDays(startDate, 16),
      temperature: temperature(tempBase + 0.28, '06:45'),
      mood: { balanced: true },
    },
    {
      date: addDays(startDate, 20),
      temperature: temperature(tempBase + 0.32, '06:45'),
      mood: cycleIndex % 2 === 0 ? { happy: true } : { anxious: true },
    },
  ]

  if (cycleIndex % 2 === 1) {
    days.push({
      date: addDays(startDate, 22),
      bleeding: { value: 0, exclude: true },
      temperature: temperature(tempBase + 0.34, '07:05', true),
      mood: { stressed: true },
      note: { value: `Synthetic non-menstrual spotting in cycle ${cycleNumber}.` },
    })
  }

  if (cycleIndex === 0) {
    days[0].sex = { partner: true, condom: true }
    days[0].desire = { value: 2 }
  }

  return days
}

function temperature(value, time, exclude = false) {
  return {
    value: Math.round(value * 100) / 100,
    exclude,
    time,
    note: 'synthetic basal temperature',
  }
}

function addDays(dateString, days) {
  const date = new Date(`${dateString}T00:00:00.000Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}
