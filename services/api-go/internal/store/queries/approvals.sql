-- name: GetPendingApprovalForWorkspace :one
SELECT a.*, ag.name AS requested_by_name
FROM approvals a
LEFT JOIN agents ag ON ag.id = a.requested_by_agent_id
WHERE a.workspace_id = $1 AND a.decision IS NULL
ORDER BY a.created_at DESC
LIMIT 1;

-- name: GetApproval :one
SELECT a.*, ag.name AS requested_by_name
FROM approvals a
LEFT JOIN agents ag ON ag.id = a.requested_by_agent_id
WHERE a.id = $1;

-- name: DecideApproval :one
UPDATE approvals
SET decision = $2, decided_by_user_id = $3, decided_at = now()
WHERE id = $1
RETURNING *;
