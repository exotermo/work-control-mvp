-- name: ListTasksByProject :many
SELECT
  t.id, t.code, t.title, t.status, t.progress_percent,
  COUNT(DISTINCT e.agent_id) FILTER (WHERE e.finished_at IS NULL) AS active_agent_count
FROM tasks t
LEFT JOIN task_steps ts ON ts.task_id = t.id
LEFT JOIN executions e ON e.task_step_id = ts.id
WHERE t.project_id = $1
GROUP BY t.id
ORDER BY t.created_at DESC;

-- name: CountTasksByProject :one
SELECT count(*) FROM tasks WHERE project_id = $1;

-- name: GetTaskByID :one
SELECT * FROM tasks WHERE id = $1;

-- name: CreateTask :one
INSERT INTO tasks (project_id, code, title, created_by_user_id)
VALUES ($1, $2, $3, $4)
RETURNING *;

-- name: ListTaskStepsByTask :many
SELECT * FROM task_steps WHERE task_id = $1 ORDER BY sequence_order;

-- name: ListAgentsForTask :many
SELECT
  a.id, a.name, a.role, a.model,
  d.name AS machine_name,
  e.status AS runtime_status,
  e.progress_percent,
  e.current_activity
FROM task_steps ts
JOIN LATERAL (
  SELECT * FROM executions ex WHERE ex.task_step_id = ts.id ORDER BY ex.started_at DESC LIMIT 1
) e ON true
JOIN agents a ON a.id = e.agent_id
LEFT JOIN devices d ON d.id = e.device_id
WHERE ts.task_id = $1
ORDER BY ts.sequence_order;

-- name: ListExecutionEventsByTask :many
SELECT * FROM execution_events WHERE task_id = $1 ORDER BY occurred_at DESC;
