package tasks

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"

	"github.com/example/work-control/services/api-go/internal/auth"
	"github.com/example/work-control/services/api-go/internal/devidentity"
	"github.com/example/work-control/services/api-go/internal/events"
	"github.com/example/work-control/services/api-go/internal/store"
)

func TestCreateTaskIntegration(t *testing.T) {
	databaseURL := os.Getenv("TEST_DATABASE_URL")
	if databaseURL == "" {
		t.Skip("set TEST_DATABASE_URL to run the PostgreSQL integration test")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	conn, err := pgx.Connect(ctx, databaseURL)
	if err != nil {
		t.Fatalf("connect to TEST_DATABASE_URL: %v", err)
	}
	t.Cleanup(func() {
		closeCtx, closeCancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer closeCancel()
		if err := conn.Close(closeCtx); err != nil {
			t.Errorf("close PostgreSQL connection: %v", err)
		}
	})

	tx, err := conn.Begin(ctx)
	if err != nil {
		t.Fatalf("begin test transaction: %v", err)
	}
	t.Cleanup(func() {
		rollbackCtx, rollbackCancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer rollbackCancel()
		if err := tx.Rollback(rollbackCtx); err != nil && err != pgx.ErrTxClosed {
			t.Errorf("rollback test transaction: %v", err)
		}
	})

	projectID := uuid.New()
	fixtureSuffix := uuid.NewString()
	setupStatements := []struct {
		query string
		args  []any
	}{
		{
			`INSERT INTO users (id, email, display_name) VALUES ($1, $2, $3) ON CONFLICT (id) DO NOTHING`,
			[]any{devidentity.DevUserID, "integration-" + fixtureSuffix + "@example.invalid", "Integration Dev User"},
		},
		{
			`INSERT INTO workspaces (id, name, slug) VALUES ($1, $2, $3) ON CONFLICT (id) DO NOTHING`,
			[]any{devidentity.DevWorkspaceID, "Integration Workspace", "integration-" + fixtureSuffix},
		},
		{
			`INSERT INTO workspace_members (workspace_id, user_id, role) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING`,
			[]any{devidentity.DevWorkspaceID, devidentity.DevUserID, "owner"},
		},
		{
			`INSERT INTO projects (id, workspace_id, name, slug) VALUES ($1, $2, $3, $4)`,
			[]any{projectID, devidentity.DevWorkspaceID, "Integration Project", "integration-" + fixtureSuffix},
		},
	}
	for _, statement := range setupStatements {
		if _, err := tx.Exec(ctx, statement.query, statement.args...); err != nil {
			t.Fatalf("prepare integration fixture (database must be migrated): %v", err)
		}
	}

	baselineAuditIDs := auditLogIDs(t, ctx, tx, devidentity.DevWorkspaceID)
	queries := store.New(tx)
	handler := &Handler{Queries: queries, Hub: events.NewHub()}
	mux := http.NewServeMux()
	mux.HandleFunc("POST /v1/tasks", handler.Create)
	server := httptest.NewServer(auth.Middleware(devidentity.DevUserID, devidentity.DevWorkspaceID)(mux))
	defer server.Close()

	auditCanary := "must-not-appear-in-audit-" + fixtureSuffix
	description := "Integration task " + auditCanary
	body := fmt.Sprintf(`{"project_id":%q,"description":%q}`, projectID.String(), description)
	request, err := http.NewRequestWithContext(ctx, http.MethodPost, server.URL+"/v1/tasks", bytes.NewBufferString(body))
	if err != nil {
		t.Fatalf("build POST /v1/tasks request: %v", err)
	}
	request.Header.Set("Content-Type", "application/json")

	response, err := server.Client().Do(request)
	if err != nil {
		t.Fatalf("POST /v1/tasks: %v", err)
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusCreated {
		t.Fatalf("POST /v1/tasks status = %d, want %d", response.StatusCode, http.StatusCreated)
	}

	var created TaskDTO
	if err := json.NewDecoder(response.Body).Decode(&created); err != nil {
		t.Fatalf("decode created task: %v", err)
	}
	createdID, err := uuid.Parse(created.ID)
	if err != nil {
		t.Fatalf("created task id = %q: %v", created.ID, err)
	}
	if created.Title != description {
		t.Errorf("created task title = %q, want %q", created.Title, description)
	}
	if created.Status != "QUEUED" {
		t.Errorf("created task status = %q, want QUEUED", created.Status)
	}
	if created.ProgressPercent != 0 {
		t.Errorf("created task progress = %d, want 0", created.ProgressPercent)
	}
	if created.Code == "" {
		t.Error("created task code is empty")
	}

	var storedProjectID, storedCreatedBy uuid.UUID
	var storedTitle, storedStatus string
	if err := tx.QueryRow(ctx, `SELECT project_id, title, status, created_by_user_id FROM tasks WHERE id = $1`, createdID).
		Scan(&storedProjectID, &storedTitle, &storedStatus, &storedCreatedBy); err != nil {
		t.Fatalf("load created task from PostgreSQL: %v", err)
	}
	if storedProjectID != projectID || storedTitle != description || storedStatus != "QUEUED" {
		t.Errorf("stored task = (project=%s, title=%q, status=%q), want (%s, %q, QUEUED)",
			storedProjectID, storedTitle, storedStatus, projectID, description)
	}
	if storedCreatedBy != devidentity.DevUserID {
		t.Errorf("stored created_by_user_id = %s, want dev user %s", storedCreatedBy, devidentity.DevUserID)
	}

	assertNewTaskAuditLog(t, ctx, tx, baselineAuditIDs, auditCanary)
}

func auditLogIDs(t *testing.T, ctx context.Context, tx pgx.Tx, workspaceID uuid.UUID) map[uuid.UUID]struct{} {
	t.Helper()

	rows, err := tx.Query(ctx, `SELECT id FROM audit_logs WHERE workspace_id = $1 AND action = 'task.create'`, workspaceID)
	if err != nil {
		t.Fatalf("list baseline task audit logs: %v", err)
	}
	defer rows.Close()

	ids := make(map[uuid.UUID]struct{})
	for rows.Next() {
		var id uuid.UUID
		if err := rows.Scan(&id); err != nil {
			t.Fatalf("scan baseline task audit log: %v", err)
		}
		ids[id] = struct{}{}
	}
	if err := rows.Err(); err != nil {
		t.Fatalf("iterate baseline task audit logs: %v", err)
	}
	return ids
}

func assertNewTaskAuditLog(t *testing.T, ctx context.Context, tx pgx.Tx, baseline map[uuid.UUID]struct{}, canary string) {
	t.Helper()

	rows, err := tx.Query(ctx, `
		SELECT id, user_id, action, result, metadata
		FROM audit_logs
		WHERE workspace_id = $1 AND action = 'task.create'`, devidentity.DevWorkspaceID)
	if err != nil {
		t.Fatalf("list task audit logs after creation: %v", err)
	}
	defer rows.Close()

	newRows := 0
	for rows.Next() {
		var id uuid.UUID
		var userID uuid.NullUUID
		var action, result string
		var metadata []byte
		if err := rows.Scan(&id, &userID, &action, &result, &metadata); err != nil {
			t.Fatalf("scan task audit log: %v", err)
		}
		if _, existed := baseline[id]; existed {
			continue
		}

		newRows++
		if !userID.Valid || userID.UUID != devidentity.DevUserID {
			t.Errorf("audit user_id = %v, want dev user %s", userID, devidentity.DevUserID)
		}
		if action != "task.create" || result != "success" {
			t.Errorf("audit action/result = %q/%q, want task.create/success", action, result)
		}
		var metadataObject map[string]any
		if err := json.Unmarshal(metadata, &metadataObject); err != nil {
			t.Errorf("audit metadata is not valid JSON: %q: %v", metadata, err)
		} else if len(metadataObject) != 0 {
			t.Errorf("audit metadata = %s, want empty object", metadata)
		}
		if bytes.Contains(metadata, []byte(canary)) {
			t.Errorf("audit metadata leaked request description canary: %s", metadata)
		}
	}
	if err := rows.Err(); err != nil {
		t.Fatalf("iterate task audit logs: %v", err)
	}
	if newRows != 1 {
		t.Fatalf("new task.create audit rows = %d, want 1", newRows)
	}
}
