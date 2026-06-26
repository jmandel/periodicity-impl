const CYCLE_CANONICAL = 'https://cycle.fhir.me'
const CYCLE_CODE_SYSTEM = `${CYCLE_CANONICAL}/CodeSystem/cycle`
const DRIP_CODE_SYSTEM =
  'https://gitlab.com/bloodyhealth/drip/CodeSystem/drip-cycle-ig'
const LOINC = 'http://loinc.org'
const SNOMED = 'http://snomed.info/sct'
const UCUM = 'http://unitsofmeasure.org'
const OBSERVATION_CATEGORY =
  'http://terminology.hl7.org/CodeSystem/observation-category'

const BUNDLE_PROFILE = `${CYCLE_CANONICAL}/StructureDefinition/period-tracking-bundle`
const FACT_PROFILE = `${CYCLE_CANONICAL}/StructureDefinition/period-tracking-fact`
const MENSTRUAL_BLEEDING_PROFILE = `${CYCLE_CANONICAL}/StructureDefinition/menstrual-bleeding`
const MENSTRUAL_FLOW_PROFILE = `${CYCLE_CANONICAL}/StructureDefinition/menstrual-flow`
const SYMPTOM_PROFILE = `${CYCLE_CANONICAL}/StructureDefinition/symptom`
const BASAL_BODY_TEMPERATURE_PROFILE = `${CYCLE_CANONICAL}/StructureDefinition/basal-body-temperature`

const FLOW_BY_DRIP_VALUE = [
  { code: 'flow-spotting', display: 'Spotting' },
  { code: 'flow-light', display: 'Light' },
  { code: 'flow-moderate', display: 'Moderate' },
  { code: 'flow-heavy', display: 'Heavy' },
]

const PAIN_SYMPTOMS = [
  [
    'cramps',
    'cramps',
    { system: SNOMED, code: '431416001', display: 'Menstrual cramp' },
  ],
  ['ovulationPain', 'ovulation pain'],
  [
    'headache',
    'headache',
    { system: SNOMED, code: '25064002', display: 'Headache' },
  ],
  ['backache', 'backache'],
  ['nausea', 'nausea'],
  ['tenderBreasts', 'tender breasts'],
  ['migraine', 'migraine'],
  ['other', 'other pain'],
]

const MOOD_SYMPTOMS = [
  ['happy', 'happy'],
  ['sad', 'sad'],
  [
    'stressed',
    'stressed',
    { system: SNOMED, code: '73595000', display: 'Stress' },
  ],
  ['balanced', 'balanced'],
  ['fine', 'fine'],
  ['anxious', 'anxious'],
  ['energetic', 'energetic'],
  [
    'fatigue',
    'fatigue',
    { system: SNOMED, code: '84229001', display: 'Fatigue' },
  ],
  ['angry', 'angry'],
  ['other', 'other mood'],
]

const MUCUS_FEELING = ['dry', 'nothing', 'wet', 'slippery']
const MUCUS_TEXTURE = ['nothing', 'creamy', 'egg white']
const MUCUS_NFP = ['t', 'O', 'f', 'S', 'S+']
const CERVIX_OPENING = ['closed', 'medium', 'open']
const CERVIX_FIRMNESS = ['hard', 'soft']
const CERVIX_POSITION = ['low', 'medium', 'high']

export function buildCycleFhirBundle(cycleDays, options = {}) {
  let observationIndex = 0
  const observations = []
  const sortedCycleDays = [...cycleDays].sort((a, b) =>
    a.date.localeCompare(b.date)
  )

  function addObservation(observation) {
    observationIndex += 1
    observations.push({
      resourceType: 'Observation',
      id: `obs-${String(observationIndex).padStart(4, '0')}`,
      status: 'final',
      ...observation,
    })
  }

  sortedCycleDays.forEach((day) => {
    addBleedingFacts(day, addObservation)
    addTemperatureFact(day, addObservation)
    addSymptomFacts(day, addObservation)
    addFertilitySignFacts(day, addObservation)
    addDailyNoteFact(day, addObservation)
  })

  const hasBleedingCore = observations.some((observation) =>
    hasProfile(observation, MENSTRUAL_BLEEDING_PROFILE)
  )

  if (!hasBleedingCore) {
    throw new Error(
      'SMART Link requires at least one recorded bleeding value.'
    )
  }

  return {
    resourceType: 'Bundle',
    id: options.id || 'drip-cycle-ig-export',
    meta: {
      profile: [BUNDLE_PROFILE],
    },
    type: 'collection',
    timestamp: options.exportedAt || new Date().toISOString(),
    entry: observations.map((observation) => ({
      fullUrl: `urn:drip:cycle-ig:${observation.id}`,
      resource: observation,
    })),
  }
}

export default function transformToCycleFhirJson(cycleDays, options = {}) {
  return JSON.stringify(buildCycleFhirBundle(cycleDays, options), null, 2)
}

function addBleedingFacts(day, addObservation) {
  const { bleeding } = day
  if (!bleeding || !isNumber(bleeding.value)) return

  addObservation({
    meta: { profile: [MENSTRUAL_BLEEDING_PROFILE] },
    category: surveyCategory(),
    code: cycleCode('menstrual-bleeding', 'Menstrual bleeding'),
    effectiveDateTime: day.date,
    valueBoolean: bleeding.exclude !== true,
  })

  if (bleeding.exclude) return

  const flow = FLOW_BY_DRIP_VALUE[bleeding.value]
  if (!flow) return

  addObservation({
    meta: { profile: [MENSTRUAL_FLOW_PROFILE] },
    category: surveyCategory(),
    code: cycleCode('menstrual-flow', 'Patient-reported menstrual flow category'),
    effectiveDateTime: day.date,
    valueCodeableConcept: {
      coding: [
        {
          system: CYCLE_CODE_SYSTEM,
          code: flow.code,
          display: flow.display,
        },
      ],
      text: flow.display.toLowerCase(),
    },
  })
}

function addTemperatureFact(day, addObservation) {
  const { temperature } = day
  if (!temperature || !isNumber(temperature.value) || temperature.exclude) {
    return
  }

  addObservation({
    meta: { profile: [BASAL_BODY_TEMPERATURE_PROFILE] },
    category: [
      {
        coding: [
          {
            system: OBSERVATION_CATEGORY,
            code: 'vital-signs',
            display: 'Vital Signs',
          },
        ],
      },
    ],
    code: {
      coding: [
        {
          system: LOINC,
          code: '8310-5',
          display: 'Body temperature',
        },
      ],
      text: 'Basal body temperature',
    },
    effectiveDateTime: dateWithOptionalTime(day.date, temperature.time),
    valueQuantity: {
      value: temperature.value,
      unit: 'Cel',
      system: UCUM,
      code: 'Cel',
    },
  })
}

function addSymptomFacts(day, addObservation) {
  addBooleanSymptomFacts(day, 'pain', PAIN_SYMPTOMS, addObservation)
  addBooleanSymptomFacts(day, 'mood', MOOD_SYMPTOMS, addObservation)
}

function addBooleanSymptomFacts(day, group, symptomMap, addObservation) {
  const source = day[group]
  if (!source) return

  symptomMap.forEach(([key, display, standardCoding]) => {
    if (source[key] !== true) return

    const text =
      key === 'other' && source.note ? `${display}: ${source.note}` : display

    addObservation({
      meta: { profile: [SYMPTOM_PROFILE] },
      category: surveyCategory(),
      code: cycleCode('symptom', 'Symptom'),
      effectiveDateTime: day.date,
      valueCodeableConcept: {
        coding: [
          ...(standardCoding ? [standardCoding] : []),
          {
            system: DRIP_CODE_SYSTEM,
            code: `${group}.${key}`,
            display,
          },
        ],
        text,
      },
    })
  })
}

function addFertilitySignFacts(day, addObservation) {
  addMucusFact(day, addObservation)
  addCervixFact(day, addObservation)
}

function addMucusFact(day, addObservation) {
  const { mucus } = day
  if (!mucus || mucus.exclude) return

  const parts = [
    labelForValue('feeling', MUCUS_FEELING, mucus.feeling),
    labelForValue('texture', MUCUS_TEXTURE, mucus.texture),
    labelForValue('nfp value', MUCUS_NFP, mucus.value),
  ].filter(Boolean)

  if (parts.length === 0) return

  addAppNativeCodeableFact({
    day,
    addObservation,
    code: 'cervical-mucus',
    display: 'Cervical mucus',
    valueCode: mucus.value === null ? 'cervical-mucus' : `mucus-${mucus.value}`,
    valueDisplay: parts.join(', '),
  })
}

function addCervixFact(day, addObservation) {
  const { cervix } = day
  if (!cervix || cervix.exclude) return

  const parts = [
    labelForValue('opening', CERVIX_OPENING, cervix.opening),
    labelForValue('firmness', CERVIX_FIRMNESS, cervix.firmness),
    labelForValue('position', CERVIX_POSITION, cervix.position),
  ].filter(Boolean)

  if (parts.length === 0) return

  addAppNativeCodeableFact({
    day,
    addObservation,
    code: 'cervix',
    display: 'Cervix observation',
    valueCode: 'cervix-observation',
    valueDisplay: parts.join(', '),
  })
}

function addDailyNoteFact(day, addObservation) {
  const note = day.note && day.note.value
  if (!note) return

  addObservation({
    meta: { profile: [FACT_PROFILE] },
    category: surveyCategory(),
    code: appCode('daily-note', 'Daily note'),
    effectiveDateTime: day.date,
    valueString: note,
  })
}

function addAppNativeCodeableFact({
  day,
  addObservation,
  code,
  display,
  valueCode,
  valueDisplay,
}) {
  addObservation({
    meta: { profile: [FACT_PROFILE] },
    category: surveyCategory(),
    code: appCode(code, display),
    effectiveDateTime: day.date,
    valueCodeableConcept: {
      coding: [
        {
          system: DRIP_CODE_SYSTEM,
          code: valueCode,
          display: valueDisplay,
        },
      ],
      text: valueDisplay,
    },
  })
}

function cycleCode(code, display) {
  return {
    coding: [
      {
        system: CYCLE_CODE_SYSTEM,
        code,
        display,
      },
    ],
    text: display,
  }
}

function appCode(code, display) {
  return {
    coding: [
      {
        system: DRIP_CODE_SYSTEM,
        code,
        display,
      },
    ],
    text: display,
  }
}

function surveyCategory() {
  return [
    {
      coding: [
        {
          system: OBSERVATION_CATEGORY,
          code: 'survey',
          display: 'Survey',
        },
      ],
    },
  ]
}

function labelForValue(label, labels, value) {
  return isNumber(value) ? `${label}: ${labels[value]}` : null
}

function dateWithOptionalTime(date, time) {
  return typeof time === 'string' && /^\d\d:\d\d$/.test(time)
    ? `${date}T${time}`
    : date
}

function hasProfile(resource, profile) {
  return resource.meta && resource.meta.profile.includes(profile)
}

function isNumber(value) {
  return typeof value === 'number'
}
