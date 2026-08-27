// cmd/seed popula dados de desenvolvimento idempotentes — a mesma história "bug #184" usada em
// apps/android/.../data/fake/FakeData.kt, para o app mostrar o mesmo conteúdo já vindo do
// Postgres. Só roda com SEED_DEV_DATA=true; nunca implícito em `migrate up`.
package main

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/example/work-control/services/api-go/internal/config"
	"github.com/example/work-control/services/api-go/internal/devidentity"
	"github.com/example/work-control/services/api-go/internal/devseed"
	"github.com/example/work-control/services/api-go/internal/store"
)

// namespace fixo só para gerar IDs determinísticos das linhas "secundárias" (steps, execuções,
// eventos, tarefas além da #184) — reprodutíveis a cada execução, sem precisar de uma constante
// escrita à mão para cada uma.
var seedNamespace = uuid.MustParse("11111111-2222-3333-4444-555555555555")

func id(key string) uuid.UUID {
	return uuid.NewSHA1(seedNamespace, []byte(key))
}

func ts(minutesAgo int) pgtype.Timestamptz {
	return pgtype.Timestamptz{Time: time.Now().Add(-time.Duration(minutesAgo) * time.Minute), Valid: true}
}

func main() {
	cfg, err := config.Load()
	if err != nil {
		slog.Error("invalid configuration", "error", err)
		os.Exit(1)
	}
	if !cfg.SeedDevData {
		slog.Info("SEED_DEV_DATA is not true, skipping seed")
		return
	}

	ctx := context.Background()
	pool, err := pgxpool.New(ctx, cfg.PostgresDSN)
	if err != nil {
		slog.Error("failed to connect to postgres", "error", err)
		os.Exit(1)
	}
	defer pool.Close()

	q := store.New(pool)

	if _, err := q.UpsertUser(ctx, store.UpsertUserParams{
		ID:          devidentity.DevUserID,
		Email:       "dev@workcontrol.local",
		DisplayName: "Dev Owner",
	}); err != nil {
		fail("seed user", err)
	}

	if _, err := q.UpsertWorkspace(ctx, store.UpsertWorkspaceParams{
		ID:   devidentity.DevWorkspaceID,
		Name: "Escritório",
		Slug: "escritorio",
	}); err != nil {
		fail("seed workspace", err)
	}

	if err := q.UpsertWorkspaceMember(ctx, store.UpsertWorkspaceMemberParams{
		WorkspaceID: devidentity.DevWorkspaceID,
		UserID:      devidentity.DevUserID,
		Role:        "owner",
	}); err != nil {
		fail("seed workspace member", err)
	}

	if err := q.UpsertExternalIdentity(ctx, store.UpsertExternalIdentityParams{
		Issuer:  devidentity.KeycloakLocalIssuer,
		Subject: devidentity.KeycloakLocalSubject.String(),
		UserID:  devidentity.DevUserID,
	}); err != nil {
		fail("seed external identity", err)
	}

	if _, err := q.UpsertProject(ctx, store.UpsertProjectParams{
		ID:          devidentity.DevProjectID,
		WorkspaceID: devidentity.DevWorkspaceID,
		Name:        "Projeto Atlas",
		Slug:        "atlas",
		RepoUrl:     pgtype.Text{String: "", Valid: false},
	}); err != nil {
		fail("seed project", err)
	}

	seedDevices(ctx, q)
	seedAgents(ctx, q)
	seedTask184(ctx, q)
	seedOtherTasks(ctx, q)
	seedApproval(ctx, q)

	slog.Info("seed completed")
}

func seedDevices(ctx context.Context, q *store.Queries) {
	devicesData := []struct {
		id     uuid.UUID
		name   string
		role   string
		status store.DeviceStatus
		cpu    int16
		ram    int16
		disk   int16
		caps   []string
	}{
		{devseed.DeviceWorkstation01ID, "Workstation-01", "Dev Machine · Ubuntu 24.04", store.DeviceStatusONLINE, 45, 62, 38, []string{"terminal", "filesystem", "git", "docker"}},
		{devseed.DeviceServerAtlasID, "Server-Atlas", "App Server · Docker · Node.js", store.DeviceStatusONLINE, 23, 41, 67, []string{"terminal", "filesystem", "docker"}},
		{devseed.DeviceServerCIID, "Server-CI", "CI/CD · GitHub Actions Runner", store.DeviceStatusONLINE, 71, 55, 22, []string{"terminal", "docker"}},
		{devseed.DeviceWorkstation02ID, "Workstation-02", "Dev Machine · macOS 15", store.DeviceStatusOFFLINE, 0, 0, 0, []string{"terminal", "filesystem", "git"}},
	}

	for _, d := range devicesData {
		if _, err := q.UpsertDevice(ctx, store.UpsertDeviceParams{
			ID:              d.id,
			WorkspaceID:     devidentity.DevWorkspaceID,
			Name:            d.name,
			RoleLabel:       d.role,
			Status:          d.status,
			CpuPercent:      d.cpu,
			RamPercent:      d.ram,
			DiskPercent:     d.disk,
			LastHeartbeatAt: ts(0),
		}); err != nil {
			fail("seed device "+d.name, err)
		}
		for _, cap := range d.caps {
			if err := q.UpsertDeviceCapability(ctx, store.UpsertDeviceCapabilityParams{
				DeviceID: d.id, Capability: cap, Enabled: true,
			}); err != nil {
				fail("seed device capability", err)
			}
		}
	}
}

func seedAgents(ctx context.Context, q *store.Queries) {
	agentsData := []struct {
		id    uuid.UUID
		name  string
		role  store.AgentRole
		model string
	}{
		{devseed.AgentDevID, "Agent Dev", store.AgentRoleDEV, "Claude Sonnet 4.5"},
		{devseed.AgentQAID, "Agent QA", store.AgentRoleQA, "GPT-4o"},
		{devseed.AgentDevOpsID, "Agent DevOps", store.AgentRoleDEVOPS, "Claude Haiku 4.5"},
	}
	for _, a := range agentsData {
		if _, err := q.UpsertAgent(ctx, store.UpsertAgentParams{
			ID: a.id, WorkspaceID: devidentity.DevWorkspaceID, Name: a.name, Role: a.role, Model: a.model, IsActive: true,
		}); err != nil {
			fail("seed agent "+a.name, err)
		}
	}
}

// seedTask184 reproduz FakeData.kt: TaskItem #184 + FakeData.taskDetail(...) com fidelidade
// completa — é o cenário usado para verificar byte-a-byte que o Android real bate com os fakes.
func seedTask184(ctx context.Context, q *store.Queries) {
	task, err := q.UpsertTask(ctx, store.UpsertTaskParams{
		ID:              devseed.Task184ID,
		ProjectID:       devidentity.DevProjectID,
		Code:            "#184",
		Title:           "Investigar e corrigir bug #184",
		Description:     "Investigar e corrigir bug #184 no módulo de autenticação que causa crash no login",
		Status:          store.TaskStatusINPROGRESS,
		ProgressPercent: 68,
		CreatedByUserID: uuid.NullUUID{UUID: devidentity.DevUserID, Valid: true},
	})
	if err != nil {
		fail("seed task 184", err)
	}

	steps := []struct {
		key     string
		order   int32
		label   string
		status  store.ExecutionNodeStatus
		agentID uuid.NullUUID
	}{
		{"184-step-user", 1, "Usuário", store.ExecutionNodeStatusDONE, uuid.NullUUID{}},
		{"184-step-orch", 2, "Orquestrador", store.ExecutionNodeStatusDONE, uuid.NullUUID{}},
		{"184-step-dev", 3, "Agent Dev", store.ExecutionNodeStatusACTIVE, uuid.NullUUID{UUID: devseed.AgentDevID, Valid: true}},
		{"184-step-qa", 4, "Agent QA", store.ExecutionNodeStatusPENDING, uuid.NullUUID{UUID: devseed.AgentQAID, Valid: true}},
		{"184-step-devops", 5, "DevOps", store.ExecutionNodeStatusPENDING, uuid.NullUUID{UUID: devseed.AgentDevOpsID, Valid: true}},
	}
	stepIDs := map[string]uuid.UUID{}
	for _, s := range steps {
		stepID := id(s.key)
		stepIDs[s.key] = stepID
		if _, err := q.UpsertTaskStep(ctx, store.UpsertTaskStepParams{
			ID: stepID, TaskID: task.ID, SequenceOrder: s.order, Label: s.label, Status: s.status, AgentID: s.agentID,
		}); err != nil {
			fail("seed task_step "+s.key, err)
		}
	}

	executions := []struct {
		key      string
		stepKey  string
		agentID  uuid.UUID
		deviceID uuid.NullUUID
		status   store.AgentRuntimeStatus
		progress pgtype.Int4
		activity string
	}{
		{"184-exec-dev", "184-step-dev", devseed.AgentDevID, uuid.NullUUID{UUID: devseed.DeviceWorkstation01ID, Valid: true}, store.AgentRuntimeStatusRUNNING, pgtype.Int4{Int32: 68, Valid: true}, "Corrigindo bug #184 · auth.service.ts"},
		{"184-exec-qa", "184-step-qa", devseed.AgentQAID, uuid.NullUUID{UUID: devseed.DeviceServerAtlasID, Valid: true}, store.AgentRuntimeStatusRUNNING, pgtype.Int4{Int32: 66, Valid: true}, "Executando testes · 21/32 concluídos"},
		{"184-exec-devops", "184-step-devops", devseed.AgentDevOpsID, uuid.NullUUID{UUID: devseed.DeviceServerCIID, Valid: true}, store.AgentRuntimeStatusWARNING, pgtype.Int4{}, "Aguardando aprovação de deploy"},
	}
	for _, e := range executions {
		if _, err := q.UpsertExecution(ctx, store.UpsertExecutionParams{
			ID:              id(e.key),
			TaskStepID:      stepIDs[e.stepKey],
			AgentID:         e.agentID,
			DeviceID:        e.deviceID,
			AttemptNumber:   1,
			Status:          e.status,
			ProgressPercent: e.progress,
			CurrentActivity: e.activity,
			StartedAt:       ts(42),
			FinishedAt:      pgtype.Timestamptz{},
		}); err != nil {
			fail("seed execution "+e.key, err)
		}
	}

	activityLog := []struct {
		key         string
		message     string
		highlighted bool
		minutesAgo  int
	}{
		{"184-event-1", "Tarefa criada pelo usuário", false, 14},
		{"184-event-2", "Orquestrador: Agent Dev iniciado", false, 13},
		{"184-event-3", "Agent Dev: auth.service.ts aberto", false, 7},
		{"184-event-4", "Agent Dev: analisando stack trace linha 284", false, 3},
		{"184-event-5", "Agent Dev: patch aplicado em auth.service.ts", true, 1},
	}
	for _, e := range activityLog {
		if _, err := q.UpsertExecutionEvent(ctx, store.UpsertExecutionEventParams{
			ID:          id(e.key),
			TaskID:      task.ID,
			ExecutionID: uuid.NullUUID{UUID: id("184-exec-dev"), Valid: true},
			AgentID:     uuid.NullUUID{UUID: devseed.AgentDevID, Valid: true},
			EventType:   "log",
			Message:     e.message,
			Highlighted: e.highlighted,
			OccurredAt:  ts(e.minutesAgo),
		}); err != nil {
			fail("seed execution_event "+e.key, err)
		}
	}
}

// seedOtherTasks cobre #183 (fila), #182/#181 (concluídas) e #179 (falhou) — dados coerentes com
// o status, não fidelidade de log completa (só a #184 tem essa garantia, ver docs do plano).
func seedOtherTasks(ctx context.Context, q *store.Queries) {
	type otherTask struct {
		code       string
		title      string
		status     store.TaskStatus
		progress   int32
		withSteps  bool
		stepStatus store.ExecutionNodeStatus
		execStatus store.AgentRuntimeStatus
		finished   bool
	}
	tasksData := []otherTask{
		{"#183", "Adicionar rate limiting na API", store.TaskStatusQUEUED, 0, false, "", "", false},
		{"#182", "Refactor módulo de autenticação", store.TaskStatusCOMPLETED, 100, true, store.ExecutionNodeStatusDONE, store.AgentRuntimeStatusIDLE, true},
		{"#181", "Migração PostgreSQL → Supabase", store.TaskStatusCOMPLETED, 100, true, store.ExecutionNodeStatusDONE, store.AgentRuntimeStatusIDLE, true},
		{"#179", "Configurar CI/CD pipeline", store.TaskStatusFAILED, 45, true, store.ExecutionNodeStatusACTIVE, store.AgentRuntimeStatusERROR, true},
	}

	for _, t := range tasksData {
		taskID := id("task-" + t.code)
		task, err := q.UpsertTask(ctx, store.UpsertTaskParams{
			ID:              taskID,
			ProjectID:       devidentity.DevProjectID,
			Code:            t.code,
			Title:           t.title,
			Status:          t.status,
			ProgressPercent: t.progress,
			CreatedByUserID: uuid.NullUUID{UUID: devidentity.DevUserID, Valid: true},
		})
		if err != nil {
			fail("seed task "+t.code, err)
		}
		if !t.withSteps {
			continue
		}

		stepID := id("step-" + t.code)
		if _, err := q.UpsertTaskStep(ctx, store.UpsertTaskStepParams{
			ID: stepID, TaskID: task.ID, SequenceOrder: 1, Label: "Agent Dev", Status: t.stepStatus,
			AgentID: uuid.NullUUID{UUID: devseed.AgentDevID, Valid: true},
		}); err != nil {
			fail("seed task_step for "+t.code, err)
		}

		finishedAt := pgtype.Timestamptz{}
		if t.finished {
			finishedAt = ts(60)
		}
		if _, err := q.UpsertExecution(ctx, store.UpsertExecutionParams{
			ID:              id("exec-" + t.code),
			TaskStepID:      stepID,
			AgentID:         devseed.AgentDevID,
			DeviceID:        uuid.NullUUID{UUID: devseed.DeviceWorkstation01ID, Valid: true},
			AttemptNumber:   1,
			Status:          t.execStatus,
			ProgressPercent: pgtype.Int4{Int32: t.progress, Valid: true},
			CurrentActivity: fmt.Sprintf("%s: %s", t.code, t.status),
			StartedAt:       ts(120),
			FinishedAt:      finishedAt,
		}); err != nil {
			fail("seed execution for "+t.code, err)
		}
	}
}

func seedApproval(ctx context.Context, q *store.Queries) {
	payload := []byte(`{
		"environment": "staging.atlas.dev",
		"branch": "fix/auth-bug-184",
		"commit_summary": "3 novos commits",
		"image": "atlas:1.4.2-staging.7",
		"rollback_policy": "Automático em falha",
		"tests_passed": 32,
		"tests_total": 32
	}`)
	if _, err := q.UpsertApproval(ctx, store.UpsertApprovalParams{
		ID:                 id("approval-184-staging"),
		WorkspaceID:        devidentity.DevWorkspaceID,
		TaskID:             uuid.NullUUID{UUID: devseed.Task184ID, Valid: true},
		ExecutionID:        uuid.NullUUID{UUID: id("184-exec-devops"), Valid: true},
		RequestedByAgentID: uuid.NullUUID{UUID: devseed.AgentDevOpsID, Valid: true},
		Title:              "Deploy em staging",
		Description:        "O Agent DevOps preparou o deploy do fix do bug #184 para o ambiente de staging. Todos os testes unitários passaram com sucesso.",
		Payload:            payload,
	}); err != nil {
		fail("seed approval", err)
	}
}

func fail(step string, err error) {
	slog.Error("seed step failed", "step", step, "error", err)
	os.Exit(1)
}
