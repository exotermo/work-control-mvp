// Package devices expõe as máquinas do workspace — alimenta MachineRepository no Android.
package devices

import (
	"errors"
	"net/http"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"

	"github.com/example/work-control/services/api-go/internal/auth"
	"github.com/example/work-control/services/api-go/internal/httpx"
	"github.com/example/work-control/services/api-go/internal/store"
)

type Handler struct {
	Queries *store.Queries
}

type DeviceDTO struct {
	ID               string `json:"id"`
	Name             string `json:"name"`
	RoleLabel        string `json:"role_label"`
	Status           string `json:"status"`
	CpuPercent       int16  `json:"cpu_percent"`
	RamPercent       int16  `json:"ram_percent"`
	DiskPercent      int16  `json:"disk_percent"`
	ActiveAgentCount int64  `json:"active_agent_count"`
}

func (h *Handler) List(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	user := auth.FromContext(ctx)

	rows, err := h.Queries.ListDevicesForWorkspace(ctx, user.WorkspaceID)
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to list devices")
		return
	}

	dtos := make([]DeviceDTO, 0, len(rows))
	for _, d := range rows {
		dtos = append(dtos, DeviceDTO{
			ID:               d.ID.String(),
			Name:             d.Name,
			RoleLabel:        d.RoleLabel,
			Status:           string(d.Status),
			CpuPercent:       d.CpuPercent,
			RamPercent:       d.RamPercent,
			DiskPercent:      d.DiskPercent,
			ActiveAgentCount: d.ActiveAgentCount,
		})
	}
	httpx.WriteJSON(w, http.StatusOK, dtos)
}

func (h *Handler) Get(w http.ResponseWriter, r *http.Request) {
	id, err := uuid.Parse(r.PathValue("id"))
	if err != nil {
		httpx.WriteError(w, http.StatusBadRequest, "invalid device id")
		return
	}
	d, err := h.Queries.GetDevice(r.Context(), id)
	if errors.Is(err, pgx.ErrNoRows) {
		httpx.WriteError(w, http.StatusNotFound, "device not found")
		return
	} else if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to load device")
		return
	}
	httpx.WriteJSON(w, http.StatusOK, DeviceDTO{
		ID:          d.ID.String(),
		Name:        d.Name,
		RoleLabel:   d.RoleLabel,
		Status:      string(d.Status),
		CpuPercent:  d.CpuPercent,
		RamPercent:  d.RamPercent,
		DiskPercent: d.DiskPercent,
	})
}
