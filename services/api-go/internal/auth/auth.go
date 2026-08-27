// Package auth autentica requests e injeta a identidade interna no contexto.
package auth

import (
	"context"
	"errors"
	"net/http"
	"strings"

	"github.com/google/uuid"

	"github.com/example/work-control/services/api-go/internal/httpx"
)

type CurrentUser struct {
	UserID      uuid.UUID
	WorkspaceID uuid.UUID
}

type ctxKey struct{}

// Middleware is the fixed-identity bridge retained exclusively for AUTH_MODE=dev. Existing
// handlers still rely on its single WorkspaceID. OIDC authentication deliberately does not
// invent an active workspace: resource-level membership enforcement remains a separate gap.
func Middleware(userID, workspaceID uuid.UUID) func(http.Handler) http.Handler {
	current := CurrentUser{UserID: userID, WorkspaceID: workspaceID}
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ctx := context.WithValue(r.Context(), ctxKey{}, current)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

type TokenIdentity struct {
	Issuer  string
	Subject string
}

type TokenVerifier interface {
	Verify(context.Context, string) (TokenIdentity, error)
}

type IdentityResolver interface {
	ResolveUserID(context.Context, string, string) (uuid.UUID, error)
}

var ErrIdentityNotLinked = errors.New("external identity is not linked to an internal user")

func OIDCMiddleware(verifier TokenVerifier, resolver IdentityResolver) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			rawToken, ok := bearerToken(r.Header.Get("Authorization"))
			if !ok {
				w.Header().Set("WWW-Authenticate", "Bearer")
				httpx.WriteError(w, http.StatusUnauthorized, "authentication required")
				return
			}

			identity, err := verifier.Verify(r.Context(), rawToken)
			if err != nil {
				w.Header().Set("WWW-Authenticate", `Bearer error="invalid_token"`)
				httpx.WriteError(w, http.StatusUnauthorized, "invalid access token")
				return
			}

			userID, err := resolver.ResolveUserID(r.Context(), identity.Issuer, identity.Subject)
			switch {
			case errors.Is(err, ErrIdentityNotLinked):
				httpx.WriteError(w, http.StatusForbidden, "identity is not provisioned")
				return
			case err != nil:
				httpx.WriteError(w, http.StatusInternalServerError, "failed to resolve identity")
				return
			}

			current := CurrentUser{UserID: userID}
			ctx := context.WithValue(r.Context(), ctxKey{}, current)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func bearerToken(value string) (string, bool) {
	parts := strings.Fields(value)
	if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") || parts[1] == "" {
		return "", false
	}
	return parts[1], true
}

func FromContext(ctx context.Context) CurrentUser {
	u, _ := ctx.Value(ctxKey{}).(CurrentUser)
	return u
}

func Lookup(ctx context.Context) (CurrentUser, bool) {
	u, ok := ctx.Value(ctxKey{}).(CurrentUser)
	return u, ok
}
