export default [
  {
    date: '2026-01-02',
    bleeding: { value: 3, exclude: false },
    temperature: {
      value: 36.42,
      exclude: false,
      time: '06:40',
      note: 'synthetic basal temperature',
    },
    pain: { cramps: true, headache: true },
    mood: { fatigue: true },
    note: { value: 'Synthetic sample cycle 1 day 1.' },
    sex: { partner: true, condom: true },
    desire: { value: 2 },
  },
  {
    date: '2026-01-03',
    bleeding: { value: 2, exclude: false },
    temperature: { value: 36.51, exclude: false, time: '06:35' },
    mucus: { feeling: 0, texture: 0, value: 0, exclude: false },
  },
  {
    date: '2026-01-04',
    bleeding: { value: 1, exclude: false },
    pain: { backache: true },
  },
  {
    date: '2026-01-29',
    bleeding: { value: 0, exclude: false },
    cervix: { opening: 1, firmness: 1, position: 2, exclude: false },
    mucus: { feeling: 3, texture: 2, value: 4, exclude: false },
  },
  {
    date: '2026-02-20',
    bleeding: { value: 0, exclude: true },
    temperature: { value: 36.9, exclude: true, time: '07:05' },
    mood: { stressed: true, anxious: true },
    note: { value: 'Excluded spotting and temperature are synthetic.' },
  },
  {
    date: '2026-03-01',
    bleeding: { value: 2, exclude: false },
    temperature: { value: 36.58, exclude: false, time: '06:20' },
    pain: { nausea: true, tenderBreasts: true },
  },
  {
    date: '2026-03-02',
    bleeding: { value: 3, exclude: false },
    pain: { migraine: true, other: true, note: 'synthetic other pain' },
  },
]
