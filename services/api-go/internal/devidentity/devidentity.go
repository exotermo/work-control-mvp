// Package devidentity guarda os IDs fixos do usuário/workspace/projeto de desenvolvimento.
//
// AUTH_MODE=dev trata toda request como vinda deste usuário/workspace. O seed também liga o
// usuário local do Keycloak por (issuer, subject), permitindo validar AUTH_MODE=oidc sem usar
// e-mail como identidade. Os valores são públicos e exclusivos da baseline local.
package devidentity

import "github.com/google/uuid"

var (
	DevUserID            = uuid.MustParse("00000000-0000-0000-0000-000000000001")
	DevWorkspaceID       = uuid.MustParse("00000000-0000-0000-0000-000000000002")
	DevProjectID         = uuid.MustParse("00000000-0000-0000-0000-000000000003")
	KeycloakLocalSubject = uuid.MustParse("10000000-0000-0000-0000-000000000001")
)

const KeycloakLocalIssuer = "http://localhost:8180/realms/work-control-local"
