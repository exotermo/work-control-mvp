// Package config carrega e valida a configuração do processo a partir de variáveis de ambiente.
package config

import (
	"errors"
	"fmt"
	"net/url"
	"os"
	"strings"
)

type AuthMode string

const (
	AuthModeDev  AuthMode = "dev"
	AuthModeOIDC AuthMode = "oidc"
)

type OIDCConfig struct {
	Issuer            string
	Audience          string
	RequiredScope     string
	ExpectedTokenType string
	SigningAlgorithms []string
}

type Config struct {
	AppEnv        string
	HTTPHost      string
	HTTPPort      string
	PostgresDSN   string
	RedisAddr     string
	SeedDevData   bool
	DemoSimulator bool
	AuthMode      AuthMode
	OIDC          OIDCConfig
}

// Load preserves the zero-setup local experience with AUTH_MODE=dev as the default. The
// validation below deliberately makes that default fatal in every non-local environment.
func Load() (Config, error) {
	cfg := Config{
		AppEnv:   getEnv("APP_ENV", "local"),
		HTTPHost: getEnv("HTTP_HOST", "127.0.0.1"),
		HTTPPort: getEnv("HTTP_PORT", "8080"),
		PostgresDSN: fmt.Sprintf(
			"postgres://%s:%s@%s:%s/%s?sslmode=disable",
			getEnv("POSTGRES_USER", "workcontrol"),
			getEnv("POSTGRES_PASSWORD", "local-only"),
			getEnv("POSTGRES_HOST", "localhost"),
			getEnv("POSTGRES_PORT", "5432"),
			getEnv("POSTGRES_DB", "workcontrol"),
		),
		RedisAddr:     fmt.Sprintf("%s:%s", getEnv("REDIS_HOST", "localhost"), getEnv("REDIS_PORT", "6379")),
		SeedDevData:   getEnv("SEED_DEV_DATA", "false") == "true",
		DemoSimulator: getEnv("DEMO_SIMULATOR", "false") == "true",
		AuthMode:      AuthMode(getEnv("AUTH_MODE", string(AuthModeDev))),
		OIDC: OIDCConfig{
			Issuer:            os.Getenv("OIDC_ALLOWED_ISSUER"),
			Audience:          os.Getenv("OIDC_AUDIENCE"),
			RequiredScope:     getEnv("OIDC_REQUIRED_SCOPE", "work-control.api"),
			ExpectedTokenType: getEnv("OIDC_EXPECTED_TOKEN_TYPE", "Bearer"),
			SigningAlgorithms: splitCSV(getEnv("OIDC_SIGNING_ALGORITHMS", "RS256")),
		},
	}
	if err := cfg.Validate(); err != nil {
		return Config{}, err
	}
	return cfg, nil
}

func (c Config) Validate() error {
	switch c.AuthMode {
	case AuthModeDev:
		if c.AppEnv != "local" {
			return fmt.Errorf("AUTH_MODE=dev is allowed only when APP_ENV=local (got %q)", c.AppEnv)
		}
		return nil
	case AuthModeOIDC:
		return c.OIDC.Validate(c.AppEnv)
	default:
		return fmt.Errorf("unsupported AUTH_MODE %q: expected dev or oidc", c.AuthMode)
	}
}

func (c OIDCConfig) Validate(appEnv string) error {
	if c.Issuer == "" {
		return errors.New("OIDC_ALLOWED_ISSUER is required when AUTH_MODE=oidc")
	}
	issuerURL, err := url.Parse(c.Issuer)
	if err != nil || issuerURL.Scheme == "" || issuerURL.Host == "" {
		return errors.New("OIDC_ALLOWED_ISSUER must be an absolute URL")
	}
	if issuerURL.RawQuery != "" || issuerURL.Fragment != "" || issuerURL.User != nil {
		return errors.New("OIDC_ALLOWED_ISSUER must not contain user info, query, or fragment")
	}
	if appEnv != "local" && issuerURL.Scheme != "https" {
		return errors.New("OIDC_ALLOWED_ISSUER must use https outside APP_ENV=local")
	}
	if c.Audience == "" {
		return errors.New("OIDC_AUDIENCE is required when AUTH_MODE=oidc")
	}
	if c.RequiredScope == "" {
		return errors.New("OIDC_REQUIRED_SCOPE is required when AUTH_MODE=oidc")
	}
	if strings.ContainsAny(c.RequiredScope, " \t\r\n") {
		return errors.New("OIDC_REQUIRED_SCOPE must contain exactly one scope")
	}
	if c.ExpectedTokenType == "" {
		return errors.New("OIDC_EXPECTED_TOKEN_TYPE is required when AUTH_MODE=oidc")
	}
	if len(c.SigningAlgorithms) == 0 {
		return errors.New("OIDC_SIGNING_ALGORITHMS must contain at least one allowed algorithm")
	}
	allowedAlgorithms := map[string]struct{}{
		"RS256": {}, "RS384": {}, "RS512": {},
		"PS256": {}, "PS384": {}, "PS512": {},
		"ES256": {}, "ES384": {}, "ES512": {},
		"EdDSA": {},
	}
	for _, algorithm := range c.SigningAlgorithms {
		if _, ok := allowedAlgorithms[algorithm]; !ok {
			return fmt.Errorf("OIDC_SIGNING_ALGORITHMS contains unsupported algorithm %q", algorithm)
		}
	}
	return nil
}

func splitCSV(value string) []string {
	parts := strings.Split(value, ",")
	result := make([]string, 0, len(parts))
	for _, part := range parts {
		if trimmed := strings.TrimSpace(part); trimmed != "" {
			result = append(result, trimmed)
		}
	}
	return result
}

func getEnv(key, fallback string) string {
	if v, ok := os.LookupEnv(key); ok && v != "" {
		return v
	}
	return fallback
}
