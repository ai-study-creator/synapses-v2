# Migration Plan

Status: `APPROVED`

This plan is based on the full source map in `system-map.md` and requirements in `requirements.md`.

## Dependency Order

1. `mcp-devto` independent STDIO service.
2. Shared target build/runtime conventions.
3. Orchestrator configuration.
4. LLM client.
5. Orchestrator MCP client lifecycle.
6. Slack signature and event endpoint.
7. Slack app mention processing.
8. Container/compose runtime.
9. End-to-end behavior comparison.

## Implementation Slices

| Order | Slice | Includes | Depends on | Primary tests |
|---:|---|---|---|---|
| 1 | `mcp-devto` | Approved MCP Dev.to service requirements. | None | MCP contract, Dev.to payload, subprocess startup |
| 2 | `config` | Environment loading and target config model. | Build setup | Unit tests for required/default/fallback config |
| 3 | `llm-client` | Provider normalization, model selection, request config, retries, response parsing. | Config | Unit tests with stub HTTP client |
| 4 | `orchestrator-mcp-lifecycle` | Ktor app startup/shutdown and MCP subprocess session ownership. | `mcp-devto`, Config | Integration tests with fake/built MCP subprocess |
| 5 | `slack-events` | `POST /slack/events`, signature verification, retries, JSON/UTF-8 handling, URL verification, dedup, event routing. | Config | Ktor route tests |
| 6 | `mention-processing` | Slack app mention async processing, persona, tool schema conversion, LLM call, MCP tool execution, Slack replies. | LLM client, MCP lifecycle, Slack events | Service tests with fake Slack/MCP/LLM clients |
| 7 | `runtime` | Dockerfile, compose, port/env/service topology. | All modules | Build/smoke tests |
| 8 | `behavior-comparison` | Source vs target selected scenarios. | All slices | Manual/golden behavior comparison |

## Current Status

| Slice | Status |
|---|---|
| `mcp-devto` | Implemented and tested in target; full MCP client subprocess list-tools comparison still previously noted as pending, but user accepted moving on. |
| `config` | Implemented and tested. |
| `llm-client` | Implemented, tested, and approved. |
| `orchestrator-mcp-lifecycle` | Implemented and tested. |
| `slack-events` | Implemented and tested. |
| `mention-processing` | Implemented and tested. |
| `runtime` | Implemented and smoke tested. |
| `behavior-comparison` | Completed for migrated code; real external provider calls require credentials. |

## Test Strategy

| Area | Test type |
|---|---|
| Config | Unit |
| LLM client | Unit with fake HTTP responses |
| Ktor routes | Route/integration tests |
| Slack signature | Contract/unit tests using known signing inputs |
| Dedup | Unit tests with controllable clock if target design permits |
| MCP lifecycle | Integration tests with subprocess/fake MCP server |
| Mention processing | Service tests with fake Slack, LLM, and MCP clients |
| Runtime | Gradle build, Docker build, container smoke |

## Resolved Planning Decisions

- Preserve source Slack app mention behavior exactly: `devto` persona plus MCP tools.
- Ignore prompt-folder behavior when it conflicts with current source code.
- Preserve source `.env` loading behavior in the target.

## Recommended Next Slice

Migration code is complete. Remaining work is credentialed external validation with real Slack, LLM, and Dev.to accounts.
