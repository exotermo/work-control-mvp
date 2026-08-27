-- name: InsertAuditLog :exec
INSERT INTO audit_logs (workspace_id, user_id, agent_id, device_id, approval_id, action, result, metadata)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8);
