# Slice: config

Status: `APPROVED`

## Source Evidence

- `orchestrator/config.py:6-13`: settings read `.env` and `orchestrator/.env`, ignoring extra variables.
- `orchestrator/config.py:15-17`: `SLACK_BOT_TOKEN`, `SLACK_SIGNING_SECRET`, and `LLM_API_KEY` are required.
- `orchestrator/config.py:18`: `LLM_PROVIDER` defaults to `openai`.

## Requirements

- `REQ-CONFIG-001`: Settings load from `.env` and `orchestrator/.env`, ignoring extra variables.
- `REQ-CONFIG-002`: `SLACK_BOT_TOKEN`, `SLACK_SIGNING_SECRET`, and `LLM_API_KEY` are required.
- `REQ-CONFIG-003`: `LLM_PROVIDER` defaults to `openai`.

## Tests

- `TEST-CONFIG-001` -> `REQ-CONFIG-001`: loads root and orchestrator dotenv files and ignores extra variables.
- `TEST-CONFIG-002` -> `REQ-CONFIG-001`: environment variables override dotenv values.
- `TEST-CONFIG-003` -> `REQ-CONFIG-002`: missing required values fail config loading.
- `TEST-CONFIG-004` -> `REQ-CONFIG-003`: `LLM_PROVIDER` defaults to `openai`.

## Decisions / Open Points

- Preserve `.env` loading behavior even though it is not the ideal Kotlin deployment style.

## Result

- Implemented: `services/orchestrator` module with `AppConfig` and `AppConfigLoader`.
- Verified: `./gradlew :services:orchestrator:test` and `./gradlew clean build`.
- Differences: None intended.
