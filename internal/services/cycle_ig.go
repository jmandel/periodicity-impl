package services

import (
	"bytes"
	"context"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"math"
	"net/http"
	"net/url"
	"sort"
	"strings"
	"time"

	"github.com/ovumcy/ovumcy-web/internal/models"
)

const (
	CycleIGShlepBaseURL   = "https://shlep.exe.xyz"
	CycleIGViewerBaseURL  = "https://cycle.fhir.me/view"
	CycleIGShareMaxUses   = 5
	CycleIGShareDays      = 7
	cycleIGCanonical      = "https://cycle.fhir.me"
	cycleIGSystem         = cycleIGCanonical + "/CodeSystem/cycle"
	ovumcyCycleIGSystem   = "https://github.com/ovumcy/ovumcy-web/CodeSystem/ovumcy-cycle-ig"
	observationCategory   = "http://terminology.hl7.org/CodeSystem/observation-category"
	snomedSystem          = "http://snomed.info/sct"
	loincSystem           = "http://loinc.org"
	ucumSystem            = "http://unitsofmeasure.org"
	bundleProfile         = cycleIGCanonical + "/StructureDefinition/period-tracking-bundle"
	factProfile           = cycleIGCanonical + "/StructureDefinition/period-tracking-fact"
	bleedingProfile       = cycleIGCanonical + "/StructureDefinition/menstrual-bleeding"
	flowProfile           = cycleIGCanonical + "/StructureDefinition/menstrual-flow"
	symptomProfile        = cycleIGCanonical + "/StructureDefinition/symptom"
	basalTempProfile      = cycleIGCanonical + "/StructureDefinition/basal-body-temperature"
	cycleIGFHIRContentTyp = "application/fhir+json"
)

var (
	ErrCycleIGNoBleedingFacts = errors.New("cycle ig requires at least one logged bleeding fact")
	ErrCycleIGNoFacts         = errors.New("cycle ig requires at least one logged fact")
	ErrCycleIGCreateShare     = errors.New("cycle ig create share failed")
	ErrCycleIGRevokeShare     = errors.New("cycle ig revoke share failed")
	ErrCycleIGResolveShare    = errors.New("cycle ig resolve share failed")
)

type CycleIGScope struct {
	From                *time.Time
	To                  *time.Time
	IncludeFlow         bool
	IncludeSymptoms     bool
	IncludeBBT          bool
	IncludeMucus        bool
	IncludeMood         bool
	IncludeCycleFactors bool
	IncludeNotes        bool
}

func DefaultCycleIGScope() CycleIGScope {
	return CycleIGScope{
		IncludeFlow:         true,
		IncludeSymptoms:     true,
		IncludeBBT:          true,
		IncludeMucus:        true,
		IncludeMood:         true,
		IncludeCycleFactors: true,
		IncludeNotes:        true,
	}
}

type CycleIGSnapshot struct {
	UserID          uint
	Scope           CycleIGScope
	Logs            []models.DailyLog
	SymptomsByID    map[uint]models.SymptomType
	TemperatureUnit string
	GeneratedAt     time.Time
}

type CycleIGSummary struct {
	SourceLogCount   int
	ObservationCount int
	DateFrom         string
	DateTo           string
	BleedingTrue     int
	BleedingFalse    int
	FlowFacts        int
	SymptomFacts     int
	BBTFacts         int
	MucusFacts       int
	MoodFacts        int
	CycleFactorFacts int
	NoteFacts        int
}

type CycleIGShare struct {
	ID          string
	FileURL     string
	ManageToken string
	ViewerLink  string
	BareSHLink  string
	KeyB64URL   string
	Ciphertext  string
	BundleJSON  string
	ExpiresAt   time.Time
	MaxUses     int
	Summary     CycleIGSummary
	QRDataURL   string
}

type CycleIGSampleResult struct {
	RowsCreatedOrUpdated int
	DateFrom             string
	DateTo               string
	BleedingTrue         int
	BleedingFalse        int
}

type CycleIGService struct {
	days     *DayService
	symptoms *SymptomService
	settings *SettingsService
	shares   *CycleIGShareClient
}

func NewCycleIGService(days *DayService, symptoms *SymptomService, settings *SettingsService, shares *CycleIGShareClient) *CycleIGService {
	if shares == nil {
		shares = NewCycleIGShareClient(CycleIGShlepBaseURL, http.DefaultClient)
	}
	return &CycleIGService{
		days:     days,
		symptoms: symptoms,
		settings: settings,
		shares:   shares,
	}
}

func (service *CycleIGService) BuildSnapshot(ctx context.Context, userID uint, scope CycleIGScope, now time.Time, location *time.Location) (CycleIGSnapshot, error) {
	logs, err := service.days.FetchLogsForOptionalRange(ctx, userID, scope.From, scope.To, location)
	if err != nil {
		return CycleIGSnapshot{}, err
	}

	symptoms, err := service.symptoms.FetchSymptoms(ctx, userID)
	if err != nil {
		return CycleIGSnapshot{}, err
	}
	symptomsByID := make(map[uint]models.SymptomType, len(symptoms))
	for _, symptom := range symptoms {
		symptomsByID[symptom.ID] = symptom
	}

	settings, err := service.settings.LoadSettings(ctx, userID)
	if err != nil {
		return CycleIGSnapshot{}, err
	}

	copiedLogs := append([]models.DailyLog(nil), logs...)
	sort.Slice(copiedLogs, func(i, j int) bool {
		return copiedLogs[i].Date.Before(copiedLogs[j].Date)
	})

	return CycleIGSnapshot{
		UserID:          userID,
		Scope:           scope,
		Logs:            copiedLogs,
		SymptomsByID:    symptomsByID,
		TemperatureUnit: NormalizeTemperatureUnit(settings.TemperatureUnit),
		GeneratedAt:     now.UTC(),
	}, nil
}

func (service *CycleIGService) Preview(ctx context.Context, userID uint, scope CycleIGScope, now time.Time, location *time.Location) (CycleIGSummary, error) {
	snapshot, err := service.BuildSnapshot(ctx, userID, scope, now, location)
	if err != nil {
		return CycleIGSummary{}, err
	}
	_, summary, err := BuildCycleIGBundle(snapshot)
	return summary, err
}

func (service *CycleIGService) CreateShare(ctx context.Context, userID uint, scope CycleIGScope, now time.Time, location *time.Location) (CycleIGShare, error) {
	snapshot, err := service.BuildSnapshot(ctx, userID, scope, now, location)
	if err != nil {
		return CycleIGShare{}, err
	}
	bundle, summary, err := BuildCycleIGBundle(snapshot)
	if err != nil {
		return CycleIGShare{}, err
	}
	bundleBytes, err := json.Marshal(bundle)
	if err != nil {
		return CycleIGShare{}, err
	}

	key, keyB64, err := NewCycleIGContentKey()
	if err != nil {
		return CycleIGShare{}, err
	}
	ciphertext, err := EncryptCycleIGBundle(bundleBytes, key)
	if err != nil {
		return CycleIGShare{}, err
	}

	expiresAt := now.UTC().Add(CycleIGShareDays * 24 * time.Hour)
	exp := expiresAt.Unix()
	hostShare, err := service.shares.Create(ctx, CycleIGCreateShareRequest{
		Ciphertext:  ciphertext,
		ContentType: cycleIGFHIRContentTyp,
		Policy: CycleIGSharePolicy{
			Exp:     exp,
			MaxUses: CycleIGShareMaxUses,
			Audit:   true,
		},
	})
	if err != nil {
		return CycleIGShare{}, fmt.Errorf("%w: %v", ErrCycleIGCreateShare, err)
	}

	link := ComposeCycleIGSHLink(hostShare.FileURL, keyB64, CycleIGSHLinkOptions{
		Flag:  "U",
		Label: "Ovumcy SMART Link",
		Exp:   exp,
	})
	viewerLink := ComposeCycleIGViewerLink(CycleIGViewerBaseURL, link)

	return CycleIGShare{
		ID:          hostShare.ID,
		FileURL:     hostShare.FileURL,
		ManageToken: hostShare.ManageToken,
		ViewerLink:  viewerLink,
		BareSHLink:  link,
		KeyB64URL:   keyB64,
		Ciphertext:  ciphertext,
		BundleJSON:  string(bundleBytes),
		ExpiresAt:   expiresAt,
		MaxUses:     CycleIGShareMaxUses,
		Summary:     summary,
	}, nil
}

func (service *CycleIGService) RevokeShare(ctx context.Context, id string, manageToken string) error {
	if err := service.shares.Revoke(ctx, strings.TrimSpace(id), strings.TrimSpace(manageToken)); err != nil {
		return fmt.Errorf("%w: %v", ErrCycleIGRevokeShare, err)
	}
	return nil
}

func (service *CycleIGService) LoadSampleData(ctx context.Context, userID uint, location *time.Location) (CycleIGSampleResult, error) {
	symptoms, err := service.symptoms.FetchSymptoms(ctx, userID)
	if err != nil {
		return CycleIGSampleResult{}, err
	}
	symptomIDByName := make(map[string]uint, len(symptoms))
	for _, symptom := range symptoms {
		symptomIDByName[cycleIGNameKey(symptom.Name)] = symptom.ID
	}

	entries := syntheticCycleIGSampleEntries(symptomIDByName)
	result := CycleIGSampleResult{}
	for _, entry := range entries {
		normalized, err := NormalizeDayEntryInput(entry.Input)
		if err != nil {
			return CycleIGSampleResult{}, err
		}
		day, err := ParseDayDate(entry.Date, location)
		if err != nil {
			return CycleIGSampleResult{}, err
		}
		dayStart, _ := DayRange(day, location)
		if _, _, err := service.days.UpsertDayEntry(ctx, userID, dayStart, normalized, location); err != nil {
			return CycleIGSampleResult{}, err
		}
		result.RowsCreatedOrUpdated++
		if normalized.IsPeriod {
			result.BleedingTrue++
		} else {
			result.BleedingFalse++
		}
		if result.DateFrom == "" || entry.Date < result.DateFrom {
			result.DateFrom = entry.Date
		}
		if entry.Date > result.DateTo {
			result.DateTo = entry.Date
		}
	}
	return result, nil
}

type cycleIGSampleEntry struct {
	Date  string
	Input DayEntryInput
}

func syntheticCycleIGSampleEntries(symptomIDByName map[string]uint) []cycleIGSampleEntry {
	cycleStarts := []string{
		"2026-01-02",
		"2026-01-30",
		"2026-02-27",
		"2026-03-27",
		"2026-04-24",
		"2026-05-22",
		"2026-06-19",
	}
	flows := []string{models.FlowSpotting, models.FlowLight, models.FlowMedium, models.FlowHeavy}
	mucusValues := []string{models.CervicalMucusDry, models.CervicalMucusMoist, models.CervicalMucusCreamy, models.CervicalMucusEggWhite}
	factorValues := [][]string{
		{models.CycleFactorStress},
		{models.CycleFactorTravel},
		{models.CycleFactorSleepDisruption},
		{models.CycleFactorMedicationChange},
		{models.CycleFactorIllness},
		{},
		{models.CycleFactorStress, models.CycleFactorSleepDisruption},
	}

	entries := make([]cycleIGSampleEntry, 0, len(cycleStarts)*7)
	for cycleIndex, start := range cycleStarts {
		startDay, _ := time.ParseInLocation(exportDateLayout, start, time.UTC)
		for dayOffset := 0; dayOffset < 7; dayOffset++ {
			day := startDay.AddDate(0, 0, dayOffset)
			input := DayEntryInput{
				IsPeriod:        dayOffset < 4,
				Flow:            models.FlowNone,
				Mood:            (cycleIndex+dayOffset)%5 + 1,
				SexActivity:     models.SexActivityNone,
				CervicalMucus:   models.CervicalMucusNone,
				PregnancyTest:   models.PregnancyTestNone,
				CycleFactorKeys: append([]string(nil), factorValues[(cycleIndex+dayOffset)%len(factorValues)]...),
				SymptomIDs:      []uint{},
			}
			if input.IsPeriod {
				input.Flow = flows[dayOffset%len(flows)]
			} else {
				input.CervicalMucus = mucusValues[(cycleIndex+dayOffset)%len(mucusValues)]
			}
			if dayOffset == 0 || dayOffset == 1 || dayOffset == 4 {
				input.BBT = 36.38 + float64(cycleIndex)*0.03 + float64(dayOffset)*0.04
			}
			input.SymptomIDs = sampleSymptomIDs(symptomIDByName, cycleIndex, dayOffset)
			if dayOffset == 2 || dayOffset == 5 {
				input.Notes = fmt.Sprintf("Synthetic Cycle IG sample cycle %d day %d", cycleIndex+1, dayOffset+1)
			}
			entries = append(entries, cycleIGSampleEntry{
				Date:  day.Format(exportDateLayout),
				Input: input,
			})
		}
	}
	return entries
}

func sampleSymptomIDs(symptomIDByName map[string]uint, cycleIndex int, dayOffset int) []uint {
	namesByDay := [][]string{
		{"cramps", "fatigue"},
		{"bloating", "backpain"},
		{"headache", "nausea"},
		{"breasttenderness", "moodswings"},
		{"insomnia"},
		{"foodcravings", "irritability"},
		{"acne"},
	}
	names := append([]string(nil), namesByDay[dayOffset%len(namesByDay)]...)
	if cycleIndex%2 == 1 {
		names = append(names, "constipation")
	}
	if cycleIndex%3 == 2 {
		names = append(names, "diarrhea")
	}
	ids := make([]uint, 0, len(names))
	for _, name := range names {
		if id := symptomIDByName[name]; id != 0 {
			ids = append(ids, id)
		}
	}
	sort.Slice(ids, func(i, j int) bool { return ids[i] < ids[j] })
	return ids
}

type fhirBundle map[string]any

func BuildCycleIGBundle(snapshot CycleIGSnapshot) (map[string]any, CycleIGSummary, error) {
	builder := cycleIGBundleBuilder{
		snapshot: snapshot,
		summary:  CycleIGSummary{SourceLogCount: len(snapshot.Logs)},
		entries:  make([]any, 0, len(snapshot.Logs)*3),
	}

	for _, logEntry := range snapshot.Logs {
		builder.addBleeding(logEntry)
		if snapshot.Scope.IncludeFlow {
			builder.addFlow(logEntry)
		}
		if snapshot.Scope.IncludeSymptoms {
			builder.addSymptoms(logEntry)
		}
		if snapshot.Scope.IncludeBBT {
			builder.addBBT(logEntry)
		}
		if snapshot.Scope.IncludeMucus {
			builder.addCervicalMucus(logEntry)
		}
		if snapshot.Scope.IncludeMood {
			builder.addMood(logEntry)
		}
		if snapshot.Scope.IncludeCycleFactors {
			builder.addCycleFactors(logEntry)
		}
		if snapshot.Scope.IncludeNotes {
			builder.addNote(logEntry)
		}
	}

	if builder.summary.BleedingTrue+builder.summary.BleedingFalse == 0 {
		return nil, builder.summary, ErrCycleIGNoBleedingFacts
	}
	if builder.summary.ObservationCount == 0 {
		return nil, builder.summary, ErrCycleIGNoFacts
	}

	bundle := map[string]any{
		"resourceType": "Bundle",
		"id":           "ovumcy-cycle-ig-export",
		"meta": map[string]any{
			"profile": []string{bundleProfile},
		},
		"type":      "collection",
		"timestamp": snapshot.GeneratedAt.UTC().Format(time.RFC3339),
		"entry":     builder.entries,
	}
	return bundle, builder.summary, nil
}

type cycleIGBundleBuilder struct {
	snapshot CycleIGSnapshot
	summary  CycleIGSummary
	index    int
	entries  []any
}

func (builder *cycleIGBundleBuilder) addBleeding(logEntry models.DailyLog) {
	date := CalendarDayKey(logEntry.Date)
	builder.addObservation(bleedingProfile, cycleCode("menstrual-bleeding", "Menstrual bleeding"), date, map[string]any{
		"valueBoolean": logEntry.IsPeriod,
	})
	if logEntry.IsPeriod {
		builder.summary.BleedingTrue++
	} else {
		builder.summary.BleedingFalse++
	}
}

func (builder *cycleIGBundleBuilder) addFlow(logEntry models.DailyLog) {
	if !logEntry.IsPeriod {
		return
	}
	flow, ok := cycleIGFlowValue(logEntry.Flow)
	if !ok {
		return
	}
	builder.addObservation(flowProfile, cycleCode("menstrual-flow", "Patient-reported menstrual flow category"), CalendarDayKey(logEntry.Date), map[string]any{
		"valueCodeableConcept": codeableConcept([]codingValue{
			{System: cycleIGSystem, Code: flow.Code, Display: flow.Display},
		}, strings.ToLower(flow.Display)),
	})
	builder.summary.FlowFacts++
}

func (builder *cycleIGBundleBuilder) addSymptoms(logEntry models.DailyLog) {
	for _, id := range logEntry.SymptomIDs {
		symptom, ok := builder.snapshot.SymptomsByID[id]
		if !ok || strings.TrimSpace(symptom.Name) == "" {
			continue
		}
		codings := []codingValue{}
		if standard, ok := cycleIGStandardSymptomCoding(symptom.Name); ok {
			codings = append(codings, standard)
		}
		codings = append(codings, codingValue{
			System:  ovumcyCycleIGSystem,
			Code:    "symptom-" + cycleIGSlug(symptom.Name),
			Display: symptom.Name,
		})
		builder.addObservation(symptomProfile, cycleCode("symptom", "Symptom"), CalendarDayKey(logEntry.Date), map[string]any{
			"valueCodeableConcept": codeableConcept(codings, symptom.Name),
		})
		builder.summary.SymptomFacts++
	}
}

func (builder *cycleIGBundleBuilder) addBBT(logEntry models.DailyLog) {
	if !IsValidDayBBT(logEntry.BBT) || logEntry.BBT == 0 || math.IsNaN(logEntry.BBT) || math.IsInf(logEntry.BBT, 0) {
		return
	}
	unit, code := "Cel", "Cel"
	if NormalizeTemperatureUnit(builder.snapshot.TemperatureUnit) == "f" {
		unit, code = "°F", "[degF]"
	}
	builder.addObservation(basalTempProfile, codeableConcept([]codingValue{
		{System: loincSystem, Code: "8310-5", Display: "Body temperature"},
	}, "Basal body temperature"), CalendarDayKey(logEntry.Date), map[string]any{
		"category": []any{map[string]any{
			"coding": []any{map[string]any{
				"system":  observationCategory,
				"code":    "vital-signs",
				"display": "Vital Signs",
			}},
		}},
		"valueQuantity": map[string]any{
			"value":  logEntry.BBT,
			"unit":   unit,
			"system": ucumSystem,
			"code":   code,
		},
	})
	builder.summary.BBTFacts++
}

func (builder *cycleIGBundleBuilder) addCervicalMucus(logEntry models.DailyLog) {
	mucus := NormalizeDayCervicalMucus(logEntry.CervicalMucus)
	if mucus == "" || mucus == models.CervicalMucusNone {
		return
	}
	builder.addAppCodeableFact(logEntry, "cervical-mucus", "Cervical mucus", "cervical-mucus-"+mucus, cycleIGTitle(mucus))
	builder.summary.MucusFacts++
}

func (builder *cycleIGBundleBuilder) addMood(logEntry models.DailyLog) {
	if !IsValidDayMood(logEntry.Mood) || logEntry.Mood == 0 {
		return
	}
	builder.addObservation(factProfile, appCode("mood-rating", "Ovumcy mood rating"), CalendarDayKey(logEntry.Date), map[string]any{
		"valueInteger": logEntry.Mood,
	})
	builder.summary.MoodFacts++
}

func (builder *cycleIGBundleBuilder) addCycleFactors(logEntry models.DailyLog) {
	factors, ok := NormalizeDayCycleFactorKeys(logEntry.CycleFactorKeys)
	if !ok {
		return
	}
	for _, factor := range factors {
		builder.addAppCodeableFact(logEntry, "cycle-factor", "Cycle factor", "cycle-factor-"+factor, cycleIGTitle(factor))
		builder.summary.CycleFactorFacts++
	}
}

func (builder *cycleIGBundleBuilder) addNote(logEntry models.DailyLog) {
	note := strings.TrimSpace(logEntry.Notes)
	if note == "" {
		return
	}
	builder.addObservation(factProfile, appCode("daily-note", "Daily note"), CalendarDayKey(logEntry.Date), map[string]any{
		"valueString": note,
	})
	builder.summary.NoteFacts++
}

func (builder *cycleIGBundleBuilder) addAppCodeableFact(logEntry models.DailyLog, code string, display string, valueCode string, valueDisplay string) {
	builder.addObservation(factProfile, appCode(code, display), CalendarDayKey(logEntry.Date), map[string]any{
		"valueCodeableConcept": codeableConcept([]codingValue{
			{System: ovumcyCycleIGSystem, Code: valueCode, Display: valueDisplay},
		}, valueDisplay),
	})
}

func (builder *cycleIGBundleBuilder) addObservation(profile string, code map[string]any, date string, values map[string]any) {
	builder.index++
	builder.summary.ObservationCount++
	if builder.summary.DateFrom == "" || date < builder.summary.DateFrom {
		builder.summary.DateFrom = date
	}
	if date > builder.summary.DateTo {
		builder.summary.DateTo = date
	}
	resource := map[string]any{
		"resourceType":       "Observation",
		"id":                 fmt.Sprintf("obs-%04d", builder.index),
		"meta":               map[string]any{"profile": []string{profile}},
		"status":             "final",
		"category":           surveyCategory(),
		"code":               code,
		"effectiveDateTime":  date,
		"issued":             builder.snapshot.GeneratedAt.UTC().Format(time.RFC3339),
		"performer":          []any{},
		"interpretation":     []any{},
		"referenceRange":     []any{},
		"component":          []any{},
		"bodySite":           nil,
		"method":             nil,
		"specimen":           nil,
		"device":             nil,
		"hasMember":          []any{},
		"derivedFrom":        []any{},
		"note":               []any{},
		"extension":          []any{},
		"modifierExtension":  []any{},
		"contained":          []any{},
		"identifier":         []any{},
		"basedOn":            []any{},
		"partOf":             []any{},
		"focus":              []any{},
		"triggeredBy":        []any{},
		"dataAbsentReason":   nil,
		"bodyStructure":      nil,
		"referenceRangeNote": nil,
	}
	for key, value := range values {
		if key == "category" {
			resource[key] = value
			continue
		}
		resource[key] = value
	}
	removeNilAndEmptyFHIRFields(resource)
	builder.entries = append(builder.entries, map[string]any{
		"fullUrl":  "urn:ovumcy:cycle-ig:" + resource["id"].(string),
		"resource": resource,
	})
}

func removeNilAndEmptyFHIRFields(resource map[string]any) {
	for key, value := range resource {
		if value == nil {
			delete(resource, key)
			continue
		}
		switch typed := value.(type) {
		case []any:
			if len(typed) == 0 {
				delete(resource, key)
			}
		}
	}
}

type flowValue struct {
	Code    string
	Display string
}

func cycleIGFlowValue(flow string) (flowValue, bool) {
	switch NormalizeDayFlow(flow) {
	case models.FlowNone:
		return flowValue{Code: "flow-none", Display: "None"}, true
	case models.FlowSpotting:
		return flowValue{Code: "flow-spotting", Display: "Spotting"}, true
	case models.FlowLight:
		return flowValue{Code: "flow-light", Display: "Light"}, true
	case models.FlowMedium:
		return flowValue{Code: "flow-moderate", Display: "Moderate"}, true
	case models.FlowHeavy:
		return flowValue{Code: "flow-heavy", Display: "Heavy"}, true
	default:
		return flowValue{}, false
	}
}

func cycleIGStandardSymptomCoding(name string) (codingValue, bool) {
	switch cycleIGNameKey(name) {
	case "cramps":
		return codingValue{System: snomedSystem, Code: "431416001", Display: "Menstrual cramp"}, true
	case "headache":
		return codingValue{System: snomedSystem, Code: "25064002", Display: "Headache"}, true
	case "fatigue":
		return codingValue{System: snomedSystem, Code: "84229001", Display: "Fatigue"}, true
	case "bloating":
		return codingValue{System: snomedSystem, Code: "116289008", Display: "Abdominal bloating"}, true
	case "nausea":
		return codingValue{System: snomedSystem, Code: "422587007", Display: "Nausea"}, true
	case "breasttenderness":
		return codingValue{System: snomedSystem, Code: "55222007", Display: "Breast tenderness"}, true
	case "acne":
		return codingValue{System: snomedSystem, Code: "11381005", Display: "Acne"}, true
	default:
		return codingValue{}, false
	}
}

type codingValue struct {
	System  string
	Code    string
	Display string
}

func surveyCategory() []any {
	return []any{map[string]any{
		"coding": []any{map[string]any{
			"system":  observationCategory,
			"code":    "survey",
			"display": "Survey",
		}},
	}}
}

func cycleCode(code string, display string) map[string]any {
	return codeableConcept([]codingValue{{System: cycleIGSystem, Code: code, Display: display}}, display)
}

func appCode(code string, display string) map[string]any {
	return codeableConcept([]codingValue{{System: ovumcyCycleIGSystem, Code: code, Display: display}}, display)
}

func codeableConcept(codings []codingValue, text string) map[string]any {
	items := make([]any, 0, len(codings))
	for _, coding := range codings {
		items = append(items, map[string]any{
			"system":  coding.System,
			"code":    coding.Code,
			"display": coding.Display,
		})
	}
	return map[string]any{
		"coding": items,
		"text":   text,
	}
}

func cycleIGNameKey(value string) string {
	var builder strings.Builder
	for _, r := range strings.ToLower(strings.TrimSpace(value)) {
		if r >= 'a' && r <= 'z' || r >= '0' && r <= '9' {
			builder.WriteRune(r)
		}
	}
	return builder.String()
}

func cycleIGSlug(value string) string {
	var builder strings.Builder
	lastDash := false
	for _, r := range strings.ToLower(strings.TrimSpace(value)) {
		isAlnum := r >= 'a' && r <= 'z' || r >= '0' && r <= '9'
		if isAlnum {
			builder.WriteRune(r)
			lastDash = false
			continue
		}
		if !lastDash && builder.Len() > 0 {
			builder.WriteByte('-')
			lastDash = true
		}
	}
	slug := strings.Trim(builder.String(), "-")
	if slug == "" {
		return "value"
	}
	return slug
}

func cycleIGTitle(value string) string {
	parts := strings.FieldsFunc(value, func(r rune) bool {
		return r == '_' || r == '-' || r == ' '
	})
	for i, part := range parts {
		if part == "" {
			continue
		}
		parts[i] = strings.ToUpper(part[:1]) + part[1:]
	}
	return strings.Join(parts, " ")
}

type CycleIGShareClient struct {
	baseURL    string
	httpClient *http.Client
}

type CycleIGSharePolicy struct {
	Exp     int64 `json:"exp,omitempty"`
	MaxUses int   `json:"maxUses,omitempty"`
	Audit   bool  `json:"audit,omitempty"`
}

type CycleIGCreateShareRequest struct {
	Ciphertext  string             `json:"ciphertext"`
	ContentType string             `json:"contentType,omitempty"`
	Policy      CycleIGSharePolicy `json:"policy,omitempty"`
}

type CycleIGCreateShareResponse struct {
	ID          string `json:"id"`
	FileURL     string `json:"fileUrl"`
	ManageToken string `json:"manageToken"`
}

func NewCycleIGShareClient(baseURL string, httpClient *http.Client) *CycleIGShareClient {
	if httpClient == nil {
		httpClient = http.DefaultClient
	}
	return &CycleIGShareClient{
		baseURL:    strings.TrimRight(strings.TrimSpace(baseURL), "/"),
		httpClient: httpClient,
	}
}

func (client *CycleIGShareClient) Create(ctx context.Context, payload CycleIGCreateShareRequest) (CycleIGCreateShareResponse, error) {
	body, err := json.Marshal(payload)
	if err != nil {
		return CycleIGCreateShareResponse{}, err
	}
	request, err := http.NewRequestWithContext(ctx, http.MethodPost, client.baseURL+"/shares", bytes.NewReader(body))
	if err != nil {
		return CycleIGCreateShareResponse{}, err
	}
	request.Header.Set("content-type", "application/json")
	request.Header.Set("accept", "application/json")

	response, err := client.httpClient.Do(request)
	if err != nil {
		return CycleIGCreateShareResponse{}, err
	}
	defer response.Body.Close()
	responseBody, _ := io.ReadAll(io.LimitReader(response.Body, 1<<20))
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return CycleIGCreateShareResponse{}, fmt.Errorf("shlep create status %d: %s", response.StatusCode, strings.TrimSpace(string(responseBody)))
	}

	var decoded CycleIGCreateShareResponse
	if err := json.Unmarshal(responseBody, &decoded); err != nil {
		return CycleIGCreateShareResponse{}, err
	}
	if strings.TrimSpace(decoded.ID) == "" || strings.TrimSpace(decoded.FileURL) == "" || strings.TrimSpace(decoded.ManageToken) == "" {
		return CycleIGCreateShareResponse{}, errors.New("shlep create response missing fields")
	}
	return decoded, nil
}

func (client *CycleIGShareClient) Revoke(ctx context.Context, id string, manageToken string) error {
	if strings.TrimSpace(id) == "" || strings.TrimSpace(manageToken) == "" {
		return errors.New("share id and manage token are required")
	}
	request, err := http.NewRequestWithContext(ctx, http.MethodDelete, client.baseURL+"/shares/"+url.PathEscape(id), nil)
	if err != nil {
		return err
	}
	request.Header.Set("authorization", "Bearer "+manageToken)
	request.Header.Set("accept", "application/json")

	response, err := client.httpClient.Do(request)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	responseBody, _ := io.ReadAll(io.LimitReader(response.Body, 1<<20))
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return fmt.Errorf("shlep revoke status %d: %s", response.StatusCode, strings.TrimSpace(string(responseBody)))
	}
	return nil
}

func (client *CycleIGShareClient) ResolveCiphertext(ctx context.Context, fileURL string, recipient string) (string, error) {
	parsed, err := url.Parse(fileURL)
	if err != nil {
		return "", err
	}
	query := parsed.Query()
	query.Set("recipient", recipient)
	parsed.RawQuery = query.Encode()

	request, err := http.NewRequestWithContext(ctx, http.MethodGet, parsed.String(), nil)
	if err != nil {
		return "", err
	}
	response, err := client.httpClient.Do(request)
	if err != nil {
		return "", err
	}
	defer response.Body.Close()
	body, _ := io.ReadAll(io.LimitReader(response.Body, 8<<20))
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return "", fmt.Errorf("%w: status %d: %s", ErrCycleIGResolveShare, response.StatusCode, strings.TrimSpace(string(body)))
	}
	return string(body), nil
}

func NewCycleIGContentKey() ([]byte, string, error) {
	key := make([]byte, 32)
	if _, err := rand.Read(key); err != nil {
		return nil, "", err
	}
	return key, base64.RawURLEncoding.EncodeToString(key), nil
}

func EncryptCycleIGBundle(plaintext []byte, key []byte) (string, error) {
	if len(key) != 32 {
		return "", errors.New("A256GCM key must be 32 bytes")
	}
	protectedHeader := base64.RawURLEncoding.EncodeToString([]byte(`{"alg":"dir","enc":"A256GCM","cty":"application/fhir+json"}`))
	block, err := aes.NewCipher(key)
	if err != nil {
		return "", err
	}
	aead, err := cipher.NewGCM(block)
	if err != nil {
		return "", err
	}
	nonce := make([]byte, aead.NonceSize())
	if _, err := rand.Read(nonce); err != nil {
		return "", err
	}
	sealed := aead.Seal(nil, nonce, plaintext, []byte(protectedHeader))
	tagSize := aead.Overhead()
	ciphertext := sealed[:len(sealed)-tagSize]
	tag := sealed[len(sealed)-tagSize:]
	return strings.Join([]string{
		protectedHeader,
		"",
		base64.RawURLEncoding.EncodeToString(nonce),
		base64.RawURLEncoding.EncodeToString(ciphertext),
		base64.RawURLEncoding.EncodeToString(tag),
	}, "."), nil
}

func DecryptCycleIGBundle(compactJWE string, key []byte) ([]byte, error) {
	parts := strings.Split(compactJWE, ".")
	if len(parts) != 5 {
		return nil, errors.New("compact JWE must have five parts")
	}
	nonce, err := base64.RawURLEncoding.DecodeString(parts[2])
	if err != nil {
		return nil, err
	}
	ciphertext, err := base64.RawURLEncoding.DecodeString(parts[3])
	if err != nil {
		return nil, err
	}
	tag, err := base64.RawURLEncoding.DecodeString(parts[4])
	if err != nil {
		return nil, err
	}
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}
	aead, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}
	sealed := append(append([]byte{}, ciphertext...), tag...)
	return aead.Open(nil, nonce, sealed, []byte(parts[0]))
}

type CycleIGSHLinkOptions struct {
	Flag  string
	Label string
	Exp   int64
}

type cycleIGSHLinkPayload struct {
	URL   string `json:"url"`
	Key   string `json:"key"`
	Flag  string `json:"flag,omitempty"`
	Label string `json:"label,omitempty"`
	Exp   int64  `json:"exp,omitempty"`
	V     int    `json:"v"`
}

func ComposeCycleIGSHLink(fileURL string, keyB64URL string, options CycleIGSHLinkOptions) string {
	flag := strings.TrimSpace(options.Flag)
	if flag == "" {
		flag = "U"
	}
	payload := cycleIGSHLinkPayload{
		URL:   fileURL,
		Key:   keyB64URL,
		Flag:  flag,
		Label: strings.TrimSpace(options.Label),
		Exp:   options.Exp,
		V:     1,
	}
	encoded, _ := json.Marshal(payload)
	return "shlink:/" + base64.RawURLEncoding.EncodeToString(encoded)
}

func ComposeCycleIGViewerLink(viewerBase string, bareSHLink string) string {
	return strings.TrimRight(viewerBase, "/") + "#" + bareSHLink
}

func ParseCycleIGSHLink(input string) (cycleIGSHLinkPayload, error) {
	index := strings.Index(input, "shlink:/")
	if index < 0 {
		return cycleIGSHLinkPayload{}, errors.New("no shlink:/ found")
	}
	raw := strings.TrimSpace(input[index+len("shlink:/"):])
	decoded, err := base64.RawURLEncoding.DecodeString(raw)
	if err != nil {
		return cycleIGSHLinkPayload{}, err
	}
	var payload cycleIGSHLinkPayload
	if err := json.Unmarshal(decoded, &payload); err != nil {
		return cycleIGSHLinkPayload{}, err
	}
	return payload, nil
}
