# Slice: mention-processing

Status: `IMPLEMENTED`

## Source Evidence

- `orchestrator/slack_app.py:57-62`: bot/self event detection.
- `orchestrator/slack_app.py:65-79`: MCP tools convert to OpenAI function schemas.
- `orchestrator/slack_app.py:82-96`: tool calls parse JSON arguments, skip invalid JSON, call MCP tools, and format results.
- `orchestrator/slack_app.py:99-163`: Slack app mention processing flow.

## Requirements

- `REQ-MENTION-001` through `REQ-MENTION-011`.
- `REQ-MCP-CLIENT-001` through `REQ-MCP-CLIENT-004`.

## Tests

- `TEST-MENTION-001`: bot/self events are ignored.
- `TEST-MENTION-002`: missing MCP client posts the source unavailable message using `thread_ts`.
- `TEST-MENTION-003`: missing `thread_ts` falls back to event `ts`.
- `TEST-MENTION-004`: `devto` persona, event text, MCP tool schema, and `tool_choice=auto` are sent to LLM.
- `TEST-MENTION-005`: OpenAI tool calls execute through MCP and tool results replace LLM text.
- `TEST-MENTION-006`: non-OpenAI provider returns the source tool-calling warning.
- `TEST-MENTION-007`: empty final output becomes `No response was generated.`
- `TEST-MCP-CLIENT-002`: invalid JSON tool arguments are skipped.
- `TEST-MENTION-008`: Slack API and unexpected processing errors are swallowed.

## Result

- Implemented: Slack app mention processor with injectable Slack, LLM, and MCP clients.
- Implemented: Slack Java SDK `chat.postMessage` adapter.
- Verified: `./gradlew :services:orchestrator:test`.
- Differences: None intended.
