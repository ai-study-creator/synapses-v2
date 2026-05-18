# Requirements

Status: `APPROVED`

This file maps the full source behavior before continuing the migration. Source code is authoritative; prompt-only ideas are marked separately in `system-map.md` and are not migration requirements.

## Orchestrator Lifecycle

| ID | Requirement | Source evidence | Status |
|---|---|---|---|
| REQ-ORCH-LIFE-001 | The HTTP app must expose `GET /` returning `{"message": "Orchestrator Brain is running!"}`. | `orchestrator/main.py:73-75` | APPROVED |
| REQ-ORCH-LIFE-002 | On startup, the app must attempt to start and initialize the `mcp-devto` MCP client over STDIO. | `orchestrator/main.py:22-49` | APPROVED |
| REQ-ORCH-LIFE-003 | MCP subprocess environment must inherit current process environment and set `DOTENV_CONFIG_QUIET=true` when absent. | `orchestrator/main.py:31-40` | APPROVED |
| REQ-ORCH-LIFE-004 | If MCP startup fails, the app must keep running with `app.state.mcp_client = None`. | `orchestrator/main.py:50-56` | APPROVED |
| REQ-ORCH-LIFE-005 | On shutdown, the app must close the MCP client stack and clear `app.state.mcp_client`. | `orchestrator/main.py:60-65` | APPROVED |

## Configuration

| ID | Requirement | Source evidence | Status |
|---|---|---|---|
| REQ-CONFIG-001 | Settings load from `.env` and `orchestrator/.env`, ignoring extra variables. | `orchestrator/config.py:6-13` | APPROVED |
| REQ-CONFIG-002 | `SLACK_BOT_TOKEN`, `SLACK_SIGNING_SECRET`, and `LLM_API_KEY` are required by the settings model. | `orchestrator/config.py:15-17` | APPROVED |
| REQ-CONFIG-003 | `LLM_PROVIDER` defaults to `openai`. | `orchestrator/config.py:18` | APPROVED |

## Slack Events

| ID | Requirement | Source evidence | Status |
|---|---|---|---|
| REQ-SLACK-001 | `POST /slack/events` must read the raw request body and validate UTF-8. | `orchestrator/slack_app.py:166-179` | APPROVED |
| REQ-SLACK-002 | Missing or invalid Slack signature must return `403` with `Invalid Slack signature`. | `orchestrator/slack_app.py:171-186` | APPROVED |
| REQ-SLACK-003 | Slack retry requests with `x-slack-retry-num` must return `200` without processing. | `orchestrator/slack_app.py:188-192` | APPROVED |
| REQ-SLACK-004 | Invalid JSON payload must return `400` with `Invalid JSON payload`. | `orchestrator/slack_app.py:194-198` | APPROVED |
| REQ-SLACK-005 | Slack `url_verification` with challenge must return `{"challenge": challenge}`. | `orchestrator/slack_app.py:200-205` | APPROVED |
| REQ-SLACK-006 | `url_verification` without challenge must return `400` with `Missing challenge`. | `orchestrator/slack_app.py:200-205` | APPROVED |
| REQ-SLACK-007 | Duplicate events within 60 seconds must return `200` and skip processing. | `orchestrator/slack_app.py:18-54`, `orchestrator/slack_app.py:207-209` | APPROVED |
| REQ-SLACK-008 | Dedup key uses `event_id` when present, otherwise event type/channel/timestamp fallback. | `orchestrator/slack_app.py:28-37` | APPROVED |
| REQ-SLACK-009 | Payloads without an `event` must return `200`. | `orchestrator/slack_app.py:211-214` | APPROVED |
| REQ-SLACK-010 | `app_mention` events from bots/self must return `200` and skip processing. | `orchestrator/slack_app.py:57-62`, `orchestrator/slack_app.py:216-219` | APPROVED |
| REQ-SLACK-011 | Non-bot `app_mention` events must schedule asynchronous processing and immediately return `200`. | `orchestrator/slack_app.py:216-223` | APPROVED |
| REQ-SLACK-012 | Non-`app_mention` events must return `200`. | `orchestrator/slack_app.py:225` | APPROVED |

## Slack App Mention Processing

| ID | Requirement | Source evidence | Status |
|---|---|---|---|
| REQ-MENTION-001 | Processing must ignore bot/self events. | `orchestrator/slack_app.py:103-112` | APPROVED |
| REQ-MENTION-002 | Reply channel is `event["channel"]`; reply thread is `event.thread_ts` or `event.ts`. | `orchestrator/slack_app.py:114-116`, `orchestrator/slack_app.py:154-158` | APPROVED |
| REQ-MENTION-003 | If MCP client is unavailable, post `MCP client is not available. Please check orchestrator startup logs.` to the Slack thread. | `orchestrator/slack_app.py:118-124` | APPROVED |
| REQ-MENTION-004 | The LLM prompt for Slack app mentions uses the `devto` persona and the Slack event text. | `orchestrator/slack_app.py:126-130` | APPROVED |
| REQ-MENTION-005 | Available MCP tools must be converted to OpenAI tool schema and passed to the LLM with `tool_choice="auto"`. | `orchestrator/slack_app.py:65-79`, `orchestrator/slack_app.py:131-137` | APPROVED |
| REQ-MENTION-006 | For OpenAI provider responses, tool calls are executed via MCP and content is used as fallback output. | `orchestrator/slack_app.py:139-145` | APPROVED |
| REQ-MENTION-007 | For non-OpenAI providers, Slack output is `Configured LLM provider does not support this tool-calling flow yet. Use `LLM_PROVIDER=openai`.` | `orchestrator/slack_app.py:146-147` | APPROVED |
| REQ-MENTION-008 | Tool results replace LLM text output when present. | `orchestrator/slack_app.py:149-150` | APPROVED |
| REQ-MENTION-009 | Empty final output must become `No response was generated.` | `orchestrator/slack_app.py:151-152` | APPROVED |
| REQ-MENTION-010 | Final output is posted to Slack thread with `chat_postMessage`. | `orchestrator/slack_app.py:154-158` | APPROVED |
| REQ-MENTION-011 | Slack API and unexpected processing errors are logged and not rethrown. | `orchestrator/slack_app.py:160-163` | APPROVED |

## MCP Tool Use From Orchestrator

| ID | Requirement | Source evidence | Status |
|---|---|---|---|
| REQ-MCP-CLIENT-001 | MCP tool list converts each tool into OpenAI function schema with name, description, and input schema. | `orchestrator/slack_app.py:65-79` | APPROVED |
| REQ-MCP-CLIENT-002 | Tool call arguments are parsed from JSON strings; invalid JSON tool calls are skipped and logged. | `orchestrator/slack_app.py:82-96` | APPROVED |
| REQ-MCP-CLIENT-003 | Each valid tool call invokes `mcp_client.call_tool(function_name, arguments=function_args)`. | `orchestrator/slack_app.py:87-95` | APPROVED |
| REQ-MCP-CLIENT-004 | Tool call result text uses `Executed `<function_name>` successfully: <tool_result>`. | `orchestrator/slack_app.py:94-95` | APPROVED |

## LLM Client

| ID | Requirement | Source evidence | Status |
|---|---|---|---|
| REQ-LLM-001 | Supported providers are `openai` and `github_models`; aliases `github` and `github-models` normalize to `github_models`. | `orchestrator/llm_utils.py:114-123` | APPROVED |
| REQ-LLM-002 | Missing API key after provider fallback raises `Missing API key for configured LLM provider.` | `orchestrator/llm_utils.py:64-81` | APPROVED |
| REQ-LLM-003 | OpenAI fallback key is `OPENAI_API_KEY` when configured API key is empty. | `orchestrator/llm_utils.py:69-70` | APPROVED |
| REQ-LLM-004 | GitHub Models fallback keys are `GITHUB_MODELS_TOKEN`, `GITHUB_TOKEN`, then `GH_TOKEN`. | `orchestrator/llm_utils.py:72-78` | APPROVED |
| REQ-LLM-005 | Default model uses `AI_MODEL` if set; otherwise OpenAI defaults to `gpt-4.1-mini`. | `orchestrator/llm_utils.py:125-132` | APPROVED |
| REQ-LLM-006 | GitHub Models default model is `openai/gpt-4.1`, overridable by `GITHUB_MODELS_MODEL`. | `orchestrator/llm_utils.py:133` | APPROVED |
| REQ-LLM-007 | OpenAI requests go to `https://api.openai.com/v1/chat/completions` with bearer authorization and JSON content type. | `orchestrator/llm_utils.py:13`, `orchestrator/llm_utils.py:135-140` | APPROVED |
| REQ-LLM-008 | GitHub Models requests use `GITHUB_MODELS_URL` or default URL, bearer authorization, JSON content type, GitHub accept header, and API version header default `2022-11-28`. | `orchestrator/llm_utils.py:14`, `orchestrator/llm_utils.py:142-148` | APPROVED |
| REQ-LLM-009 | Chat payload includes model, messages, temperature, optional tools, and optional tool choice. | `orchestrator/llm_utils.py:83-112` | APPROVED |
| REQ-LLM-010 | LLM requests retry up to 3 attempts for request exceptions and transient HTTP statuses `{408,409,429,500,502,503,504}` with `0.5 * attempt` sleep. | `orchestrator/llm_utils.py:151-185` | APPROVED |
| REQ-LLM-011 | Non-2xx non-retried LLM responses raise `LLM API error <status>: <sanitized body>`, with body flattened and truncated to 1000 chars. | `orchestrator/llm_utils.py:182-185`, `orchestrator/llm_utils.py:306-310` | APPROVED |
| REQ-LLM-012 | Invalid JSON success response raises `LLM provider returned invalid JSON.` | `orchestrator/llm_utils.py:172-176` | APPROVED |
| REQ-LLM-013 | Parsed LLM response must contain non-empty choices, message, and either content or tool calls. | `orchestrator/llm_utils.py:187-208` | APPROVED |
| REQ-LLM-014 | Message validation requires non-empty list, role in `{system,user,assistant,tool}`, and non-empty content except for tool role. | `orchestrator/llm_utils.py:210-228` | APPROVED |
| REQ-LLM-015 | Text content extraction supports string content and list items with text fields. | `orchestrator/llm_utils.py:253-266` | APPROVED |
| REQ-LLM-016 | Tool call extraction supports function name and arguments as dict, string, or `{}` fallback. | `orchestrator/llm_utils.py:269-303` | APPROVED |

## Persona Prompts

| ID | Requirement | Source evidence | Status |
|---|---|---|---|
| REQ-PERSONA-001 | `devto`, `twitter`, and `slack_summary` persona prompts are defined. | `orchestrator/llm_utils.py:17-31` | APPROVED |
| REQ-PERSONA-002 | Unknown persona context raises `Unknown persona context: <context>`. | `orchestrator/llm_utils.py:234-238` | APPROVED |
| REQ-PERSONA-003 | Prompt builder uses profile override when provided, otherwise persona prompt, then user prompt. | `orchestrator/llm_utils.py:241-250` | APPROVED |

## MCP Dev.to

See detailed approved slice file: `migration/output/mcp-devto.md`.

| ID | Requirement | Source evidence | Status |
|---|---|---|---|
| REQ-MCP-DEVTO-001..007 | Preserve approved MCP Dev.to behavior. | `migration/output/mcp-devto.md` | APPROVED |

## Runtime And Containers

| ID | Requirement | Source evidence | Status |
|---|---|---|---|
| REQ-RUNTIME-001 | Source orchestrator container exposes port `8000` and runs Uvicorn on host `0.0.0.0`, port `8000`. | `Dockerfile:23-34`, `docker-compose.yml:25` | APPROVED |
| REQ-RUNTIME-002 | Source compose maps host port `8000` to container port `8000`. | `docker-compose.yml:8-9` | APPROVED |
| REQ-RUNTIME-003 | Source compose builds `mcp-devto` but runs it with command `true`; orchestrator starts the MCP subprocess. | `docker-compose.yml:22-34`, `orchestrator/main.py:35-47` | APPROVED |

## Not Source Requirements

These appear in initial prompt files but are not implemented in current source code:

- GitHub daily cron trigger.
- APScheduler dependency.
- Anthropic provider.
- Slack app mention flow without MCP tools.
