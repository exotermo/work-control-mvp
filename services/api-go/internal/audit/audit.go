// Package audit registra o evento de auditoria exigido pelo README para toda ação relevante.
package audit

import (
	"context"
	"log/slog"

	"github.com/google/uuid"

	"github.com/example/work-control/services/api-go/internal/store"
)

func Log(ctx context.Context, q *store.Queries, workspaceID, userID uuid.UUID, action, result string) {
	err := q.InsertAuditLog(ctx, store.InsertAuditLogParams{
		WorkspaceID: workspaceID,
		UserID:      uuid.NullUUID{UUID: userID, Valid: true},
		Action:      action,
		Result:      result,
		Metadata:    []byte("{}"),
	})
	if err != nil {
		slog.ErrorContext(ctx, "failed to write audit log", "action", action, "error", err)
	}
}
