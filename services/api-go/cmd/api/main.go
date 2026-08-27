package main

import (
	"context"
	"log/slog"
	"net"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/example/work-control/services/api-go/internal/agents"
	"github.com/example/work-control/services/api-go/internal/approvals"
	"github.com/example/work-control/services/api-go/internal/auth"
	"github.com/example/work-control/services/api-go/internal/config"
	"github.com/example/work-control/services/api-go/internal/devices"
	"github.com/example/work-control/services/api-go/internal/devidentity"
	"github.com/example/work-control/services/api-go/internal/events"
	"github.com/example/work-control/services/api-go/internal/health"
	"github.com/example/work-control/services/api-go/internal/store"
	"github.com/example/work-control/services/api-go/internal/tasks"
	"github.com/example/work-control/services/api-go/internal/workspace"
)

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	cfg, err := config.Load()
	if err != nil {
		slog.Error("invalid configuration", "error", err)
		os.Exit(1)
	}

	pool, err := pgxpool.New(ctx, cfg.PostgresDSN)
	if err != nil {
		slog.Error("failed to connect to postgres", "error", err)
		os.Exit(1)
	}
	defer pool.Close()

	if err := pool.Ping(ctx); err != nil {
		slog.Error("postgres ping failed", "error", err)
		os.Exit(1)
	}

	queries := store.New(pool)
	accounts := auth.NewSQLAccountStore(queries)
	hub := events.NewHub()

	if cfg.DemoSimulator {
		go events.RunDemoSimulator(ctx, queries, hub)
	}

	workspaceHandler := &workspace.Handler{Queries: queries}
	tasksHandler := &tasks.Handler{Queries: queries, Hub: hub}
	agentsHandler := &agents.Handler{Queries: queries}
	devicesHandler := &devices.Handler{Queries: queries}
	approvalsHandler := &approvals.Handler{Queries: queries}
	wsHandler := &events.WSHandler{Hub: hub}
	healthHandler := &health.Handler{Database: pool}
	meHandler := &auth.MeHandler{Accounts: accounts}

	apiMux := http.NewServeMux()
	apiMux.HandleFunc("GET /v1/me", meHandler.Get)
	apiMux.HandleFunc("GET /v1/workspaces", workspaceHandler.List)
	apiMux.HandleFunc("GET /v1/workspaces/{id}/dashboard", workspaceHandler.Dashboard)
	apiMux.HandleFunc("GET /v1/tasks", tasksHandler.List)
	apiMux.HandleFunc("GET /v1/tasks/{id}", tasksHandler.Get)
	apiMux.HandleFunc("POST /v1/tasks", tasksHandler.Create)
	apiMux.HandleFunc("GET /v1/tasks/{id}/events/ws", wsHandler.Serve)
	apiMux.HandleFunc("GET /v1/agents", agentsHandler.ListActive)
	apiMux.HandleFunc("GET /v1/devices", devicesHandler.List)
	apiMux.HandleFunc("GET /v1/devices/{id}", devicesHandler.Get)
	apiMux.HandleFunc("GET /v1/approvals", approvalsHandler.Pending)
	apiMux.HandleFunc("POST /v1/approvals/{id}/approve", approvalsHandler.Approve)
	apiMux.HandleFunc("POST /v1/approvals/{id}/reject", approvalsHandler.Reject)

	rootMux := http.NewServeMux()
	rootMux.HandleFunc("GET /health/live", healthHandler.Live)
	rootMux.HandleFunc("GET /health/ready", healthHandler.Ready)

	var authenticatedAPI http.Handler
	switch cfg.AuthMode {
	case config.AuthModeDev:
		authenticatedAPI = auth.Middleware(devidentity.DevUserID, devidentity.DevWorkspaceID)(apiMux)
	case config.AuthModeOIDC:
		verifier, err := auth.NewOIDCVerifier(ctx, cfg.OIDC)
		if err != nil {
			slog.Error("failed to initialize OIDC authentication", "error", err)
			os.Exit(1)
		}
		authenticatedAPI = auth.OIDCMiddleware(verifier, accounts)(apiMux)
	}
	rootMux.Handle("/", authenticatedAPI)

	server := &http.Server{
		Addr:    net.JoinHostPort(cfg.HTTPHost, cfg.HTTPPort),
		Handler: rootMux,
	}

	go func() {
		<-ctx.Done()
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = server.Shutdown(shutdownCtx)
	}()

	slog.Info("work control api listening", "address", server.Addr, "demo_simulator", cfg.DemoSimulator, "auth_mode", cfg.AuthMode)
	if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		slog.Error("server error", "error", err)
		os.Exit(1)
	}
}
