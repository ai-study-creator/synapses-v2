# Requirement Test Matrix

Use this file to track whether every approved requirement has test coverage.

| Requirement ID | Status | Test ID | Test Type | Scenario | Source Evidence | Result |
|---|---|---|---|---|---|---|
| REQ-MCP-DEVTO-001 | APPROVED | TEST-MCP-DEVTO-001 | Integration | Server starts with valid `DEVTO_API_KEY` over STDIO | `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:47-50`, `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:84-93` | Passed by subprocess smoke check: valid startup writes `MCP Dev.to Server Started` to stderr and `0` bytes to stdout. |
| REQ-MCP-DEVTO-002 | APPROVED | TEST-MCP-DEVTO-002 | Integration | Missing `DEVTO_API_KEY` fails startup | `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:7-14`, `/home/leonardo/workspaces/synapses/mcp-devto/.env.example:1` | Passed by subprocess smoke check: exits `1` and writes missing-key message. |
| REQ-MCP-DEVTO-003 | APPROVED | TEST-MCP-DEVTO-003 | Contract | Tool schema is listed through MCP | `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:52-62` | Passed by automated target test `CreateDevPostToolTest`. |
| REQ-MCP-DEVTO-003 | APPROVED | TEST-MCP-DEVTO-004 | Contract | Invalid tool input is rejected before Dev.to API call | `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:56-60` | Passed by automated target test `CreateDevPostToolTest`. |
| REQ-MCP-DEVTO-004 | APPROVED | TEST-MCP-DEVTO-005 | Integration | Valid tool call sends Dev.to draft payload | `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:16-35` | Passed by automated target test `DevToClientTest`. |
| REQ-MCP-DEVTO-005 | APPROVED | TEST-MCP-DEVTO-006 | Contract | Successful Dev.to response returns MCP success content | `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:63-70` | Passed by automated target test `CreateDevPostToolTest`. |
| REQ-MCP-DEVTO-006 | APPROVED | TEST-MCP-DEVTO-007 | Contract | Dev.to non-OK response returns MCP error content | `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:71-80` | Passed by automated target test `DevToClientTest`. |
| REQ-MCP-DEVTO-007 | APPROVED | TEST-MCP-DEVTO-008 | Integration | Built MCP server runs as a separate STDIO subprocess | `/home/leonardo/workspaces/synapses/mcp-devto/Dockerfile:2-20`, `/home/leonardo/workspaces/synapses/orchestrator/main.py:31-47` | Partial: `installDist` and subprocess startup checks pass; full MCP client initialize/list-tools check still pending. |
| REQ-CONFIG-001 | APPROVED | TEST-CONFIG-001 | Unit | Load root `.env` and `orchestrator/.env`, ignore extras, later file overrides earlier file | `/home/leonardo/workspaces/synapses/orchestrator/config.py:6-13` | Passed by automated target test `AppConfigLoaderTest`. |
| REQ-CONFIG-001 | APPROVED | TEST-CONFIG-002 | Unit | Environment values override dotenv values | `/home/leonardo/workspaces/synapses/orchestrator/config.py:6-13` | Passed by automated target test `AppConfigLoaderTest`. |
| REQ-CONFIG-002 | APPROVED | TEST-CONFIG-003 | Unit | Missing required Slack/LLM settings fail config loading | `/home/leonardo/workspaces/synapses/orchestrator/config.py:15-17` | Passed by automated target test `AppConfigLoaderTest`. |
| REQ-CONFIG-003 | APPROVED | TEST-CONFIG-004 | Unit | `LLM_PROVIDER` defaults to `openai` | `/home/leonardo/workspaces/synapses/orchestrator/config.py:18` | Passed by automated target test `AppConfigLoaderTest`. |
| REQ-LLM-001 | APPROVED | TEST-LLM-001 | Unit | Provider values and aliases normalize to source-supported providers | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:114-123` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-002 | APPROVED | TEST-LLM-001 | Unit | Missing API key after fallback raises the source error | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:64-81` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-003 | APPROVED | TEST-LLM-001 | Unit | OpenAI uses `OPENAI_API_KEY` fallback | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:69-70` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-004 | APPROVED | TEST-LLM-001 | Unit | GitHub Models fallback key order is preserved | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:72-78` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-005 | APPROVED | TEST-LLM-002 | Unit | `AI_MODEL` wins; OpenAI default is `gpt-4.1-mini` | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:125-132` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-006 | APPROVED | TEST-LLM-002 | Unit | GitHub Models default and override are preserved | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:133` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-007 | APPROVED | TEST-LLM-002 | Unit | OpenAI URL and headers are preserved | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:13`, `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:135-140` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-008 | APPROVED | TEST-LLM-002 | Unit | GitHub Models URL and headers are preserved | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:14`, `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:142-148` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-009 | APPROVED | TEST-LLM-003 | Unit | Chat payload includes model, messages, temperature, tools, and tool choice | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:83-112` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-010 | APPROVED | TEST-LLM-004 | Unit | Transient responses retry up to 3 attempts with source backoff | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:151-185` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-011 | APPROVED | TEST-LLM-004 | Unit | Non-retried provider errors include sanitized body | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:182-185`, `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:306-310` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-012 | APPROVED | TEST-LLM-004 | Unit | Invalid JSON success response raises source error | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:172-176` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-013 | APPROVED | TEST-LLM-005 | Unit | Missing choices/message/output is rejected | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:187-208` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-014 | APPROVED | TEST-LLM-003 | Unit | Message validation preserves source rules | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:210-228` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-015 | APPROVED | TEST-LLM-005 | Unit | String and list text content extraction are preserved | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:253-266` | Passed by automated target test `LlmClientTest`. |
| REQ-LLM-016 | APPROVED | TEST-LLM-005 | Unit | Tool-call name and argument parsing are preserved | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:269-303` | Passed by automated target test `LlmClientTest`. |
| REQ-PERSONA-001 | APPROVED | TEST-PERSONA-001 | Unit | `devto`, `twitter`, and `slack_summary` prompts are available | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:17-31` | Passed by automated target test `PersonaPromptsTest`. |
| REQ-PERSONA-002 | APPROVED | TEST-PERSONA-002 | Unit | Unknown persona context raises source error | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:234-238` | Passed by automated target test `PersonaPromptsTest`. |
| REQ-PERSONA-003 | APPROVED | TEST-PERSONA-001 | Unit | Prompt builder uses override or persona plus user prompt | `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py:241-250` | Passed by automated target test `PersonaPromptsTest`. |
| REQ-ORCH-LIFE-001 | APPROVED | TEST-ORCH-LIFE-001 | Integration | Ktor root route returns source health payload | `/home/leonardo/workspaces/synapses/orchestrator/main.py:73-75` | Passed by automated target test `OrchestratorRuntimeTest`. |
| REQ-ORCH-LIFE-002 | APPROVED | TEST-ORCH-LIFE-002 | Unit | Runtime startup invokes MCP factory | `/home/leonardo/workspaces/synapses/orchestrator/main.py:22-49` | Passed by automated target test `OrchestratorRuntimeTest`. |
| REQ-ORCH-LIFE-003 | APPROVED | TEST-ORCH-LIFE-002 | Unit | MCP subprocess environment inherits values and defaults `DOTENV_CONFIG_QUIET=true` | `/home/leonardo/workspaces/synapses/orchestrator/main.py:31-40` | Passed by automated target test `OrchestratorRuntimeTest`. |
| REQ-ORCH-LIFE-004 | APPROVED | TEST-ORCH-LIFE-003 | Unit | MCP startup failure leaves runtime without an MCP client | `/home/leonardo/workspaces/synapses/orchestrator/main.py:50-56` | Passed by automated target test `OrchestratorRuntimeTest`. |
| REQ-ORCH-LIFE-005 | APPROVED | TEST-ORCH-LIFE-004 | Unit | Shutdown closes MCP client and clears runtime state | `/home/leonardo/workspaces/synapses/orchestrator/main.py:60-65` | Passed by automated target test `OrchestratorRuntimeTest`. |
| REQ-SLACK-001 | APPROVED | TEST-SLACK-001 | Integration | Invalid UTF-8 raw request body returns source error | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:166-179` | Passed by automated target test `SlackEventsTest`. |
| REQ-SLACK-002 | APPROVED | TEST-SLACK-001 | Integration | Missing or invalid Slack signature returns `403` | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:171-186` | Passed by automated target test `SlackEventsTest`. |
| REQ-SLACK-003 | APPROVED | TEST-SLACK-002 | Integration | Slack retry header returns `200` without processing | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:188-192` | Passed by automated target test `SlackEventsTest`. |
| REQ-SLACK-004 | APPROVED | TEST-SLACK-003 | Integration | Invalid JSON returns `400` | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:194-198` | Passed by automated target test `SlackEventsTest`. |
| REQ-SLACK-005 | APPROVED | TEST-SLACK-004 | Integration | URL verification challenge returns challenge payload | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:200-205` | Passed by automated target test `SlackEventsTest`. |
| REQ-SLACK-006 | APPROVED | TEST-SLACK-004 | Integration | URL verification without challenge returns `400` | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:200-205` | Passed by automated target test `SlackEventsTest`. |
| REQ-SLACK-007 | APPROVED | TEST-SLACK-005 | Unit | Duplicate events within 60 seconds are skipped | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:18-54`, `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:207-209` | Passed by automated target test `SlackEventsTest`. |
| REQ-SLACK-008 | APPROVED | TEST-SLACK-005 | Unit | Dedup key uses event ID or event type/channel/timestamp fallback | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:28-37` | Passed by automated target test `SlackEventsTest`. |
| REQ-SLACK-009 | APPROVED | TEST-SLACK-006 | Integration | Payload without `event` returns `200` | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:211-214` | Passed by automated target test `SlackEventsTest`. |
| REQ-SLACK-010 | APPROVED | TEST-SLACK-007 | Integration | Bot/self app mention events return `200` and skip processing | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:57-62`, `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:216-219` | Passed by automated target test `SlackEventsTest`. |
| REQ-SLACK-011 | APPROVED | TEST-SLACK-008 | Integration | User app mention events enqueue processing and return `200` | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:216-223` | Passed by automated target test `SlackEventsTest`. |
| REQ-SLACK-012 | APPROVED | TEST-SLACK-006 | Integration | Non-app-mention events return `200` | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:225` | Passed by automated target test `SlackEventsTest`. |
| REQ-MENTION-001 | APPROVED | TEST-MENTION-001 | Unit | Mention processing ignores bot/self events | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:103-112` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MENTION-002 | APPROVED | TEST-MENTION-002 | Unit | Reply uses event channel and `thread_ts` when present | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:114-116`, `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:154-158` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MENTION-002 | APPROVED | TEST-MENTION-003 | Unit | Reply falls back to event `ts` when `thread_ts` is missing | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:114-116`, `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:154-158` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MENTION-003 | APPROVED | TEST-MENTION-002 | Unit | Missing MCP client posts source unavailable message | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:118-124` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MENTION-004 | APPROVED | TEST-MENTION-004 | Unit | LLM prompt uses `devto` persona and event text | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:126-130` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MENTION-005 | APPROVED | TEST-MENTION-004 | Unit | MCP tools convert to OpenAI tool schema and are passed with `tool_choice=auto` | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:65-79`, `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:131-137` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MENTION-006 | APPROVED | TEST-MENTION-005 | Unit | OpenAI provider tool calls execute via MCP with content fallback | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:139-145` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MENTION-007 | APPROVED | TEST-MENTION-006 | Unit | Non-OpenAI provider returns source tool-calling warning | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:146-147` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MENTION-008 | APPROVED | TEST-MENTION-005 | Unit | Tool results replace LLM text output | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:149-150` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MENTION-009 | APPROVED | TEST-MENTION-007 | Unit | Empty final output becomes `No response was generated.` | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:151-152` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MENTION-010 | APPROVED | TEST-MENTION-005 | Unit | Final output is posted to Slack thread | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:154-158` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MENTION-011 | APPROVED | TEST-MENTION-008 | Unit | Slack API and unexpected processing errors are not rethrown | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:160-163` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MCP-CLIENT-001 | APPROVED | TEST-MENTION-004 | Unit | MCP tool list converts to OpenAI function schema | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:65-79` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MCP-CLIENT-002 | APPROVED | TEST-MCP-CLIENT-002 | Unit | Invalid JSON tool arguments are skipped | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:82-96` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MCP-CLIENT-003 | APPROVED | TEST-MENTION-005 | Unit | Valid tool call invokes MCP `callTool` | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:87-95` | Passed by automated target test `MentionProcessingTest`. |
| REQ-MCP-CLIENT-004 | APPROVED | TEST-MENTION-005 | Unit | Tool result text uses source format | `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py:94-95` | Passed by automated target test `MentionProcessingTest`. |
| REQ-RUNTIME-001 | APPROVED | TEST-RUNTIME-002 | Integration | Installed Ktor app starts on port `8000` and returns source health payload | `/home/leonardo/workspaces/synapses/Dockerfile:23-34`, `/home/leonardo/workspaces/synapses/docker-compose.yml:25` | Passed by local runtime smoke on `http://127.0.0.1:8000/`. |
| REQ-RUNTIME-001 | APPROVED | TEST-RUNTIME-004 | Integration | Containerized app starts and returns source health payload | `/home/leonardo/workspaces/synapses/Dockerfile:23-34`, `/home/leonardo/workspaces/synapses/docker-compose.yml:25` | Passed by container smoke on `http://127.0.0.1:8001/`. |
| REQ-RUNTIME-002 | APPROVED | TEST-RUNTIME-003 | Manual validation | Compose maps host `8000` to container `8000` | `/home/leonardo/workspaces/synapses/docker-compose.yml:8-9` | Passed by target `docker-compose.yml` file check and Docker image build. |
| REQ-RUNTIME-003 | APPROVED | TEST-RUNTIME-001 | Integration | Gradle builds install distributions for both orchestrator and `mcp-devto` | `/home/leonardo/workspaces/synapses/docker-compose.yml:22-34`, `/home/leonardo/workspaces/synapses/orchestrator/main.py:35-47` | Passed by `./gradlew clean build :services:mcp-devto:installDist :services:orchestrator:installDist`. |

## Test types

Use one of:

- Acceptance
- Contract
- Integration
- Unit
- Golden master
- Manual validation

## Rule

Every `APPROVED` requirement must have at least one mapped test before implementation is accepted.
