package events

import (
	"context"
	"net/http"
	"net/http/httptest"
	"reflect"
	"strings"
	"testing"
	"time"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
	"github.com/google/uuid"
)

func TestWSHandlerPublishesEnvelopeAfterSubscription(t *testing.T) {
	t.Parallel()

	taskID := uuid.MustParse("00000000-0000-0000-0000-000000000184")
	hub := NewHub()
	handler := &WSHandler{Hub: hub}

	mux := http.NewServeMux()
	mux.HandleFunc("GET /v1/tasks/{id}/events/ws", handler.Serve)
	server := httptest.NewServer(mux)
	defer server.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	websocketURL := "ws" + strings.TrimPrefix(server.URL, "http") + "/v1/tasks/" + taskID.String() + "/events/ws"
	conn, response, err := websocket.Dial(ctx, websocketURL, nil)
	if response != nil && response.Body != nil {
		defer response.Body.Close()
	}
	if err != nil {
		t.Fatalf("dial websocket: %v", err)
	}
	defer conn.CloseNow()

	waitForSubscriberCount(t, hub, taskID, 1)

	want := Envelope{
		Type: "execution_event",
		Event: &ExecutionEventPayload{
			ID:          "event-1",
			EventType:   "agent.progress",
			Message:     "deterministic websocket event",
			Highlighted: true,
			OccurredAt:  "2026-08-24T12:00:00Z",
		},
	}
	hub.Publish(taskID, want)
	want.TaskID = taskID.String()

	var got Envelope
	if err := wsjson.Read(ctx, conn, &got); err != nil {
		t.Fatalf("read websocket envelope: %v", err)
	}
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("envelope = %#v, want %#v", got, want)
	}

	if err := conn.Close(websocket.StatusNormalClosure, "test completed"); err != nil {
		t.Fatalf("close websocket cleanly: %v", err)
	}
	waitForSubscriberCount(t, hub, taskID, 0)
}

func waitForSubscriberCount(t *testing.T, hub *Hub, taskID uuid.UUID, want int) {
	t.Helper()

	deadline := time.NewTimer(2 * time.Second)
	ticker := time.NewTicker(time.Millisecond)
	defer deadline.Stop()
	defer ticker.Stop()

	for {
		hub.mu.RLock()
		got := len(hub.subs[taskID])
		hub.mu.RUnlock()
		if got == want {
			return
		}

		select {
		case <-deadline.C:
			t.Fatalf("subscriber count = %d, want %d", got, want)
		case <-ticker.C:
		}
	}
}
