-- Upserts idempotentes usados só por cmd/seed. IDs vêm fixos do chamador.

-- name: UpsertUser :one
INSERT INTO users (id, email, display_name) VALUES ($1, $2, $3)
ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email, display_name = EXCLUDED.display_name
RETURNING *;

-- name: UpsertWorkspace :one
INSERT INTO workspaces (id, name, slug) VALUES ($1, $2, $3)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, slug = EXCLUDED.slug
RETURNING *;

-- name: UpsertWorkspaceMember :exec
INSERT INTO workspace_members (workspace_id, user_id, role) VALUES ($1, $2, $3)
ON CONFLICT (workspace_id, user_id) DO NOTHING;

-- name: UpsertExternalIdentity :exec
INSERT INTO external_identities (issuer, subject, user_id) VALUES ($1, $2, $3)
ON CONFLICT (issuer, subject) DO UPDATE SET user_id = EXCLUDED.user_id;

-- name: UpsertProject :one
INSERT INTO projects (id, workspace_id, name, slug, repo_url) VALUES ($1, $2, $3, $4, $5)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, slug = EXCLUDED.slug, repo_url = EXCLUDED.repo_url
RETURNING *;

-- name: UpsertDevice :one
INSERT INTO devices (id, workspace_id, name, role_label, status, cpu_percent, ram_percent, disk_percent, last_heartbeat_at)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name, role_label = EXCLUDED.role_label, status = EXCLUDED.status,
  cpu_percent = EXCLUDED.cpu_percent, ram_percent = EXCLUDED.ram_percent, disk_percent = EXCLUDED.disk_percent,
  last_heartbeat_at = EXCLUDED.last_heartbeat_at
RETURNING *;

-- name: UpsertDeviceCapability :exec
INSERT INTO device_capabilities (device_id, capability, enabled) VALUES ($1, $2, $3)
ON CONFLICT (device_id, capability) DO UPDATE SET enabled = EXCLUDED.enabled;

-- name: UpsertAgent :one
INSERT INTO agents (id, workspace_id, name, role, model, is_active) VALUES ($1, $2, $3, $4, $5, $6)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name, role = EXCLUDED.role, model = EXCLUDED.model, is_active = EXCLUDED.is_active
RETURNING *;

-- name: UpsertTask :one
INSERT INTO tasks (id, project_id, code, title, description, status, progress_percent, created_by_user_id)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
ON CONFLICT (id) DO UPDATE SET
  title = EXCLUDED.title, description = EXCLUDED.description, status = EXCLUDED.status,
  progress_percent = EXCLUDED.progress_percent
RETURNING *;

-- name: UpsertTaskStep :one
INSERT INTO task_steps (id, task_id, sequence_order, label, status, agent_id)
VALUES ($1, $2, $3, $4, $5, $6)
ON CONFLICT (id) DO UPDATE SET label = EXCLUDED.label, status = EXCLUDED.status, agent_id = EXCLUDED.agent_id
RETURNING *;

-- name: UpsertExecution :one
INSERT INTO executions (id, task_step_id, agent_id, device_id, attempt_number, status, progress_percent, current_activity, started_at, finished_at)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
ON CONFLICT (id) DO UPDATE SET
  status = EXCLUDED.status, progress_percent = EXCLUDED.progress_percent,
  current_activity = EXCLUDED.current_activity, finished_at = EXCLUDED.finished_at
RETURNING *;

-- name: UpsertExecutionEvent :one
INSERT INTO execution_events (id, task_id, execution_id, agent_id, event_type, message, highlighted, occurred_at)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
ON CONFLICT (id) DO UPDATE SET message = EXCLUDED.message, highlighted = EXCLUDED.highlighted
RETURNING *;

-- name: UpsertApproval :one
INSERT INTO approvals (id, workspace_id, task_id, execution_id, requested_by_agent_id, title, description, payload)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, description = EXCLUDED.description, payload = EXCLUDED.payload
RETURNING *;
