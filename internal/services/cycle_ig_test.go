package services

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/ovumcy/ovumcy-web/internal/models"
)

func TestCycleIGBundleMapsStoredFactsAndOmitsUnsupportedFields(t *testing.T) {
	snapshot := CycleIGSnapshot{
		UserID:          9,
		TemperatureUnit: "c",
		GeneratedAt:     time.Date(2026, 6, 25, 12, 0, 0, 0, time.UTC),
		Scope:           DefaultCycleIGScope(),
		SymptomsByID: map[uint]models.SymptomType{
			1: {ID: 1, Name: "Cramps", IsBuiltin: true},
			2: {ID: 2, Name: "Mood swings", IsBuiltin: true},
		},
		Logs: []models.DailyLog{
			{
				Date:            mustCycleIGDay(t, "2026-02-10"),
				IsPeriod:        true,
				CycleStart:      true,
				IsUncertain:     true,
				Flow:            models.FlowHeavy,
				Mood:            4,
				SexActivity:     models.SexActivityUnprotected,
				BBT:             36.62,
				CervicalMucus:   models.CervicalMucusEggWhite,
				PregnancyTest:   models.PregnancyTestPositive,
				CycleFactorKeys: []string{models.CycleFactorStress},
				SymptomIDs:      []uint{1, 2},
				Notes:           "synthetic note",
			},
			{
				Date:            mustCycleIGDay(t, "2026-02-11"),
				IsPeriod:        false,
				Flow:            models.FlowNone,
				SexActivity:     models.SexActivityProtected,
				PregnancyTest:   models.PregnancyTestNegative,
				CycleFactorKeys: []string{},
				SymptomIDs:      []uint{},
			},
		},
	}

	bundle, summary, err := BuildCycleIGBundle(snapshot)
	if err != nil {
		t.Fatalf("BuildCycleIGBundle returned error: %v", err)
	}
	if summary.SourceLogCount != 2 || summary.ObservationCount != 10 {
		t.Fatalf("unexpected summary counts: %+v", summary)
	}
	if summary.BleedingTrue != 1 || summary.BleedingFalse != 1 || summary.FlowFacts != 1 ||
		summary.SymptomFacts != 2 || summary.BBTFacts != 1 || summary.MucusFacts != 1 ||
		summary.MoodFacts != 1 || summary.CycleFactorFacts != 1 || summary.NoteFacts != 1 {
		t.Fatalf("unexpected fact counts: %+v", summary)
	}

	serialized, err := json.Marshal(bundle)
	if err != nil {
		t.Fatalf("marshal bundle: %v", err)
	}
	payload := string(serialized)
	for _, want := range []string{
		`"https://cycle.fhir.me/StructureDefinition/period-tracking-bundle"`,
		`"menstrual-bleeding"`,
		`"flow-heavy"`,
		`"431416001"`,
		`"mood-rating"`,
		`"cervical-mucus-eggwhite"`,
		`"cycle-factor-stress"`,
		`"synthetic note"`,
	} {
		if !strings.Contains(payload, want) {
			t.Fatalf("expected bundle to contain %q in %s", want, payload)
		}
	}
	for _, omitted := range []string{"unprotected", "protected", "pregnancy", "cycle_start", "is_uncertain"} {
		if strings.Contains(payload, omitted) {
			t.Fatalf("bundle leaked omitted source field %q in %s", omitted, payload)
		}
	}
}

func TestCycleIGBundleRequiresStoredBleedingFacts(t *testing.T) {
	_, _, err := BuildCycleIGBundle(CycleIGSnapshot{
		Scope:       DefaultCycleIGScope(),
		GeneratedAt: time.Date(2026, 6, 25, 12, 0, 0, 0, time.UTC),
	})
	if err == nil {
		t.Fatal("expected error for empty snapshot")
	}
}

func TestCycleIGCryptoAndSHLinkRoundTrip(t *testing.T) {
	key := []byte("0123456789abcdef0123456789abcdef")
	plaintext := []byte(`{"resourceType":"Bundle","type":"collection"}`)
	jwe, err := EncryptCycleIGBundle(plaintext, key)
	if err != nil {
		t.Fatalf("EncryptCycleIGBundle returned error: %v", err)
	}
	if strings.Contains(jwe, "resourceType") || strings.Contains(jwe, "Bundle") {
		t.Fatalf("compact JWE leaked plaintext: %s", jwe)
	}
	decrypted, err := DecryptCycleIGBundle(jwe, key)
	if err != nil {
		t.Fatalf("DecryptCycleIGBundle returned error: %v", err)
	}
	if string(decrypted) != string(plaintext) {
		t.Fatalf("decrypt mismatch: %s", decrypted)
	}

	keyB64 := "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY"
	bare := ComposeCycleIGSHLink("https://shlep.example/shl/abc", keyB64, CycleIGSHLinkOptions{
		Flag:  "U",
		Label: "Ovumcy test",
		Exp:   1780000000,
	})
	viewer := ComposeCycleIGViewerLink("https://cycle.fhir.me/view/", bare)
	payload, err := ParseCycleIGSHLink(viewer)
	if err != nil {
		t.Fatalf("ParseCycleIGSHLink returned error: %v", err)
	}
	if payload.URL != "https://shlep.example/shl/abc" || payload.Key != keyB64 || payload.Flag != "U" || payload.Label != "Ovumcy test" || payload.Exp != 1780000000 {
		t.Fatalf("unexpected shlink payload: %+v", payload)
	}
	if strings.Contains(strings.Split(viewer, "#")[0], keyB64) {
		t.Fatalf("key must live only in fragment: %s", viewer)
	}
}

func TestCycleIGShareClientFakeHostCreateResolveRevokeAndMaxUse(t *testing.T) {
	host := newFakeCycleIGHost(t)
	client := NewCycleIGShareClient(host.server.URL, host.server.Client())
	ctx := context.Background()

	created, err := client.Create(ctx, CycleIGCreateShareRequest{
		Ciphertext:  "jwe.fake",
		ContentType: cycleIGFHIRContentTyp,
		Policy:      CycleIGSharePolicy{MaxUses: 2, Exp: time.Now().Add(time.Hour).Unix()},
	})
	if err != nil {
		t.Fatalf("Create returned error: %v", err)
	}
	if host.plaintextSeen {
		t.Fatal("fake host observed plaintext in uploaded ciphertext")
	}
	if got, err := client.ResolveCiphertext(ctx, created.FileURL, "test one"); err != nil || got != "jwe.fake" {
		t.Fatalf("first resolve got %q err %v", got, err)
	}
	if got, err := client.ResolveCiphertext(ctx, created.FileURL, "test two"); err != nil || got != "jwe.fake" {
		t.Fatalf("second resolve got %q err %v", got, err)
	}
	if _, err := client.ResolveCiphertext(ctx, created.FileURL, "test three"); err == nil {
		t.Fatal("expected max-use resolve to fail")
	}

	created, err = client.Create(ctx, CycleIGCreateShareRequest{
		Ciphertext:  "jwe.second",
		ContentType: cycleIGFHIRContentTyp,
		Policy:      CycleIGSharePolicy{MaxUses: 5, Exp: time.Now().Add(time.Hour).Unix()},
	})
	if err != nil {
		t.Fatalf("second Create returned error: %v", err)
	}
	if err := client.Revoke(ctx, created.ID, created.ManageToken); err != nil {
		t.Fatalf("Revoke returned error: %v", err)
	}
	if _, err := client.ResolveCiphertext(ctx, created.FileURL, "after revoke"); err == nil {
		t.Fatal("expected revoked share resolve to fail")
	}
}

func TestCycleIGLiveShlepCreateResolveDecryptRevokeAndMaxUse(t *testing.T) {
	if os.Getenv("LIVE_CYCLE_IG") != "1" {
		t.Skip("set LIVE_CYCLE_IG=1 to exercise public shlep")
	}
	ctx := context.Background()
	client := NewCycleIGShareClient(CycleIGShlepBaseURL, http.DefaultClient)
	key, keyB64, err := NewCycleIGContentKey()
	if err != nil {
		t.Fatalf("NewCycleIGContentKey: %v", err)
	}
	plaintext := []byte(`{"resourceType":"Bundle","type":"collection","entry":[]}`)
	jwe, err := EncryptCycleIGBundle(plaintext, key)
	if err != nil {
		t.Fatalf("EncryptCycleIGBundle: %v", err)
	}
	share, err := client.Create(ctx, CycleIGCreateShareRequest{
		Ciphertext:  jwe,
		ContentType: cycleIGFHIRContentTyp,
		Policy:      CycleIGSharePolicy{Exp: time.Now().UTC().Add(time.Hour).Unix(), MaxUses: 5, Audit: true},
	})
	if err != nil {
		t.Fatalf("live create: %v", err)
	}
	payload := ComposeCycleIGSHLink(share.FileURL, keyB64, CycleIGSHLinkOptions{Flag: "U", Label: "Ovumcy live test"})
	if strings.Contains(share.FileURL, keyB64) || strings.Contains(payload[:strings.Index(payload, ":/")+2], keyB64) {
		t.Fatalf("key leaked outside shlink payload")
	}
	fetched, err := client.ResolveCiphertext(ctx, share.FileURL, "ovumcy live test")
	if err != nil {
		t.Fatalf("live resolve: %v", err)
	}
	if strings.Contains(fetched, "resourceType") {
		t.Fatalf("host returned plaintext instead of compact JWE")
	}
	decrypted, err := DecryptCycleIGBundle(fetched, key)
	if err != nil {
		t.Fatalf("live decrypt: %v", err)
	}
	if string(decrypted) != string(plaintext) {
		t.Fatalf("live decrypt mismatch: %s", decrypted)
	}
	if err := client.Revoke(ctx, share.ID, share.ManageToken); err != nil {
		t.Fatalf("live revoke: %v", err)
	}
	if _, err := client.ResolveCiphertext(ctx, share.FileURL, "after revoke"); err == nil {
		t.Fatal("expected live revoked share to stop resolving")
	}

	maxOne, err := client.Create(ctx, CycleIGCreateShareRequest{
		Ciphertext:  jwe,
		ContentType: cycleIGFHIRContentTyp,
		Policy:      CycleIGSharePolicy{Exp: time.Now().UTC().Add(time.Hour).Unix(), MaxUses: 1, Audit: true},
	})
	if err != nil {
		t.Fatalf("live max-use create: %v", err)
	}
	if _, err := client.ResolveCiphertext(ctx, maxOne.FileURL, "first use"); err != nil {
		t.Fatalf("live first max-use resolve: %v", err)
	}
	if _, err := client.ResolveCiphertext(ctx, maxOne.FileURL, "second use"); err == nil {
		t.Fatal("expected live max-use share to stop after one resolve")
	}
}

type fakeCycleIGHost struct {
	t             *testing.T
	server        *httptest.Server
	nextID        int
	shares        map[string]*fakeCycleIGShare
	plaintextSeen bool
}

type fakeCycleIGShare struct {
	id          string
	token       string
	ciphertext  string
	maxUses     int
	useCount    int
	revoked     bool
	contentType string
}

func newFakeCycleIGHost(t *testing.T) *fakeCycleIGHost {
	host := &fakeCycleIGHost{t: t, shares: make(map[string]*fakeCycleIGShare)}
	host.server = httptest.NewServer(http.HandlerFunc(host.serveHTTP))
	t.Cleanup(host.server.Close)
	return host
}

func (host *fakeCycleIGHost) serveHTTP(response http.ResponseWriter, request *http.Request) {
	switch {
	case request.Method == http.MethodPost && request.URL.Path == "/shares":
		host.handleCreate(response, request)
	case request.Method == http.MethodDelete && strings.HasPrefix(request.URL.Path, "/shares/"):
		host.handleRevoke(response, request)
	case request.Method == http.MethodGet && strings.HasPrefix(request.URL.Path, "/shl/"):
		host.handleResolve(response, request)
	default:
		http.NotFound(response, request)
	}
}

func (host *fakeCycleIGHost) handleCreate(response http.ResponseWriter, request *http.Request) {
	var payload CycleIGCreateShareRequest
	if err := json.NewDecoder(request.Body).Decode(&payload); err != nil {
		http.Error(response, err.Error(), http.StatusBadRequest)
		return
	}
	if strings.Contains(payload.Ciphertext, "resourceType") || strings.Contains(payload.Ciphertext, "menstrual-bleeding") {
		host.plaintextSeen = true
	}
	host.nextID++
	id := "share-test"
	if host.nextID > 1 {
		id = "share-test-2"
	}
	token := "manage-" + id
	host.shares[id] = &fakeCycleIGShare{
		id:          id,
		token:       token,
		ciphertext:  payload.Ciphertext,
		maxUses:     payload.Policy.MaxUses,
		contentType: payload.ContentType,
	}
	response.Header().Set("content-type", "application/json")
	_ = json.NewEncoder(response).Encode(CycleIGCreateShareResponse{
		ID:          id,
		FileURL:     host.server.URL + "/shl/" + id,
		ManageToken: token,
	})
}

func (host *fakeCycleIGHost) handleRevoke(response http.ResponseWriter, request *http.Request) {
	id := strings.TrimPrefix(request.URL.Path, "/shares/")
	share := host.shares[id]
	if share == nil || request.Header.Get("authorization") != "Bearer "+share.token {
		http.NotFound(response, request)
		return
	}
	share.revoked = true
	response.Header().Set("content-type", "application/json")
	_, _ = response.Write([]byte(`{"ok":true}`))
}

func (host *fakeCycleIGHost) handleResolve(response http.ResponseWriter, request *http.Request) {
	id := strings.TrimPrefix(request.URL.Path, "/shl/")
	share := host.shares[id]
	if share == nil || share.revoked || strings.TrimSpace(request.URL.Query().Get("recipient")) == "" {
		http.NotFound(response, request)
		return
	}
	if share.maxUses > 0 && share.useCount >= share.maxUses {
		http.NotFound(response, request)
		return
	}
	share.useCount++
	response.Header().Set("content-type", "application/jose")
	_, _ = response.Write([]byte(share.ciphertext))
}

func mustCycleIGDay(t *testing.T, raw string) time.Time {
	t.Helper()
	day, err := time.ParseInLocation(exportDateLayout, raw, time.UTC)
	if err != nil {
		t.Fatalf("parse test day %q: %v", raw, err)
	}
	return day
}
