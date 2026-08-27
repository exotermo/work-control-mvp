-- name: GetUserIDByExternalIdentity :one
SELECT user_id
FROM external_identities
WHERE issuer = $1 AND subject = $2;

-- name: GetUserProfile :one
SELECT id, email, display_name, created_at
FROM users
WHERE id = $1;

-- name: ListWorkspaceMembershipsForUser :many
SELECT
  w.id AS workspace_id,
  w.name AS workspace_name,
  w.slug AS workspace_slug,
  wm.role,
  wm.joined_at
FROM workspace_members wm
JOIN workspaces w ON w.id = wm.workspace_id
WHERE wm.user_id = $1
ORDER BY w.name, w.id;
