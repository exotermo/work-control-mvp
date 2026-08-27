package config

import (
	"strings"
	"testing"
)

func TestLoadDefaultsToDevOnlyInLocal(t *testing.T) {
	t.Setenv("APP_ENV", "local")
	t.Setenv("AUTH_MODE", "dev")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}
	if cfg.AuthMode != AuthModeDev {
		t.Fatalf("AuthMode = %q, want dev", cfg.AuthMode)
	}
	if cfg.HTTPHost != "127.0.0.1" {
		t.Fatalf("HTTPHost = %q, want 127.0.0.1", cfg.HTTPHost)
	}
}

func TestLoadAcceptsLoopbackHTTPHost(t *testing.T) {
	t.Setenv("APP_ENV", "local")
	t.Setenv("AUTH_MODE", "dev")
	t.Setenv("HTTP_HOST", "127.0.0.1")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}
	if cfg.HTTPHost != "127.0.0.1" {
		t.Fatalf("HTTPHost = %q, want 127.0.0.1", cfg.HTTPHost)
	}
}

func TestLoadRejectsDevOutsideLocal(t *testing.T) {
	for _, appEnv := range []string{"homolog", "production"} {
		t.Run(appEnv, func(t *testing.T) {
			t.Setenv("APP_ENV", appEnv)
			t.Setenv("AUTH_MODE", "dev")

			_, err := Load()
			if err == nil || !strings.Contains(err.Error(), "allowed only") {
				t.Fatalf("Load() error = %v, want dev-mode gate error", err)
			}
		})
	}
}

func TestLoadRejectsUnknownAuthMode(t *testing.T) {
	t.Setenv("APP_ENV", "local")
	t.Setenv("AUTH_MODE", "disabled")

	_, err := Load()
	if err == nil || !strings.Contains(err.Error(), "unsupported AUTH_MODE") {
		t.Fatalf("Load() error = %v, want unsupported mode", err)
	}
}

func TestLoadRequiresCompleteOIDCConfiguration(t *testing.T) {
	t.Setenv("APP_ENV", "production")
	t.Setenv("AUTH_MODE", "oidc")
	t.Setenv("OIDC_ALLOWED_ISSUER", "")
	t.Setenv("OIDC_AUDIENCE", "")

	_, err := Load()
	if err == nil || !strings.Contains(err.Error(), "OIDC_ALLOWED_ISSUER") {
		t.Fatalf("Load() error = %v, want missing issuer", err)
	}
}

func TestLoadAcceptsProviderNeutralOIDCConfiguration(t *testing.T) {
	t.Setenv("APP_ENV", "production")
	t.Setenv("AUTH_MODE", "oidc")
	t.Setenv("OIDC_ALLOWED_ISSUER", "https://identity.example.test/tenant")
	t.Setenv("OIDC_AUDIENCE", "urn:work-control:api:prod")
	t.Setenv("OIDC_REQUIRED_SCOPE", "work-control.api")
	t.Setenv("OIDC_EXPECTED_TOKEN_TYPE", "Bearer")
	t.Setenv("OIDC_SIGNING_ALGORITHMS", "RS256, ES256")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}
	if len(cfg.OIDC.SigningAlgorithms) != 2 || cfg.OIDC.SigningAlgorithms[1] != "ES256" {
		t.Fatalf("SigningAlgorithms = %#v", cfg.OIDC.SigningAlgorithms)
	}
}

func TestOIDCRequiresHTTPSOutsideLocal(t *testing.T) {
	cfg := Config{
		AppEnv:   "production",
		AuthMode: AuthModeOIDC,
		OIDC: OIDCConfig{
			Issuer:            "http://identity.example.test/tenant",
			Audience:          "work-control-api",
			RequiredScope:     "work-control.api",
			ExpectedTokenType: "Bearer",
			SigningAlgorithms: []string{"RS256"},
		},
	}

	err := cfg.Validate()
	if err == nil || !strings.Contains(err.Error(), "https") {
		t.Fatalf("Validate() error = %v, want https requirement", err)
	}
}

func TestOIDCRejectsUnsignedAlgorithm(t *testing.T) {
	cfg := Config{
		AppEnv:   "local",
		AuthMode: AuthModeOIDC,
		OIDC: OIDCConfig{
			Issuer:            "http://localhost:8180/realms/work-control-local",
			Audience:          "work-control-api",
			RequiredScope:     "work-control.api",
			ExpectedTokenType: "Bearer",
			SigningAlgorithms: []string{"none"},
		},
	}

	err := cfg.Validate()
	if err == nil || !strings.Contains(err.Error(), "unsupported algorithm") {
		t.Fatalf("Validate() error = %v, want algorithm rejection", err)
	}
}
