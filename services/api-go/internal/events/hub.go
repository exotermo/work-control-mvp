// Package events implementa o canal de tempo real: um hub de pub/sub em memória por task_id
// e o handler WebSocket que expõe isso em /v1/tasks/{id}/events/ws.
package events

import (
	"log/slog"
	"sync"

	"github.com/google/uuid"
)

type ExecutionEventPayload struct {
	ID          string `json:"id"`
	EventType   string `json:"event_type"`
	Message     string `json:"message"`
	Highlighted bool   `json:"highlighted"`
	OccurredAt  string `json:"occurred_at"`
}

type TaskStepUpdate struct {
	Label   string `json:"label"`
	Status  string `json:"status"`
	AgentID string `json:"agent_id,omitempty"`
}

type TaskUpdate struct {
	ID              string `json:"id"`
	Status          string `json:"status"`
	ProgressPercent int32  `json:"progress_percent"`
}

// Envelope é a mensagem trocada no WebSocket. Só um dos campos Event/Step/Task é preenchido,
// conforme Type.
type Envelope struct {
	Type   string                 `json:"type"`
	TaskID string                 `json:"task_id"`
	Event  *ExecutionEventPayload `json:"event,omitempty"`
	Step   *TaskStepUpdate        `json:"step,omitempty"`
	Task   *TaskUpdate            `json:"task,omitempty"`
}

type subscriber chan Envelope

// Hub é pub/sub em memória, um único processo. Redis Pub/Sub vira necessário quando existir
// mais de um processo api-go ou o orquestrador Java publicando de fora — não é o caso ainda.
type Hub struct {
	mu   sync.RWMutex
	subs map[uuid.UUID]map[subscriber]struct{}
}

func NewHub() *Hub {
	return &Hub{subs: make(map[uuid.UUID]map[subscriber]struct{})}
}

func (h *Hub) Subscribe(taskID uuid.UUID) subscriber {
	ch := make(subscriber, 8)
	h.mu.Lock()
	defer h.mu.Unlock()
	if h.subs[taskID] == nil {
		h.subs[taskID] = make(map[subscriber]struct{})
	}
	h.subs[taskID][ch] = struct{}{}
	return ch
}

func (h *Hub) Unsubscribe(taskID uuid.UUID, ch subscriber) {
	h.mu.Lock()
	defer h.mu.Unlock()
	delete(h.subs[taskID], ch)
	if len(h.subs[taskID]) == 0 {
		delete(h.subs, taskID)
	}
	close(ch)
}

func (h *Hub) Publish(taskID uuid.UUID, env Envelope) {
	env.TaskID = taskID.String()
	h.mu.RLock()
	defer h.mu.RUnlock()
	for ch := range h.subs[taskID] {
		select {
		case ch <- env:
		default:
			slog.Warn("dropping event: slow websocket subscriber", "task_id", taskID)
		}
	}
}
