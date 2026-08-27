// Command ws-smoke valida o handshake do stream de eventos da tarefa de demonstração.
// Ele não lê nem imprime credenciais e existe apenas como ferramenta operacional local.
package main

import (
	"context"
	"flag"
	"fmt"
	"os"
	"time"

	"github.com/coder/websocket"
)

const defaultURL = "ws://localhost:8080/v1/tasks/00000000-0000-0000-0000-000000000184/events/ws"

func main() {
	url := flag.String("url", defaultURL, "WebSocket URL to validate")
	timeout := flag.Duration("timeout", 5*time.Second, "connection timeout")
	flag.Parse()

	ctx, cancel := context.WithTimeout(context.Background(), *timeout)
	defer cancel()

	conn, response, err := websocket.Dial(ctx, *url, nil)
	if err != nil {
		if response != nil {
			fmt.Fprintf(os.Stderr, "websocket handshake failed with HTTP %d: %v\n", response.StatusCode, err)
		} else {
			fmt.Fprintf(os.Stderr, "websocket connection failed: %v\n", err)
		}
		os.Exit(1)
	}

	fmt.Println("WebSocket handshake succeeded.")
	if err := conn.Close(websocket.StatusNormalClosure, "smoke test completed"); err != nil {
		fmt.Fprintf(os.Stderr, "websocket close failed: %v\n", err)
		os.Exit(1)
	}
}
