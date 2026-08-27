// Package workspace expõe a listagem de workspaces e o dashboard combinado (paridade com o
// README; não é o que o app Android real consome — cada repositório Android fala com o recurso
// próprio em tasks/agents/devices/approvals, ver Diário de Bordo / plano de dados).
package workspace

import (
	"net/http"

	"github.com/google/uuid"

	"github.com/example/work-control/services/api-go/internal/httpx"
	"github.com/example/work-control/services/api-go/internal/store"
)

type Handler struct {
	Queries *store.Queries
}

type workspaceDTO struct {
	ID   string `json:"id"`
	Name string `json:"name"`
	Slug string `json:"slug"`
}

func (h *Handler) List(w http.ResponseWriter, r *http.Request) {
	rows, err := h.Queries.ListWorkspaces(r.Context())
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to list workspaces")
		return
	}
	dtos := make([]workspaceDTO, 0, len(rows))
	for _, ws := range rows {
		dtos = append(dtos, workspaceDTO{ID: ws.ID.String(), Name: ws.Name, Slug: ws.Slug})
	}
	httpx.WriteJSON(w, http.StatusOK, dtos)
}

type dashboardDTO struct {
	Workspace workspaceDTO `json:"workspace"`
}

func (h *Handler) Dashboard(w http.ResponseWriter, r *http.Request) {
	id, err := uuid.Parse(r.PathValue("id"))
	if err != nil {
		httpx.WriteError(w, http.StatusBadRequest, "invalid workspace id")
		return
	}
	ws, err := h.Queries.GetWorkspace(r.Context(), id)
	if err != nil {
		httpx.WriteError(w, http.StatusNotFound, "workspace not found")
		return
	}
	httpx.WriteJSON(w, http.StatusOK, dashboardDTO{
		Workspace: workspaceDTO{ID: ws.ID.String(), Name: ws.Name, Slug: ws.Slug},
	})
}
