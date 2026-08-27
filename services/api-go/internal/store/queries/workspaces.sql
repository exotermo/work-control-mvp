-- name: ListWorkspaces :many
SELECT * FROM workspaces ORDER BY name;

-- name: GetWorkspace :one
SELECT * FROM workspaces WHERE id = $1;

-- name: GetFirstWorkspace :one
SELECT * FROM workspaces ORDER BY created_at LIMIT 1;

-- name: GetFirstProjectForWorkspace :one
SELECT * FROM projects WHERE workspace_id = $1 ORDER BY created_at LIMIT 1;
