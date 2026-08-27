package auth

import (
	"context"
	"net/http"
	"time"

	"github.com/google/uuid"

	"github.com/example/work-control/services/api-go/internal/httpx"
)

type AccountReader interface {
	Account(context.Context, uuid.UUID) (Account, error)
}

type MeHandler struct {
	Accounts AccountReader
}

type meResponse struct {
	User       userDTO         `json:"user"`
	Workspaces []membershipDTO `json:"workspaces"`
}

type userDTO struct {
	ID          string    `json:"id"`
	Email       string    `json:"email"`
	DisplayName string    `json:"display_name"`
	CreatedAt   time.Time `json:"created_at"`
}

type membershipDTO struct {
	ID       string    `json:"id"`
	Name     string    `json:"name"`
	Slug     string    `json:"slug"`
	Role     string    `json:"role"`
	JoinedAt time.Time `json:"joined_at"`
}

func (h *MeHandler) Get(w http.ResponseWriter, r *http.Request) {
	current, ok := Lookup(r.Context())
	if !ok || current.UserID == uuid.Nil {
		httpx.WriteError(w, http.StatusUnauthorized, "authentication required")
		return
	}

	account, err := h.Accounts.Account(r.Context(), current.UserID)
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to load current user")
		return
	}

	workspaces := make([]membershipDTO, 0, len(account.Workspaces))
	for _, membership := range account.Workspaces {
		workspaces = append(workspaces, membershipDTO{
			ID:       membership.WorkspaceID.String(),
			Name:     membership.WorkspaceName,
			Slug:     membership.WorkspaceSlug,
			Role:     membership.Role,
			JoinedAt: membership.JoinedAt,
		})
	}
	httpx.WriteJSON(w, http.StatusOK, meResponse{
		User: userDTO{
			ID:          account.ID.String(),
			Email:       account.Email,
			DisplayName: account.DisplayName,
			CreatedAt:   account.CreatedAt,
		},
		Workspaces: workspaces,
	})
}
