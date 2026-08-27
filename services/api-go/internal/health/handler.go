// Package health expõe os sinais operacionais mínimos da API.
package health

import (
	"context"
	"net/http"
	"time"

	"github.com/example/work-control/services/api-go/internal/httpx"
)

const defaultReadyTimeout = 2 * time.Second

type Pinger interface {
	Ping(context.Context) error
}

type Handler struct {
	Database Pinger
	Timeout  time.Duration
}

func (h *Handler) Live(w http.ResponseWriter, _ *http.Request) {
	httpx.WriteJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

func (h *Handler) Ready(w http.ResponseWriter, r *http.Request) {
	timeout := h.Timeout
	if timeout <= 0 {
		timeout = defaultReadyTimeout
	}

	ctx, cancel := context.WithTimeout(r.Context(), timeout)
	defer cancel()

	if h.Database == nil || h.Database.Ping(ctx) != nil {
		httpx.WriteError(w, http.StatusServiceUnavailable, "service unavailable")
		return
	}

	httpx.WriteJSON(w, http.StatusOK, map[string]string{"status": "ready"})
}
