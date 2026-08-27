// Package tasks expõe a lista de tarefas e o detalhe agregado (fluxo de execução + agentes +
// log de atividade) que alimentam TaskRepository no Android.
package tasks

import (
	"errors"
	"fmt"
	"net/http"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"

	"github.com/example/work-control/services/api-go/internal/audit"
	"github.com/example/work-control/services/api-go/internal/auth"
	"github.com/example/work-control/services/api-go/internal/devidentity"
	"github.com/example/work-control/services/api-go/internal/events"
	"github.com/example/work-control/services/api-go/internal/httpx"
	"github.com/example/work-control/services/api-go/internal/store"
)

type Handler struct {
	Queries *store.Queries
	Hub     *events.Hub
}

type TaskDTO struct {
	ID               string `json:"id"`
	Code             string `json:"code"`
	Title            string `json:"title"`
	Status           string `json:"status"`
	ActiveAgentCount int64  `json:"active_agent_count"`
	ProgressPercent  int32  `json:"progress_percent"`
}

type ExecutionNodeDTO struct {
	Label   string  `json:"label"`
	Status  string  `json:"status"`
	AgentID *string `json:"agent_id"`
}

type AgentSummaryDTO struct {
	ID              string  `json:"id"`
	Name            string  `json:"name"`
	Role            string  `json:"role"`
	Model           string  `json:"model"`
	MachineName     *string `json:"machine_name"`
	Status          string  `json:"status"`
	ProgressPercent *int32  `json:"progress_percent"`
	CurrentActivity string  `json:"current_activity"`
}

type ActivityLogEntryDTO struct {
	OccurredAt  string `json:"occurred_at"`
	Message     string `json:"message"`
	Highlighted bool   `json:"highlighted"`
}

type TaskDetailDTO struct {
	Task          TaskDTO               `json:"task"`
	StartedAt     string                `json:"started_at"`
	ExecutionFlow []ExecutionNodeDTO    `json:"execution_flow"`
	Agents        []AgentSummaryDTO     `json:"agents"`
	ActivityLog   []ActivityLogEntryDTO `json:"activity_log"`
}

func resolveProjectID(r *http.Request) (uuid.UUID, error) {
	if v := r.URL.Query().Get("project_id"); v != "" {
		return uuid.Parse(v)
	}
	return devidentity.DevProjectID, nil
}

func (h *Handler) List(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	projectID, err := resolveProjectID(r)
	if err != nil {
		httpx.WriteError(w, http.StatusBadRequest, "invalid project_id")
		return
	}

	rows, err := h.Queries.ListTasksByProject(ctx, projectID)
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to list tasks")
		return
	}

	dtos := make([]TaskDTO, 0, len(rows))
	for _, row := range rows {
		dtos = append(dtos, TaskDTO{
			ID:               row.ID.String(),
			Code:             row.Code,
			Title:            row.Title,
			Status:           string(row.Status),
			ActiveAgentCount: row.ActiveAgentCount,
			ProgressPercent:  row.ProgressPercent,
		})
	}
	httpx.WriteJSON(w, http.StatusOK, dtos)
}

func (h *Handler) Get(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	id, err := uuid.Parse(r.PathValue("id"))
	if err != nil {
		httpx.WriteError(w, http.StatusBadRequest, "invalid task id")
		return
	}

	task, err := h.Queries.GetTaskByID(ctx, id)
	if errors.Is(err, pgx.ErrNoRows) {
		httpx.WriteError(w, http.StatusNotFound, "task not found")
		return
	} else if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to load task")
		return
	}

	steps, err := h.Queries.ListTaskStepsByTask(ctx, id)
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to load execution flow")
		return
	}
	agentRows, err := h.Queries.ListAgentsForTask(ctx, id)
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to load agents")
		return
	}
	eventRows, err := h.Queries.ListExecutionEventsByTask(ctx, id)
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to load activity log")
		return
	}

	httpx.WriteJSON(w, http.StatusOK, TaskDetailDTO{
		Task: TaskDTO{
			ID:               task.ID.String(),
			Code:             task.Code,
			Title:            task.Title,
			Status:           string(task.Status),
			ActiveAgentCount: int64(len(agentRows)),
			ProgressPercent:  task.ProgressPercent,
		},
		StartedAt:     task.CreatedAt.Time.Format(time.RFC3339),
		ExecutionFlow: toExecutionNodeDTOs(steps),
		Agents:        toAgentSummaryDTOs(agentRows),
		ActivityLog:   toActivityLogDTOs(eventRows),
	})
}

type createTaskRequest struct {
	ProjectID   string `json:"project_id"`
	Description string `json:"description"`
}

func (h *Handler) Create(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	user := auth.FromContext(ctx)

	var req createTaskRequest
	if err := httpx.DecodeJSON(r, &req); err != nil {
		httpx.WriteError(w, http.StatusBadRequest, "invalid request body")
		return
	}
	if req.Description == "" {
		httpx.WriteError(w, http.StatusBadRequest, "description is required")
		return
	}

	projectID := devidentity.DevProjectID
	if req.ProjectID != "" {
		parsed, err := uuid.Parse(req.ProjectID)
		if err != nil {
			httpx.WriteError(w, http.StatusBadRequest, "invalid project_id")
			return
		}
		projectID = parsed
	}

	count, err := h.Queries.CountTasksByProject(ctx, projectID)
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to create task")
		return
	}

	task, err := h.Queries.CreateTask(ctx, store.CreateTaskParams{
		ProjectID:       projectID,
		Code:            fmt.Sprintf("#novo-%d", count+1),
		Title:           req.Description,
		CreatedByUserID: uuid.NullUUID{UUID: user.UserID, Valid: true},
	})
	if err != nil {
		httpx.WriteError(w, http.StatusInternalServerError, "failed to create task")
		return
	}

	audit.Log(ctx, h.Queries, user.WorkspaceID, user.UserID, "task.create", "success")
	h.Hub.Publish(task.ID, events.Envelope{
		Type:   "task_updated",
		TaskID: task.ID.String(),
		Task: &events.TaskUpdate{
			ID:              task.ID.String(),
			Status:          string(task.Status),
			ProgressPercent: task.ProgressPercent,
		},
	})

	httpx.WriteJSON(w, http.StatusCreated, TaskDTO{
		ID:              task.ID.String(),
		Code:            task.Code,
		Title:           task.Title,
		Status:          string(task.Status),
		ProgressPercent: task.ProgressPercent,
	})
}

func toExecutionNodeDTOs(steps []store.TaskStep) []ExecutionNodeDTO {
	nodes := make([]ExecutionNodeDTO, 0, len(steps))
	for _, step := range steps {
		var agentID *string
		if step.AgentID.Valid {
			s := step.AgentID.UUID.String()
			agentID = &s
		}
		nodes = append(nodes, ExecutionNodeDTO{Label: step.Label, Status: string(step.Status), AgentID: agentID})
	}
	return nodes
}

func toAgentSummaryDTOs(rows []store.ListAgentsForTaskRow) []AgentSummaryDTO {
	agents := make([]AgentSummaryDTO, 0, len(rows))
	for _, a := range rows {
		var machineName *string
		if a.MachineName.Valid {
			machineName = &a.MachineName.String
		}
		var progress *int32
		if a.ProgressPercent.Valid {
			progress = &a.ProgressPercent.Int32
		}
		agents = append(agents, AgentSummaryDTO{
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
	return agents
}

func toActivityLogDTOs(rows []store.ExecutionEvent) []ActivityLogEntryDTO {
	log := make([]ActivityLogEntryDTO, 0, len(rows))
	for _, e := range rows {
		log = append(log, ActivityLogEntryDTO{
			OccurredAt:  e.OccurredAt.Time.Format(time.RFC3339),
			Message:     e.Message,
			Highlighted: e.Highlighted,
		})
	}
	return log
}
