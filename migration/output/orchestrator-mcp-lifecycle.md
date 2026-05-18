# Slice: orchestrator-mcp-lifecycle

Status: `IMPLEMENTED`

## Source Evidence

- `orchestrator/main.py:18-20`: MCP client and lifecycle stack are held for app lifetime.
- `orchestrator/main.py:22-49`: startup attempts STDIO MCP initialization.
- `orchestrator/main.py:31-40`: subprocess environment inherits process environment and sets `DOTENV_CONFIG_QUIET=true` when absent.
- `orchestrator/main.py:50-56`: startup failure keeps the HTTP app running with `mcp_client = None`.
- `orchestrator/main.py:60-65`: shutdown closes the lifecycle stack and clears `mcp_client`.
- `orchestrator/main.py:73-75`: `GET /` returns the health message.

## Requirements

- `REQ-ORCH-LIFE-001`: expose `GET /` returning `{"message": "Orchestrator Brain is running!"}`.
- `REQ-ORCH-LIFE-002`: on startup, attempt to start and initialize the `mcp-devto` MCP client over STDIO.
- `REQ-ORCH-LIFE-003`: subprocess environment inherits current process environment and sets `DOTENV_CONFIG_QUIET=true` when absent.
- `REQ-ORCH-LIFE-004`: if MCP startup fails, the app keeps running with `app.state.mcp_client = None`.
- `REQ-ORCH-LIFE-005`: on shutdown, close the MCP client stack and clear `app.state.mcp_client`.

## Tests

- `TEST-ORCH-LIFE-001` -> `REQ-ORCH-LIFE-001`: Ktor root route returns the source health payload.
- `TEST-ORCH-LIFE-002` -> `REQ-ORCH-LIFE-002`, `REQ-ORCH-LIFE-003`: startup invokes an MCP session factory with inherited environment and quiet dotenv default.
- `TEST-ORCH-LIFE-003` -> `REQ-ORCH-LIFE-004`: MCP startup failure does not fail app startup and stores no client.
- `TEST-ORCH-LIFE-004` -> `REQ-ORCH-LIFE-005`: shutdown closes the managed MCP lifecycle resource and clears state.

## Target Shape

- Add a small Ktor application module/factory in `services/orchestrator`.
- Keep MCP lifecycle behind an injectable interface so tests do not need a real subprocess for failure and shutdown checks.
- Use the built `:services:mcp-devto` artifact as the real STDIO server path when wiring production runtime.

## Decisions / Open Points

- Source code remains the source of truth.
- The exact target artifact path can follow the Gradle multi-module layout as long as the lifecycle behavior is preserved.

## Result

- Implemented: Ktor root route and injectable MCP runtime lifecycle.
- Verified: `./gradlew :services:orchestrator:test`.
- Differences: None intended.
