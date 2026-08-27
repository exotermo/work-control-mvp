package auth

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgtype"

	"github.com/example/work-control/services/api-go/internal/store"
)

type fakeAccountQueries struct {
	resolvedUserID uuid.UUID
	resolveErr     error
	identityParams store.GetUserIDByExternalIdentityParams
	user           store.User
	memberships    []store.ListWorkspaceMembershipsForUserRow
}

func (f *fakeAccountQueries) GetUserIDByExternalIdentity(_ context.Context, params store.GetUserIDByExternalIdentityParams) (uuid.UUID, error) {
	f.identityParams = params
	return f.resolvedUserID, f.resolveErr
}

func (f *fakeAccountQueries) GetUserProfile(context.Context, uuid.UUID) (store.User, error) {
	return f.user, nil
}

func (f *fakeAccountQueries) ListWorkspaceMembershipsForUser(context.Context, uuid.UUID) ([]store.ListWorkspaceMembershipsForUserRow, error) {
	return f.memberships, nil
}

func TestSQLAccountStoreResolvesExactIssuerAndSubject(t *testing.T) {
	userID := uuid.New()
	queries := &fakeAccountQueries{resolvedUserID: userID}
	accounts := NewSQLAccountStore(queries)

	got, err := accounts.ResolveUserID(context.Background(), "https://Issuer.test/tenant", "Case-Sensitive-Subject")
	if err != nil {
		t.Fatalf("ResolveUserID() error = %v", err)
	}
	if got != userID {
		t.Fatalf("user ID = %s, want %s", got, userID)
	}
	if queries.identityParams.Issuer != "https://Issuer.test/tenant" || queries.identityParams.Subject != "Case-Sensitive-Subject" {
		t.Fatalf("identity params were modified: %#v", queries.identityParams)
	}
}

func TestSQLAccountStoreMapsMissingIdentityToProvisioningError(t *testing.T) {
	accounts := NewSQLAccountStore(&fakeAccountQueries{resolveErr: pgx.ErrNoRows})
	_, err := accounts.ResolveUserID(context.Background(), "https://issuer.test", "unknown")
	if !errors.Is(err, ErrIdentityNotLinked) {
		t.Fatalf("ResolveUserID() error = %v, want ErrIdentityNotLinked", err)
	}
}

func TestSQLAccountStoreLoadsEveryMembership(t *testing.T) {
	createdAt := time.Date(2026, 8, 24, 10, 0, 0, 0, time.UTC)
	userID := uuid.New()
	queries := &fakeAccountQueries{
		user: store.User{
			ID: userID, Email: "user@example.test", DisplayName: "User",
			CreatedAt: pgtype.Timestamptz{Time: createdAt, Valid: true},
		},
		memberships: []store.ListWorkspaceMembershipsForUserRow{
			{WorkspaceID: uuid.New(), WorkspaceName: "Alpha", WorkspaceSlug: "alpha", Role: "owner"},
			{WorkspaceID: uuid.New(), WorkspaceName: "Beta", WorkspaceSlug: "beta", Role: "member"},
		},
	}

	account, err := NewSQLAccountStore(queries).Account(context.Background(), userID)
	if err != nil {
		t.Fatalf("Account() error = %v", err)
	}
	if account.ID != userID || account.CreatedAt != createdAt || len(account.Workspaces) != 2 {
		t.Fatalf("unexpected account: %#v", account)
	}
}
