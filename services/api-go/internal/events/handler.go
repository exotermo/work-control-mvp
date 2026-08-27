package events

import (
	"context"
	"log/slog"
	"net/http"
	"time"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
	"github.com/google/uuid"
)

type WSHandler struct {
	Hub *Hub
}

// Serve atende GET /v1/tasks/{id}/events/ws — o cliente só recebe (CloseRead descarta qualquer
// mensagem que ele mandar), o mesmo transporte de baixo nível que o futuro canal de
// Terminal/SSH vai reaproveitar em modo bidirecional.
func (h *WSHandler) Serve(w http.ResponseWriter, r *http.Request) {
	taskID, err := uuid.Parse(r.PathValue("id"))
	if err != nil {
		http.Error(w, "invalid task id", http.StatusBadRequest)
		return
	}

	conn, err := websocket.Accept(w, r, nil)
	if err != nil {
		slog.Error("websocket accept failed", "error", err)
		return
	}
	defer conn.CloseNow()

	ch := h.Hub.Subscribe(taskID)
	defer h.Hub.Unsubscribe(taskID, ch)

	ctx := conn.CloseRead(r.Context())

	for {
		select {
		case <-ctx.Done():
			return
		case env, ok := <-ch:
			if !ok {
				return
			}
			writeCtx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
			err := wsjson.Write(writeCtx, conn, env)
			cancel()
			if err != nil {
				return
			}
		}
	}
}
