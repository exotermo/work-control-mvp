package auth

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/google/uuid"
)

type fakeVerifier struct {
	identity TokenIdentity
	err      error
	raw      string
}

func (f *fakeVerifier) Verify(_ context.Context, raw string) (TokenIdentity, error) {
	f.raw = raw
	return f.identity, f.err
}

type fakeResolver struct {
	userID  uuid.UUID
	err     error
	issuer  string
	subject string
}

func (f *fakeResolver) ResolveUserID(_ context.Context, issuer, subject string) (uuid.UUID, error) {
	f.issuer = issuer
	f.subject = subject
	return f.userID, f.err
}

func TestOIDCMiddlewareAuthenticatesAndMapsExternalIdentity(t *testing.T) {
	userID := uuid.New()
	verifier := &fakeVerifier{identity: TokenIdentity{Issuer: "https://issuer.test", Subject: "subject-123"}}
	resolver := &fakeResolver{userID: userID}
	called := false
	next := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		called = true
		if got := FromContext(r.Context()).UserID; got != userID {
			t.Errorf("context user ID = %s, want %s", got, userID)
		}
		w.WriteHeader(http.StatusNoContent)
	})

	request := httptest.NewRequest(http.MethodGet, "/v1/me", nil)
	request.Header.Set("Authorization", "Bearer signed.jwt")
	response := httptest.NewRecorder()
	OIDCMiddleware(verifier, resolver)(next).ServeHTTP(response, request)

	if response.Code != http.StatusNoContent || !called {
		t.Fatalf("status = %d, called = %v", response.Code, called)
	}
	if verifier.raw != "signed.jwt" {
		t.Fatalf("verified token = %q", verifier.raw)
	}
	if resolver.issuer != "https://issuer.test" || resolver.subject != "subject-123" {
		t.Fatalf("resolved identity = (%q, %q)", resolver.issuer, resolver.subject)
	}
}

func TestOIDCMiddlewareFailsClosed(t *testing.T) {
	tests := []struct {
		name       string
		header     string
		verifyErr  error
		resolveErr error
		wantStatus int
	}{
		{name: "missing bearer", wantStatus: http.StatusUnauthorized},
		{name: "malformed bearer", header: "Basic token", wantStatus: http.StatusUnauthorized},
		{name: "invalid token", header: "Bearer token", verifyErr: errors.New("signature failed"), wantStatus: http.StatusUnauthorized},
		{name: "unlinked identity", header: "Bearer token", resolveErr: ErrIdentityNotLinked, wantStatus: http.StatusForbidden},
		{name: "database failure", header: "Bearer token", resolveErr: errors.New("database unavailable"), wantStatus: http.StatusInternalServerError},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			verifier := &fakeVerifier{identity: TokenIdentity{Issuer: "https://issuer.test", Subject: "sub"}, err: tt.verifyErr}
			resolver := &fakeResolver{userID: uuid.New(), err: tt.resolveErr}
			nextCalled := false
			next := http.HandlerFunc(func(http.ResponseWriter, *http.Request) { nextCalled = true })
			request := httptest.NewRequest(http.MethodGet, "/v1/me", nil)
			if tt.header != "" {
				request.Header.Set("Authorization", tt.header)
			}
			response := httptest.NewRecorder()

			OIDCMiddleware(verifier, resolver)(next).ServeHTTP(response, request)

			if response.Code != tt.wantStatus {
				t.Fatalf("status = %d, want %d", response.Code, tt.wantStatus)
			}
			if nextCalled {
				t.Fatal("next handler was called on authentication failure")
			}
		})
	}
}
