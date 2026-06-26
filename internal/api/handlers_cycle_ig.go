package api

import (
	"bytes"
	"encoding/base64"
	"errors"
	"html/template"
	"image/png"
	"strconv"
	"strings"
	"time"

	"github.com/boombuler/barcode"
	"github.com/boombuler/barcode/qr"
	"github.com/gofiber/fiber/v2"
	"github.com/ovumcy/ovumcy-web/internal/services"
)

func (handler *Handler) LoadCycleIGSampleData(c *fiber.Ctx) error {
	user, ok := currentUser(c)
	if !ok {
		return handler.respondMappedError(c, unauthorizedErrorSpec())
	}
	result, err := handler.cycleIGService.LoadSampleData(c.UserContext(), user.ID, handler.requestLocation(c))
	if err != nil {
		return handler.respondCycleIGResult(c, cycleIGResultData{
			Mode:  "error",
			Error: "Could not load the synthetic sample data.",
		})
	}
	handler.logSecurityEvent(c, "cycle_ig.sample", "success", securityEventField("rows", strconv.Itoa(result.RowsCreatedOrUpdated)))
	if isHTMX(c) {
		c.Set("HX-Refresh", "true")
		return c.SendStatus(fiber.StatusNoContent)
	}
	return redirectOrJSON(c, "/settings#settings-data")
}

func (handler *Handler) PreviewCycleIGShare(c *fiber.Ctx) error {
	user, ok := currentUser(c)
	if !ok {
		return handler.respondMappedError(c, unauthorizedErrorSpec())
	}
	scope, err := handler.parseCycleIGScope(c)
	if err != nil {
		return handler.respondCycleIGResult(c, cycleIGResultData{Mode: "error", Error: "Choose a valid date range."})
	}
	summary, err := handler.cycleIGService.Preview(c.UserContext(), user.ID, scope, time.Now(), handler.requestLocation(c))
	if err != nil {
		return handler.respondCycleIGResult(c, cycleIGResultData{Mode: "error", Error: cycleIGUserError(err)})
	}
	return handler.respondCycleIGResult(c, cycleIGResultData{
		Mode:    "preview",
		Summary: summary,
		Scope:   scope,
	})
}

func (handler *Handler) CreateCycleIGShare(c *fiber.Ctx) error {
	user, ok := currentUser(c)
	if !ok {
		return handler.respondMappedError(c, unauthorizedErrorSpec())
	}
	scope, err := handler.parseCycleIGScope(c)
	if err != nil {
		return handler.respondCycleIGResult(c, cycleIGResultData{Mode: "error", Error: "Choose a valid date range."})
	}
	share, err := handler.cycleIGService.CreateShare(c.UserContext(), user.ID, scope, time.Now(), handler.requestLocation(c))
	if err != nil {
		return handler.respondCycleIGResult(c, cycleIGResultData{Mode: "error", Error: cycleIGUserError(err)})
	}
	qrDataURL, err := cycleIGQRDataURL(share.ViewerLink)
	if err != nil {
		return handler.respondCycleIGResult(c, cycleIGResultData{Mode: "error", Error: "The share was created, but the QR image could not be rendered."})
	}
	share.QRDataURL = qrDataURL
	handler.logSecurityEvent(c, "cycle_ig.share", "success", securityEventField("share_id", share.ID))
	return handler.respondCycleIGResult(c, cycleIGResultData{
		Mode:  "share",
		Share: share,
		Scope: scope,
	})
}

func (handler *Handler) RevokeCycleIGShare(c *fiber.Ctx) error {
	_, ok := currentUser(c)
	if !ok {
		return handler.respondMappedError(c, unauthorizedErrorSpec())
	}
	shareID := strings.TrimSpace(c.Params("id"))
	manageToken := strings.TrimSpace(c.FormValue("manage_token"))
	if err := handler.cycleIGService.RevokeShare(c.UserContext(), shareID, manageToken); err != nil {
		return handler.respondCycleIGResult(c, cycleIGResultData{Mode: "error", Error: "Could not stop this share. Reload Settings and try again if it is still active."})
	}
	handler.logSecurityEvent(c, "cycle_ig.revoke", "success", securityEventField("share_id", shareID))
	return handler.respondCycleIGResult(c, cycleIGResultData{
		Mode:    "stopped",
		ShareID: shareID,
	})
}

type cycleIGResultData struct {
	Mode    string
	Error   string
	Summary services.CycleIGSummary
	Share   services.CycleIGShare
	Scope   services.CycleIGScope
	ShareID string
}

func (handler *Handler) respondCycleIGResult(c *fiber.Ctx, result cycleIGResultData) error {
	if acceptsJSON(c) && !isHTMX(c) {
		if result.Mode == "error" {
			return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{"error": result.Error})
		}
		return c.JSON(fiber.Map{"ok": true, "mode": result.Mode})
	}
	return handler.renderPartial(c, "cycle_ig_share_result", handler.cycleIGResultTemplateData(result))
}

func (handler *Handler) cycleIGResultTemplateData(result cycleIGResultData) fiber.Map {
	return fiber.Map{
		"CycleIGMode":      result.Mode,
		"CycleIGError":     result.Error,
		"CycleIGSummary":   result.Summary,
		"CycleIGShare":     result.Share,
		"CycleIGQRDataURL": template.URL(result.Share.QRDataURL), // #nosec G203 -- server-generated PNG data URI
		"CycleIGShareID":   result.ShareID,
	}
}

func (handler *Handler) parseCycleIGScope(c *fiber.Ctx) (services.CycleIGScope, error) {
	from, to, err := services.ParseExportRange(strings.TrimSpace(c.FormValue("from")), strings.TrimSpace(c.FormValue("to")), handler.requestLocation(c))
	if err != nil {
		return services.CycleIGScope{}, err
	}
	scope := services.DefaultCycleIGScope()
	scope.From = from
	scope.To = to
	scope.IncludeFlow = services.ParseBoolLike(c.FormValue("include_flow"))
	scope.IncludeSymptoms = services.ParseBoolLike(c.FormValue("include_symptoms"))
	scope.IncludeBBT = services.ParseBoolLike(c.FormValue("include_bbt"))
	scope.IncludeMucus = services.ParseBoolLike(c.FormValue("include_mucus"))
	scope.IncludeMood = services.ParseBoolLike(c.FormValue("include_mood"))
	scope.IncludeCycleFactors = services.ParseBoolLike(c.FormValue("include_cycle_factors"))
	scope.IncludeNotes = services.ParseBoolLike(c.FormValue("include_notes"))
	return scope, nil
}

func cycleIGUserError(err error) string {
	switch {
	case err == nil:
		return ""
	case errors.Is(err, services.ErrCycleIGNoBleedingFacts):
		return "No stored bleeding facts are available in that range. Load sample data or choose a range with logged days."
	case errors.Is(err, services.ErrCycleIGCreateShare):
		return "Could not create the SMART Link. Check network access to shlep.exe.xyz and try again."
	default:
		return "Could not prepare SMART Link data for that range."
	}
}

func cycleIGQRDataURL(value string) (string, error) {
	code, err := qr.Encode(value, qr.M, qr.Auto)
	if err != nil {
		return "", err
	}
	scaled, err := barcode.Scale(code, 280, 280)
	if err != nil {
		return "", err
	}
	var buffer bytes.Buffer
	if err := png.Encode(&buffer, scaled); err != nil {
		return "", err
	}
	return "data:image/png;base64," + base64.StdEncoding.EncodeToString(buffer.Bytes()), nil
}
