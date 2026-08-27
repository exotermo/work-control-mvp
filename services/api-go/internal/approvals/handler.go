// Package approvals implementa o gate de operação sensível descrito no README: toda ação
// sensível vira uma Approval que bloqueia até decisão humana, e a decisão gera auditoria.
package approvals

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"

	"github.com/example/work-control/services/api-go/internal/audit"
	"github.com/example/work-control/services/api-go/internal/auth"
	"github.com/example/work-control/services/api-go/internal/httpx"
	"github.com/example/work-control/services/api-go/internal/store"
)

type Handler struct {
	Queries *store.Queries
}

type ApprovalDTO struct {
	ID          string          `json:"id"`
	Title       string          `json:"title"`
	RequestedBy string          `json:"requested_by"`
	Description string          `json:"description"`
	Payload     json.RawMessage `json:"payload"`
	Decision    *string         `json:"decision"`
}

func (h *Handler) Pending(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	user := auth.FromContext(ctx)

	row, err := h.Queries.GetPendingApprovalForWorkspace(ctx, user.WorkspaceID)
	if errors.Is(err, pgx.ErrNoRows) {
		httpx.WriteJSON(w, http.StatusOK, []ApprovalDTO{})
		return
	} else if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to load pending approval")
		return
	}

	requestedBy := ""
	if row.RequestedByName.Valid {
		requestedBy = row.RequestedByName.String
	}
	httpx.WriteJSON(w, http.StatusOK, []ApprovalDTO{{
		ID:          row.ID.String(),
		Title:       row.Title,
		RequestedBy: requestedBy,
		Description: row.Description,
		Payload:     json.RawMessage(row.Payload),
	}})
}

func (h *Handler) Approve(w http.ResponseWriter, r *http.Request) {
	h.decide(w, r, store.ApprovalDecisionAPPROVED)
}

func (h *Handler) Reject(w http.ResponseWriter, r *http.Request) {
	h.decide(w, r, store.ApprovalDecisionREJECTED)
}

func (h *Handler) decide(w http.ResponseWriter, r *http.Request, decision store.ApprovalDecision) {
	ctx := r.Context()
	user := auth.FromContext(ctx)

	id, err := uuid.Parse(r.PathValue("id"))
	if err != nil {
		httpx.WriteError(w, http.StatusBadRequest, "invalid approval id")
		return
	}

	updated, err := h.Queries.DecideApproval(ctx, store.DecideApprovalParams{
		ID:              id,
		Decision:        store.NullApprovalDecision{ApprovalDecision: decision, Valid: true},
		DecidedByUserID: uuid.NullUUID{UUID: user.UserID, Valid: true},
	})
	if errors.Is(err, pgx.ErrNoRows) {
		httpx.WriteError(w, http.StatusNotFound, "approval not found")
		return
	} else if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to decide approval")
		return
	}

	audit.Log(ctx, h.Queries, user.WorkspaceID, user.UserID, "approval.decide", string(decision))

	requestedBy := ""
	if full, err := h.Queries.GetApproval(ctx, updated.ID); err == nil && full.RequestedByName.Valid {
		requestedBy = full.RequestedByName.String
	}

	httpx.WriteJSON(w, http.StatusOK, ApprovalDTO{
		ID:          updated.ID.String(),
		Title:       updated.Title,
		RequestedBy: requestedBy,
		Description: updated.Description,
		Payload:     json.RawMessage(updated.Payload),
		Decision:    decisionPtr(updated.Decision),
	})
}

func decisionPtr(d store.NullApprovalDecision) *string {
	if !d.Valid {
		return nil
	}
	s := string(d.ApprovalDecision)
	return &s
}
