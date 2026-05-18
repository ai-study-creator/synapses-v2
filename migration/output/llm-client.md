# Slice: llm-client

Status: `APPROVED`

## Source Evidence

- `orchestrator/llm_utils.py:64-81`: API key resolution and missing-key error.
- `orchestrator/llm_utils.py:83-185`: chat payload, provider config, retries, provider errors, invalid JSON.
- `orchestrator/llm_utils.py:187-310`: response parsing, message validation, body sanitizing.
- `orchestrator/llm_utils.py:17-31`, `234-250`: persona prompts and prompt builder.

## Requirements

- `REQ-LLM-001` through `REQ-LLM-016`.
- `REQ-PERSONA-001` through `REQ-PERSONA-003`.

## Tests

- `TEST-LLM-001`: provider normalization and API key fallbacks.
- `TEST-LLM-002`: default model and request config selection.
- `TEST-LLM-003`: message validation and chat payload shape.
- `TEST-LLM-004`: transient retries, provider errors, invalid JSON.
- `TEST-LLM-005`: response text/tool-call parsing and body sanitizing.
- `TEST-PERSONA-001`: known personas and prompt builder.
- `TEST-PERSONA-002`: unknown persona rejection.

## Result

- Implemented: `services/orchestrator/src/main/kotlin/com/synapses/orchestrator/llm`.
- Verified: `./gradlew :services:orchestrator:test`.
- Differences: None intended.
