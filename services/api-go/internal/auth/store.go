package auth

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"

	"github.com/example/work-control/services/api-go/internal/store"
)

type accountQueries interface {
	GetUserIDByExternalIdentity(context.Context, store.GetUserIDByExternalIdentityParams) (uuid.UUID, error)
	GetUserProfile(context.Context, uuid.UUID) (store.User, error)
	ListWorkspaceMembershipsForUser(context.Context, uuid.UUID) ([]store.ListWorkspaceMembershipsForUserRow, error)
}

type SQLAccountStore struct {
	queries accountQueries
}

func NewSQLAccountStore(queries accountQueries) *SQLAccountStore {
	return &SQLAccountStore{queries: queries}
}

func (s *SQLAccountStore) ResolveUserID(ctx context.Context, issuer, subject string) (uuid.UUID, error) {
	userID, err := s.queries.GetUserIDByExternalIdentity(ctx, store.GetUserIDByExternalIdentityParams{
		Issuer:  issuer,
		Subject: subject,
	})
	if errors.Is(err, pgx.ErrNoRows) {
		return uuid.Nil, ErrIdentityNotLinked
	}
	if err != nil {
		return uuid.Nil, fmt.Errorf("query external identity: %w", err)
	}
	return userID, nil
}

type Account struct {
	ID          uuid.UUID
	Email       string
	DisplayName string
	CreatedAt   time.Time
	Workspaces  []Membership
}

type Membership struct {
	WorkspaceID   uuid.UUID
	WorkspaceName string
	WorkspaceSlug string
	Role          string
	JoinedAt      time.Time
}

func (s *SQLAccountStore) Account(ctx context.Context, userID uuid.UUID) (Account, error) {
	user, err := s.queries.GetUserProfile(ctx, userID)
	if err != nil {
		return Account{}, fmt.Errorf("query user profile: %w", err)
	}
	rows, err := s.queries.ListWorkspaceMembershipsForUser(ctx, userID)
	if err != nil {
		return Account{}, fmt.Errorf("query workspace memberships: %w", err)
	}

	memberships := make([]Membership, 0, len(rows))
	for _, row := range rows {
		memberships = append(memberships, Membership{
			WorkspaceID:   row.WorkspaceID,
			WorkspaceName: row.WorkspaceName,
			WorkspaceSlug: row.WorkspaceSlug,
			Role:          row.Role,
			JoinedAt:      row.JoinedAt.Time,
		})
	}
	return Account{
		ID:          user.ID,
		Email:       user.Email,
		DisplayName: user.DisplayName,
		CreatedAt:   user.CreatedAt.Time,
		Workspaces:  memberships,
	}, nil
}
