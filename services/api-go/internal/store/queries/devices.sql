-- name: ListDevicesForWorkspace :many
SELECT
  d.id, d.name, d.role_label, d.status, d.cpu_percent, d.ram_percent, d.disk_percent,
  COUNT(DISTINCT e.agent_id) FILTER (WHERE e.finished_at IS NULL) AS active_agent_count
FROM devices d
LEFT JOIN executions e ON e.device_id = d.id
WHERE d.workspace_id = $1
GROUP BY d.id
ORDER BY d.name;

-- name: GetDevice :one
SELECT id, name, role_label, status, cpu_percent, ram_percent, disk_percent
FROM devices
WHERE id = $1;
