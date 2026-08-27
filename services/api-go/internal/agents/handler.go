// Package agents expõe os agentes ativos do workspace — alimenta AgentRepository no Android.
package agents

import (
	"net/http"

	"github.com/example/work-control/services/api-go/internal/auth"
	"github.com/example/work-control/services/api-go/internal/httpx"
	"github.com/example/work-control/services/api-go/internal/store"
)

type Handler struct {
	Queries *store.Queries
}

type AgentDTO struct {
	ID              string  `json:"id"`
	Name            string  `json:"name"`
	Role            string  `json:"role"`
	Model           string  `json:"model"`
	MachineName     *string `json:"machine_name"`
	Status          string  `json:"status"`
	ProgressPercent *int32  `json:"progress_percent"`
	CurrentActivity string  `json:"current_activity"`
}

func (h *Handler) ListActive(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	user := auth.FromContext(ctx)

	rows, err := h.Queries.ListActiveAgentsForWorkspace(ctx, user.WorkspaceID)
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to list agents")
		return
	}

	dtos := make([]AgentDTO, 0, len(rows))
	for _, a := range rows {
		var machineName *string
		if a.MachineName.Valid {
			machineName = &a.MachineName.String
		}
		var progress *int32
		if a.ProgressPercent.Valid {
			progress = &a.ProgressPercent.Int32
		}
		dtos = append(dtos, AgentDTO{
			ID:              a.ID.String(),
			Name:            a.Name,
			Role:            string(a.Role),
			Model:           a.Model,
			MachineName:     machineName,
			Status:          string(a.RuntimeStatus),
			ProgressPercent: progress,
			CurrentActivity: a.CurrentActivity,
		})
	}
	httpx.WriteJSON(w, http.StatusOK, dtos)
}
