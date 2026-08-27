package events

import (
	"context"
	"log/slog"
	"time"

	"github.com/google/uuid"

	"github.com/example/work-control/services/api-go/internal/devseed"
	"github.com/example/work-control/services/api-go/internal/store"
)

// RunDemoSimulator avança periodicamente a tarefa semeada #184 para dar vida ao WebSocket
// enquanto não existe orquestrador real publicando eventos. Só roda com DEMO_SIMULATOR=true —
// é ferramenta de verificação, não orquestração real.
func RunDemoSimulator(ctx context.Context, q *store.Queries, hub *Hub) {
	ticker := time.NewTicker(8 * time.Second)
	defer ticker.Stop()

	messages := []string{
		"Agent Dev: revisando testes de integração",
		"Agent Dev: aplicando ajuste em auth.service.ts",
		"Agent Dev: rodando suíte de testes",
		"Agent Dev: aguardando resultado do CI",
	}
	i := 0

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			task, err := q.GetTaskByID(ctx, devseed.Task184ID)
			if err != nil {
				slog.Error("demo simulator: failed to load task", "error", err)
				continue
			}
			if task.ProgressPercent >= 99 {
				continue
			}
			nextProgress := task.ProgressPercent + 3
			if nextProgress > 99 {
				nextProgress = 99
			}

			updated, err := q.UpdateTaskProgress(ctx, store.UpdateTaskProgressParams{
				ID:              devseed.Task184ID,
				ProgressPercent: nextProgress,
				Status:          task.Status,
			})
			if err != nil {
				slog.Error("demo simulator: failed to update task", "error", err)
				continue
			}

			msg := messages[i%len(messages)]
			i++
			event, err := q.InsertExecutionEvent(ctx, store.InsertExecutionEventParams{
				TaskID:      devseed.Task184ID,
				AgentID:     uuid.NullUUID{UUID: devseed.AgentDevID, Valid: true},
				EventType:   "log",
				Message:     msg,
				Highlighted: false,
			})
			if err != nil {
				slog.Error("demo simulator: failed to insert event", "error", err)
				continue
			}

			hub.Publish(devseed.Task184ID, Envelope{
				Type: "task_updated",
				Task: &TaskUpdate{ID: updated.ID.String(), Status: string(updated.Status), ProgressPercent: updated.ProgressPercent},
			})
			hub.Publish(devseed.Task184ID, Envelope{
				Type: "execution_event",
				Event: &ExecutionEventPayload{
					ID:          event.ID.String(),
					EventType:   event.EventType,
					Message:     event.Message,
					Highlighted: event.Highlighted,
					OccurredAt:  event.OccurredAt.Time.Format(time.RFC3339),
				},
			})
		}
	}
}
