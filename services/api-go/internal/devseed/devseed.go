// Package devseed centraliza os IDs fixos da história de exemplo (bug #184), compartilhados
// entre cmd/seed (que insere as linhas) e o simulador de eventos de internal/events (que avança
// a tarefa #184 periodicamente enquanto não existe orquestrador real publicando eventos).
package devseed

import "github.com/google/uuid"

var (
	Task184ID = uuid.MustParse("00000000-0000-0000-0000-000000000184")

	AgentDevID    = uuid.MustParse("00000000-0000-0000-0000-0000000a0001")
	AgentQAID     = uuid.MustParse("00000000-0000-0000-0000-0000000a0002")
	AgentDevOpsID = uuid.MustParse("00000000-0000-0000-0000-0000000a0003")

	DeviceWorkstation01ID = uuid.MustParse("00000000-0000-0000-0000-0000000d0001")
	DeviceServerAtlasID   = uuid.MustParse("00000000-0000-0000-0000-0000000d0002")
	DeviceServerCIID      = uuid.MustParse("00000000-0000-0000-0000-0000000d0003")
	DeviceWorkstation02ID = uuid.MustParse("00000000-0000-0000-0000-0000000d0004")
)
