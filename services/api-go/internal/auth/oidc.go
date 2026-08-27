package auth

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strings"

	"github.com/coreos/go-oidc/v3/oidc"

	"github.com/example/work-control/services/api-go/internal/config"
)

type OIDCVerifier struct {
	issuer            string
	requiredScope     string
	expectedTokenType string
	verifier          *oidc.IDTokenVerifier
}

func NewOIDCVerifier(ctx context.Context, cfg config.OIDCConfig) (*OIDCVerifier, error) {
	provider, err := oidc.NewProvider(ctx, cfg.Issuer)
	if err != nil {
		return nil, fmt.Errorf("discover OIDC provider: %w", err)
	}

	return &OIDCVerifier{
		issuer:            cfg.Issuer,
		requiredScope:     cfg.RequiredScope,
		expectedTokenType: cfg.ExpectedTokenType,
		verifier: provider.Verifier(&oidc.Config{
			ClientID:             cfg.Audience,
			SupportedSigningAlgs: cfg.SigningAlgorithms,
		}),
	}, nil
}

// Verify uses go-oidc's discovery/JWKS verifier for signature, exact issuer, audience, exp
// and nbf checks. Despite the library type being named IDTokenVerifier, the signed JWT checks
// are generic. To prevent ID/access-token confusion, this API additionally requires its own
// audience, an API scope, and the configured access-token type claim. The Keycloak reference
// emits typ=Bearer in access-token claims and not in ID tokens. A provider using RFC 9068's
// header typ=at+jwt needs an explicit profile extension rather than silently weakening this.
func (v *OIDCVerifier) Verify(ctx context.Context, rawToken string) (TokenIdentity, error) {
	token, err := v.verifier.Verify(ctx, rawToken)
	if err != nil {
		return TokenIdentity{}, fmt.Errorf("verify signed access token: %w", err)
	}

	var claims accessTokenClaims
	if err := token.Claims(&claims); err != nil {
		return TokenIdentity{}, fmt.Errorf("decode access token claims: %w", err)
	}
	if err := validateAccessClaims(token.Issuer, token.Subject, claims, v.issuer, v.requiredScope, v.expectedTokenType); err != nil {
		return TokenIdentity{}, err
	}

	return TokenIdentity{Issuer: token.Issuer, Subject: token.Subject}, nil
}

type accessTokenClaims struct {
	Scope     scopeSet `json:"scope"`
	TokenType string   `json:"typ"`
}

func validateAccessClaims(issuer, subject string, claims accessTokenClaims, expectedIssuer, requiredScope, expectedTokenType string) error {
	if issuer != expectedIssuer {
		return errors.New("access token issuer does not match configured issuer")
	}
	if subject == "" {
		return errors.New("access token subject is empty")
	}
	if claims.TokenType != expectedTokenType {
		return fmt.Errorf("access token type %q is not accepted", claims.TokenType)
	}
	if !claims.Scope.Contains(requiredScope) {
		return fmt.Errorf("access token is missing required scope %q", requiredScope)
	}
	return nil
}

type scopeSet map[string]struct{}

func (s *scopeSet) UnmarshalJSON(data []byte) error {
	var text string
	if err := json.Unmarshal(data, &text); err == nil {
		*s = make(scopeSet)
		for _, scope := range strings.Fields(text) {
			(*s)[scope] = struct{}{}
		}
		return nil
	}

	var list []string
	if err := json.Unmarshal(data, &list); err != nil {
		return errors.New("scope claim must be a string or string array")
	}
	*s = make(scopeSet, len(list))
	for _, scope := range list {
		if scope == "" || strings.ContainsAny(scope, " \t\r\n") {
			return errors.New("scope array contains an invalid value")
		}
		(*s)[scope] = struct{}{}
	}
	return nil
}

func (s scopeSet) Contains(scope string) bool {
	_, ok := s[scope]
	return ok
}
