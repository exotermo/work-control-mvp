package auth

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/google/uuid"
)

type fakeAccountReader struct {
	account Account
	err     error
	userID  uuid.UUID
}

func (f *fakeAccountReader) Account(_ context.Context, userID uuid.UUID) (Account, error) {
	f.userID = userID
	return f.account, f.err
}

func TestMeReturnsInternalProfileAndAllMemberships(t *testing.T) {
	userID := uuid.New()
	firstWorkspaceID := uuid.New()
	secondWorkspaceID := uuid.New()
	accounts := &fakeAccountReader{account: Account{
		ID:          userID,
		Email:       "internal@example.test",
		DisplayName: "Internal User",
		CreatedAt:   time.Date(2026, 8, 24, 12, 0, 0, 0, time.UTC),
		Workspaces: []Membership{
			{WorkspaceID: firstWorkspaceID, WorkspaceName: "Alpha", WorkspaceSlug: "alpha", Role: "owner"},
			{WorkspaceID: secondWorkspaceID, WorkspaceName: "Beta", WorkspaceSlug: "beta", Role: "member"},
		},
	}}
	handler := &MeHandler{Accounts: accounts}
	mux := http.NewServeMux()
	mux.HandleFunc("GET /v1/me", handler.Get)

	request := httptest.NewRequest(http.MethodGet, "/v1/me", nil)
	response := httptest.NewRecorder()
	Middleware(userID, uuid.New())(mux).ServeHTTP(response, request)

	if response.Code != http.StatusOK {
		t.Fatalf("status = %d, body = %s", response.Code, response.Body.String())
	}
	var body struct {
		User struct {
			ID    string `json:"id"`
			Email string `json:"email"`
		} `json:"user"`
		Workspaces []struct {
			ID   string `json:"id"`
			Role string `json:"role"`
		} `json:"workspaces"`
	}
	if err := json.Unmarshal(response.Body.Bytes(), &body); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	if body.User.ID != userID.String() || body.User.Email != "internal@example.test" {
		t.Fatalf("unexpected user: %#v", body.User)
	}
	if len(body.Workspaces) != 2 || body.Workspaces[0].ID != firstWorkspaceID.String() || body.Workspaces[1].Role != "member" {
		t.Fatalf("unexpected workspaces: %#v", body.Workspaces)
	}
	if accounts.userID != userID {
		t.Fatalf("account lookup user ID = %s", accounts.userID)
	}
}

func TestMeRejectsMissingContext(t *testing.T) {
	handler := &MeHandler{Accounts: &fakeAccountReader{}}
	response := httptest.NewRecorder()
	handler.Get(response, httptest.NewRequest(http.MethodGet, "/v1/me", nil))
	if response.Code != http.StatusUnauthorized {
		t.Fatalf("status = %d, want 401", response.Code)
	}
}

func TestMeHandlesAccountStoreFailure(t *testing.T) {
	userID := uuid.New()
	handler := &MeHandler{Accounts: &fakeAccountReader{err: errors.New("database failed")}}
	request := httptest.NewRequest(http.MethodGet, "/v1/me", nil)
	response := httptest.NewRecorder()
	Middleware(userID, uuid.New())(http.HandlerFunc(handler.Get)).ServeHTTP(response, request)
	if response.Code != http.StatusInternalServerError {
		t.Fatalf("status = %d, want 500", response.Code)
	}
}
