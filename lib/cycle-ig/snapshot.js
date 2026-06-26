const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

export const DEFAULT_CYCLE_IG_SCOPE = {
  temperature: true,
  symptoms: true,
  fertilitySigns: true,
  notes: true,
}

export function createCycleIgSnapshot(cycleDays, options = {}) {
  const sortedCycleDays = [...cycleDays].sort((a, b) =>
    a.date.localeCompare(b.date)
  )
  const firstDate = sortedCycleDays[0] && sortedCycleDays[0].date
  const lastDate = sortedCycleDays[sortedCycleDays.length - 1]
    ? sortedCycleDays[sortedCycleDays.length - 1].date
    : null
  const startDate = options.startDate || firstDate
  const endDate = options.endDate || lastDate
  const scope = {
    ...DEFAULT_CYCLE_IG_SCOPE,
    ...(options.scope || {}),
  }

  validateDateRange(startDate, endDate)

  const scopedCycleDays = sortedCycleDays
    .filter((day) => day.date >= startDate && day.date <= endDate)
    .map((day) => applyScope(day, scope))
    .filter(hasExportableData)

  if (!scopedCycleDays.some(hasBleedingValue)) {
    throw new Error(
      'SMART Link requires at least one bleeding value in the selected range.'
    )
  }

  return {
    cycleDays: scopedCycleDays,
    startDate,
    endDate,
    scope,
    preview: buildPreview(scopedCycleDays, scope),
  }
}

function validateDateRange(startDate, endDate) {
  if (!startDate || !endDate) {
    throw new Error('Select a date range before creating a SMART Link.')
  }

  if (!DATE_PATTERN.test(startDate) || !DATE_PATTERN.test(endDate)) {
    throw new Error('Use YYYY-MM-DD dates for the SMART Link range.')
  }

  if (startDate > endDate) {
    throw new Error('The SMART Link start date must be before the end date.')
  }
}

function applyScope(day, scope) {
  return {
    ...day,
    temperature: scope.temperature ? day.temperature : null,
    pain: scope.symptoms ? day.pain : null,
    mood: scope.symptoms ? day.mood : null,
    mucus: scope.fertilitySigns ? day.mucus : null,
    cervix: scope.fertilitySigns ? day.cervix : null,
    note: scope.notes ? day.note : null,
    desire: null,
    sex: null,
  }
}

function hasExportableData(day) {
  return Boolean(
    hasBleedingValue(day) ||
      hasTemperatureValue(day) ||
      day.pain ||
      day.mood ||
      hasMucusValue(day) ||
      hasCervixValue(day) ||
      hasNoteValue(day)
  )
}

function buildPreview(cycleDays, scope) {
  return {
    dayCount: cycleDays.length,
    menstrualBleedingFacts: count(cycleDays, (day) => {
      return hasBleedingValue(day) && day.bleeding.exclude !== true
    }),
    nonMenstrualBleedingFacts: count(cycleDays, (day) => {
      return hasBleedingValue(day) && day.bleeding.exclude === true
    }),
    flowFacts: count(cycleDays, (day) => {
      return (
        hasBleedingValue(day) &&
        day.bleeding.exclude !== true &&
        day.bleeding.value >= 0 &&
        day.bleeding.value <= 3
      )
    }),
    temperatureFacts: scope.temperature
      ? count(cycleDays, hasTemperatureValue)
      : 0,
    symptomFacts: scope.symptoms
      ? cycleDays.reduce((total, day) => {
          return total + countBooleanValues(day.pain) + countBooleanValues(day.mood)
        }, 0)
      : 0,
    fertilitySignFacts: scope.fertilitySigns
      ? count(cycleDays, hasMucusValue) + count(cycleDays, hasCervixValue)
      : 0,
    noteFacts: scope.notes ? count(cycleDays, hasNoteValue) : 0,
  }
}

function count(items, predicate) {
  return items.filter(predicate).length
}

function countBooleanValues(source) {
  if (!source) return 0

  return Object.keys(source).filter((key) => source[key] === true).length
}

function hasBleedingValue(day) {
  return Boolean(day.bleeding && isNumber(day.bleeding.value))
}

function hasTemperatureValue(day) {
  return Boolean(
    day.temperature && isNumber(day.temperature.value) && !day.temperature.exclude
  )
}

function hasMucusValue(day) {
  const { mucus } = day
  if (!mucus || mucus.exclude) return false

  return ['feeling', 'texture', 'value'].some((key) => isNumber(mucus[key]))
}

function hasCervixValue(day) {
  const { cervix } = day
  if (!cervix || cervix.exclude) return false

  return ['opening', 'firmness', 'position'].some((key) =>
    isNumber(cervix[key])
  )
}

function hasNoteValue(day) {
  return Boolean(day.note && day.note.value)
}

function isNumber(value) {
  return typeof value === 'number'
}
