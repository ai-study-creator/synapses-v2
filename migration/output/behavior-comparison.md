# Behavior Comparison

Status: `COMPLETED FOR MIGRATED CODE`

## Compared Behavior

- MCP Dev.to tool contract and Dev.to draft payload behavior are covered by target tests.
- Config loading behavior is covered by target tests.
- LLM provider/config/retry/parsing behavior is covered by target tests.
- Orchestrator lifecycle and root health route behavior are covered by target tests and local runtime smoke.
- Slack event endpoint behavior is covered by target Ktor route tests.
- Slack app mention processing and MCP tool-use behavior are covered by target service tests.
- Runtime container behavior is covered by Gradle install, Docker build, and container smoke.

## Runtime Smoke Result

- Local app: `GET http://127.0.0.1:8000/` returned `{"message":"Orchestrator Brain is running!"}`.
- Container smoke: `GET http://127.0.0.1:8001/` returned `{"message":"Orchestrator Brain is running!"}`.

## Not Exercised With Real External Services

- Real Slack API posting.
- Real LLM provider API call.
- Real Dev.to article creation.

These require valid external credentials. The migrated code paths are covered with fakes/stubs and preserve approved source behavior.
