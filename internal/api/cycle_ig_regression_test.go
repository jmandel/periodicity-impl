package api

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"
	"time"

	"github.com/ovumcy/ovumcy-web/internal/models"
	"github.com/ovumcy/ovumcy-web/internal/services"
)

func TestCycleIGShareResponseRendersQRDataURL(t *testing.T) {
	t.Parallel()

	host := newAPIFakeCycleIGHost(t)
	ctx := newSettingsSecurityTestContextWithOptions(t, "cycle-ig-qr@example.com", onboardingTestAppOptions{
		enableCSRF:    true,
		cycleIGShares: services.NewCycleIGShareClient(host.server.URL, host.server.Client()),
	})

	logEntry := models.DailyLog{
		UserID:   ctx.user.ID,
		Date:     time.Date(2026, time.June, 19, 0, 0, 0, 0, time.UTC),
		IsPeriod: true,
		Flow:     models.FlowLight,
	}
	if err := ctx.database.Create(&logEntry).Error; err != nil {
		t.Fatalf("create cycle ig daily log: %v", err)
	}

	response := settingsFormRequestWithCSRF(t, ctx, http.MethodPost, "/api/v1/cycle-ig/shares", url.Values{
		"include_flow": {"true"},
	}, map[string]string{
		"HX-Request": "true",
	})
	defer response.Body.Close()

	assertStatusCode(t, response, http.StatusOK)
	body := mustReadBodyString(t, response.Body)
	assertBodyContainsAll(t, body,
		bodyStringMatch{fragment: `data-cycle-ig-result="share"`, message: "expected share result partial"},
		bodyStringMatch{fragment: `src="data:image/png;base64,`, message: "expected QR image data URL to render without template URL sanitization"},
		bodyStringMatch{fragment: `https://cycle.fhir.me/view#shlink:/`, message: "expected viewer-prefixed SHLink"},
	)
	assertBodyNotContainsAll(t, body,
		bodyStringMatch{fragment: `#ZgotmplZ`, message: "QR data URL was sanitized by html/template"},
		bodyStringMatch{fragment: `resourceType`, message: "share response should not include plaintext FHIR"},
	)
	if host.plaintextSeen {
		t.Fatal("fake shlep host saw plaintext instead of compact JWE")
	}
}

type apiFakeCycleIGHost struct {
	t             *testing.T
	server        *httptest.Server
	plaintextSeen bool
}

func newAPIFakeCycleIGHost(t *testing.T) *apiFakeCycleIGHost {
	t.Helper()

	host := &apiFakeCycleIGHost{t: t}
	host.server = httptest.NewServer(http.HandlerFunc(host.serveHTTP))
	t.Cleanup(host.server.Close)
	return host
}

func (host *apiFakeCycleIGHost) serveHTTP(response http.ResponseWriter, request *http.Request) {
	if request.Method == http.MethodPost && request.URL.Path == "/shares" {
		host.handleCreate(response, request)
		return
	}
	http.NotFound(response, request)
}

func (host *apiFakeCycleIGHost) handleCreate(response http.ResponseWriter, request *http.Request) {
	var payload services.CycleIGCreateShareRequest
	if err := json.NewDecoder(request.Body).Decode(&payload); err != nil {
		http.Error(response, err.Error(), http.StatusBadRequest)
		return
	}
	if strings.Contains(payload.Ciphertext, "resourceType") || strings.Contains(payload.Ciphertext, "menstrual-bleeding") {
		host.plaintextSeen = true
	}
	response.Header().Set("content-type", "application/json")
	_ = json.NewEncoder(response).Encode(services.CycleIGCreateShareResponse{
		ID:          "cycle-ig-test-share",
		FileURL:     host.server.URL + "/shl/cycle-ig-test-share",
		ManageToken: "manage-cycle-ig-test-share",
	})
}
