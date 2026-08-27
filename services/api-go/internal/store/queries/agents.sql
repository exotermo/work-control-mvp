-- name: ListActiveAgentsForWorkspace :many
SELECT
  a.id, a.name, a.role, a.model,
  d.name AS machine_name,
  e.status AS runtime_status,
  e.progress_percent,
  e.current_activity
FROM agents a
JOIN LATERAL (
  SELECT * FROM executions ex
  WHERE ex.agent_id = a.id AND ex.finished_at IS NULL
  ORDER BY ex.started_at DESC
  LIMIT 1
) e ON true
LEFT JOIN devices d ON d.id = e.device_id
WHERE a.workspace_id = $1 AND a.is_active
ORDER BY a.name;
