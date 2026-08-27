package auth

import (
	"context"
	"crypto/rand"
	"crypto/rsa"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/go-jose/go-jose/v4"

	"github.com/example/work-control/services/api-go/internal/config"
)

func TestValidateAccessClaims(t *testing.T) {
	validScopes := scopeSet{"openid": {}, "work-control.api": {}}
	tests := []struct {
		name    string
		issuer  string
		subject string
		claims  accessTokenClaims
	}{
		{name: "wrong issuer", issuer: "https://other.test", subject: "sub", claims: accessTokenClaims{Scope: validScopes, TokenType: "Bearer"}},
		{name: "empty subject", issuer: "https://issuer.test", subject: "", claims: accessTokenClaims{Scope: validScopes, TokenType: "Bearer"}},
		{name: "ID token type", issuer: "https://issuer.test", subject: "sub", claims: accessTokenClaims{Scope: validScopes, TokenType: "ID"}},
		{name: "missing API scope", issuer: "https://issuer.test", subject: "sub", claims: accessTokenClaims{Scope: scopeSet{"openid": {}}, TokenType: "Bearer"}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := validateAccessClaims(tt.issuer, tt.subject, tt.claims, "https://issuer.test", "work-control.api", "Bearer"); err == nil {
				t.Fatal("validateAccessClaims() returned nil")
			}
		})
	}
}

func TestOIDCVerifierUsesDiscoveryJWKSAndRejectsInvalidJWTs(t *testing.T) {
	privateKey, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		t.Fatalf("generate signing key: %v", err)
	}
	otherKey, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		t.Fatalf("generate second signing key: %v", err)
	}

	var issuerURL string
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.URL.Path {
		case "/.well-known/openid-configuration":
			_ = json.NewEncoder(w).Encode(map[string]any{
				"issuer":                                issuerURL,
				"jwks_uri":                              issuerURL + "/jwks",
				"id_token_signing_alg_values_supported": []string{"RS256"},
			})
		case "/jwks":
			_ = json.NewEncoder(w).Encode(jose.JSONWebKeySet{Keys: []jose.JSONWebKey{{
				Key: &privateKey.PublicKey, KeyID: "test-key", Algorithm: "RS256", Use: "sig",
			}}})
		default:
			http.NotFound(w, r)
		}
	}))
	defer server.Close()
	issuerURL = server.URL

	verifier, err := NewOIDCVerifier(context.Background(), config.OIDCConfig{
		Issuer:            issuerURL,
		Audience:          "urn:work-control:api:test",
		RequiredScope:     "work-control.api",
		ExpectedTokenType: "Bearer",
		SigningAlgorithms: []string{"RS256"},
	})
	if err != nil {
		t.Fatalf("NewOIDCVerifier() error = %v", err)
	}

	now := time.Now()
	validClaims := map[string]any{
		"iss":   issuerURL,
		"sub":   "external-subject",
		"aud":   "urn:work-control:api:test",
		"exp":   now.Add(time.Hour).Unix(),
		"iat":   now.Add(-time.Minute).Unix(),
		"scope": "openid work-control.api",
		"typ":   "Bearer",
	}

	validToken := signJWT(t, privateKey, validClaims)
	identity, err := verifier.Verify(context.Background(), validToken)
	if err != nil {
		t.Fatalf("Verify(valid token) error = %v", err)
	}
	if identity.Issuer != issuerURL || identity.Subject != "external-subject" {
		t.Fatalf("identity = %#v", identity)
	}

	tests := []struct {
		name   string
		key    *rsa.PrivateKey
		mutate func(map[string]any)
	}{
		{name: "invalid signature", key: otherKey},
		{name: "wrong issuer", key: privateKey, mutate: func(c map[string]any) { c["iss"] = "https://other.test" }},
		{name: "wrong audience", key: privateKey, mutate: func(c map[string]any) { c["aud"] = "another-api" }},
		{name: "expired", key: privateKey, mutate: func(c map[string]any) { c["exp"] = now.Add(-time.Minute).Unix() }},
		{name: "empty subject", key: privateKey, mutate: func(c map[string]any) { c["sub"] = "" }},
		{name: "missing scope", key: privateKey, mutate: func(c map[string]any) { c["scope"] = "openid profile" }},
		{name: "ID token confusion", key: privateKey, mutate: func(c map[string]any) { c["typ"] = "ID" }},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			claims := cloneClaims(validClaims)
			if tt.mutate != nil {
				tt.mutate(claims)
			}
			if _, err := verifier.Verify(context.Background(), signJWT(t, tt.key, claims)); err == nil {
				t.Fatal("Verify() returned nil error")
			}
		})
	}
}

func signJWT(t *testing.T, key *rsa.PrivateKey, claims map[string]any) string {
	t.Helper()
	options := (&jose.SignerOptions{}).WithType("JWT").WithHeader("kid", "test-key")
	signer, err := jose.NewSigner(jose.SigningKey{Algorithm: jose.RS256, Key: key}, options)
	if err != nil {
		t.Fatalf("new signer: %v", err)
	}
	payload, err := json.Marshal(claims)
	if err != nil {
		t.Fatalf("marshal claims: %v", err)
	}
	signed, err := signer.Sign(payload)
	if err != nil {
		t.Fatalf("sign claims: %v", err)
	}
	compact, err := signed.CompactSerialize()
	if err != nil {
		t.Fatalf("serialize token: %v", err)
	}
	return compact
}

func cloneClaims(source map[string]any) map[string]any {
	result := make(map[string]any, len(source))
	for key, value := range source {
		result[key] = value
	}
	return result
}
