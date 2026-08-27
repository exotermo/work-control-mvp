-- Consultas usadas só pelo simulador de eventos de demonstração (DEMO_SIMULATOR=true).

-- name: UpdateTaskProgress :one
UPDATE tasks SET progress_percent = $2, status = $3 WHERE id = $1
RETURNING *;

-- name: InsertExecutionEvent :one
INSERT INTO execution_events (task_id, execution_id, agent_id, event_type, message, highlighted)
VALUES ($1, $2, $3, $4, $5, $6)
RETURNING *;
