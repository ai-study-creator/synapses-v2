# Migration Log

Use this file to record migration progress.

## 2026-05-17

### Migration slice
Feature/use case: `mcp-devto` module pilot.

### Source evidence reviewed
- `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts`
- `/home/leonardo/workspaces/synapses/mcp-devto/package.json`
- `/home/leonardo/workspaces/synapses/mcp-devto/tsconfig.json`
- `/home/leonardo/workspaces/synapses/mcp-devto/Dockerfile`
- `/home/leonardo/workspaces/synapses/mcp-devto/.env.example`
- `/home/leonardo/workspaces/synapses/orchestrator/main.py`
- `/home/leonardo/workspaces/synapses/README.md`
- `/home/leonardo/workspaces/synapses/tech-stack.md`

### Requirements created or updated
- `migration/output/mcp-devto.md`

### Tests created or updated
- Test scenarios drafted in `migration/output/mcp-devto.md`.
- No target tests implemented yet.

### Implementation summary
- No implementation started.

### Behavior comparison
- Match: Not run yet.
- Differences: None identified yet.

### Unknowns or risks
- Tracked in `migration/open-points.md`.

### Approved deviations
- None.

### PR
- Link: Not created.

### Decision
- Continue after spec review.

## 2026-05-18

### Migration slice
Feature/use case: `mcp-devto` module pilot.

### Source evidence reviewed
- No new source evidence reviewed.

### Requirements created or updated
- `migration/output/mcp-devto.md` marked `APPROVED`.
- Validation and Gradle module decisions incorporated into the approved spec.

### Tests created or updated
- `migration/requirement-test-matrix.md` updated from `DRAFT` to `APPROVED` for `REQ-MCP-DEVTO-001` through `REQ-MCP-DEVTO-007`.
- Target tests not implemented yet.

### Implementation summary
- No implementation started.

### Behavior comparison
- Match: Not run yet.
- Differences: Undefined validation/error details accepted for target project-wide behavior.

### Unknowns or risks
- `UNK-MCP-DEVTO-001`, `UNK-MCP-DEVTO-002`, and `UNK-MCP-DEVTO-003` resolved.
- `RISK-MCP-DEVTO-001` accepted with target automated test requirement.
- `RISK-MCP-DEVTO-002` accepted as non-blocking.

### Approved deviations
- None.

### PR
- Link: Not created.

### Decision
- Continue to target test/project setup for the approved `mcp-devto` pilot.

## 2026-05-18

### Migration slice
Feature/use case: `mcp-devto` module pilot implementation and tests.

### Source evidence reviewed
- `migration/output/mcp-devto.md`
- `migration/requirement-test-matrix.md`
- Official MCP Kotlin SDK documentation for server STDIO transport and `CallToolResult`.

### Requirements created or updated
- No requirement changes.

### Tests created or updated
- Added automated Kotlin/JUnit tests for `TEST-MCP-DEVTO-003` through `TEST-MCP-DEVTO-007`.
- Verified `TEST-MCP-DEVTO-001` and `TEST-MCP-DEVTO-002` with subprocess smoke checks.
- `TEST-MCP-DEVTO-008` is partially verified by `installDist` and subprocess startup; MCP client initialize/list-tools check remains pending.

### Implementation summary
- Added Gradle wrapper and Gradle multi-module monorepo setup.
- Added independent `services:mcp-devto` Kotlin/JVM service module.
- Implemented Dev.to draft post client, MCP tool schema, MCP tool handler, and STDIO server entry point.
- Added Java 21 runtime Dockerfile for the service artifact.

### Behavior comparison
- Match: Target tests and smoke checks match approved requirements covered so far.
- Differences: Source-undefined validation/error details use project-standard validation behavior per accepted unknown decisions.

### Unknowns or risks
- Remaining verification gap: full MCP client initialize/list-tools check for `TEST-MCP-DEVTO-008`.

### Approved deviations
- None.

### PR
- Link: Not created.

### Decision
- Continue with full subprocess MCP client behavior comparison before marking pilot complete.

## 2026-05-18

### Migration slice
Feature/use case: Phase 1 full discovery and spec map.

### Source evidence reviewed
- Full source file inventory excluding dependency/cache folders.
- `orchestrator/main.py`
- `orchestrator/config.py`
- `orchestrator/llm_utils.py`
- `orchestrator/slack_app.py`
- `orchestrator/.env.example`
- `orchestrator/requirements.txt`
- `mcp-devto/src/index.ts`
- `mcp-devto/.env.example`
- `mcp-devto/Dockerfile`
- Root `Dockerfile`
- `docker-compose.yml`
- `README.md`
- `prompts/*.md`

### Requirements created or updated
- Added `migration/output/system-map.md`.
- Added `migration/output/requirements.md`.
- Added `migration/output/migration-plan.md`.
- Updated `migration/migration.md` for the two-phase workflow.

### Tests created or updated
- No target tests added in this phase.

### Implementation summary
- No implementation changes for Phase 1.

### Behavior comparison
- Match: Not run in this phase.
- Differences: Prompt-only intended behavior was separated from current source behavior.

### Unknowns or risks
- Added `OPEN-ORCH-001`, `OPEN-ORCH-002`, and `OPEN-CONFIG-001` to `migration/open-points.md`.

### Approved deviations
- None.

### PR
- Link: Not created.

### Decision
- Review Phase 1 outputs before proceeding with planned implementation slices.

## 2026-05-18

### Migration slice
Feature/use case: Phase 1 approval decisions.

### Source evidence reviewed
- `migration/output/system-map.md`
- `migration/output/requirements.md`
- `migration/output/migration-plan.md`
- `migration/open-points.md`

### Requirements created or updated
- Phase 1 map and migration plan marked `APPROVED`.
- Source-code-as-truth decisions recorded.

### Tests created or updated
- No target tests added.

### Implementation summary
- No implementation changes.

### Behavior comparison
- Match: Not run.
- Differences: Prompt-folder conflicts resolved in favor of source code.

### Unknowns or risks
- `OPEN-ORCH-001`, `OPEN-ORCH-002`, and `OPEN-CONFIG-001` resolved.

### Approved deviations
- None.

### PR
- Link: Not created.

### Decision
- Proceed with planned implementation order, starting with `config`.

## 2026-05-18

### Migration slice
Feature/use case: `config`.

### Source evidence reviewed
- `/home/leonardo/workspaces/synapses/orchestrator/config.py`
- `/home/leonardo/workspaces/synapses/orchestrator/.env.example`
- `migration/output/requirements.md`

### Requirements created or updated
- Added `migration/output/config.md`.
- Updated `migration/output/migration-plan.md` status for `config`.

### Tests created or updated
- Added `TEST-CONFIG-001` through `TEST-CONFIG-004`.
- Updated `migration/requirement-test-matrix.md`.

### Implementation summary
- Added `:services:orchestrator` Gradle module.
- Added `AppConfig` and `AppConfigLoader`.
- Preserved source-style `.env` loading with root `.env`, `orchestrator/.env`, required values, ignored extras, and default `LLM_PROVIDER=openai`.

### Behavior comparison
- Match: Config behavior covered by unit tests from approved requirements.
- Differences: None intended.

### Unknowns or risks
- None new.

### Approved deviations
- None.

### PR
- Link: Not created.

### Decision
- Continue to `llm-client`.

## 2026-05-18

### Migration slice
Feature/use case: `llm-client`.

### Source evidence reviewed
- `/home/leonardo/workspaces/synapses/orchestrator/llm_utils.py`
- `migration/output/requirements.md`

### Requirements created or updated
- Added `migration/output/llm-client.md`.
- Updated `migration/output/migration-plan.md` status for `llm-client`.

### Tests created or updated
- Added `TEST-LLM-001` through `TEST-LLM-005`.
- Added `TEST-PERSONA-001` and `TEST-PERSONA-002`.
- Updated `migration/requirement-test-matrix.md`.

### Implementation summary
- Added Kotlin LLM provider normalization, API key fallback, model selection, request config, chat payload, retry/error handling, response parsing, and persona prompt helpers.

### Behavior comparison
- Match: LLM and persona behavior covered by unit tests from approved requirements.
- Differences: None intended.

### Unknowns or risks
- None new.

### Approved deviations
- None.

### PR
- Link: Not created.

### Decision
- Continue to `orchestrator-mcp-lifecycle`.

## 2026-05-18

### Migration slice
Feature/use case: `orchestrator-mcp-lifecycle` and `slack-events` specifications.

### Source evidence reviewed
- `/home/leonardo/workspaces/synapses/orchestrator/main.py`
- `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py`
- `migration/output/requirements.md`

### Requirements created or updated
- Approved completed `llm-client` slice.
- Added `migration/output/orchestrator-mcp-lifecycle.md`.
- Added `migration/output/slack-events.md`.
- Updated `migration/output/migration-plan.md`.

### Tests created or updated
- Planned `TEST-ORCH-LIFE-001` through `TEST-ORCH-LIFE-004`.
- Planned `TEST-SLACK-001` through `TEST-SLACK-008`.
- No target implementation tests created in this step.

### Implementation summary
- No code changes. Generated implementation-ready slice specs.

### Behavior comparison
- Match: Specs are directly mapped to approved source requirements.
- Differences: None intended.

### Unknowns or risks
- None new.

### Approved deviations
- None.

### PR
- Link: Not created.

### Decision
- Continue to implementation, starting with `orchestrator-mcp-lifecycle`.

## 2026-05-18

### Migration slice
Feature/use case: `orchestrator-mcp-lifecycle` and `slack-events` implementation.

### Source evidence reviewed
- `/home/leonardo/workspaces/synapses/orchestrator/main.py`
- `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py`
- `migration/output/orchestrator-mcp-lifecycle.md`
- `migration/output/slack-events.md`

### Requirements created or updated
- Updated `migration/output/orchestrator-mcp-lifecycle.md` to implemented.
- Updated `migration/output/slack-events.md` to implemented.
- Updated `migration/output/migration-plan.md`.

### Tests created or updated
- Added `TEST-ORCH-LIFE-001` through `TEST-ORCH-LIFE-004`.
- Added `TEST-SLACK-001` through `TEST-SLACK-008`.
- Updated `migration/requirement-test-matrix.md`.

### Implementation summary
- Added Ktor root route and injectable MCP runtime lifecycle.
- Added Kotlin MCP client wrapper using the MCP Kotlin SDK STDIO transport.
- Added Ktor Slack events route with UTF-8 validation, signature validation hook, retry handling, JSON/challenge handling, deduplication, bot/self filtering, and app mention scheduling hook.

### Behavior comparison
- Match: Lifecycle and Slack route behavior covered by automated target tests from approved requirements.
- Differences: Actual app mention processing remains deferred to the approved `mention-processing` slice.

### Unknowns or risks
- The production MCP command defaults to the Gradle `installDist` script path for `mcp-devto`; runtime packaging can refine this in the `runtime` slice.

### Approved deviations
- None.

### PR
- Link: Not created.

### Decision
- Continue to `mention-processing`.

## 2026-05-18

### Migration slice
Feature/use case: `mention-processing`.

### Source evidence reviewed
- `/home/leonardo/workspaces/synapses/orchestrator/slack_app.py`
- `migration/output/requirements.md`

### Requirements created or updated
- Added `migration/output/mention-processing.md`.
- Updated `migration/output/migration-plan.md`.

### Tests created or updated
- Added `TEST-MENTION-001` through `TEST-MENTION-008`.
- Added `TEST-MCP-CLIENT-002`.
- Updated `migration/requirement-test-matrix.md`.

### Implementation summary
- Added Slack app mention processor with injectable Slack, LLM, and MCP clients.
- Added MCP tool-schema conversion, tool-call execution, invalid JSON argument skipping, provider-specific response behavior, and source-compatible final Slack output selection.
- Added Slack Java SDK `chat.postMessage` adapter.

### Behavior comparison
- Match: Mention-processing and MCP client tool-use behavior covered by automated target tests from approved requirements.
- Differences: None intended.

### Unknowns or risks
- Runtime wiring still needs to instantiate the production Slack/LLM processor from config.

### Approved deviations
- None.

### PR
- Link: Not created.

### Decision
- Continue to `runtime`.

## 2026-05-18

### Migration slice
Feature/use case: `runtime` and behavior comparison.

### Source evidence reviewed
- `/home/leonardo/workspaces/synapses/Dockerfile`
- `/home/leonardo/workspaces/synapses/docker-compose.yml`
- `/home/leonardo/workspaces/synapses/orchestrator/main.py`
- `migration/output/requirements.md`

### Requirements created or updated
- Added `migration/output/runtime.md`.
- Added `migration/output/behavior-comparison.md`.
- Updated `migration/output/migration-plan.md`.

### Tests created or updated
- Added `TEST-RUNTIME-001` through `TEST-RUNTIME-004`.
- Updated `migration/requirement-test-matrix.md`.

### Implementation summary
- Added Ktor production `main()` entrypoint and config-to-runtime wiring.
- Added Gradle application packaging for `services/orchestrator`.
- Added root Dockerfile, docker-compose file, `.dockerignore`, and target env example.
- Kept MCP Dev.to as an orchestrator-owned subprocess inside the runtime image.

### Behavior comparison
- Match: Installed app and container both return the source health payload.
- Differences: Runtime now uses Gradle/Ktor packaging instead of Python/Uvicorn, preserving approved external behavior.

### Unknowns or risks
- Real Slack, LLM, and Dev.to flows still require valid credentials for external validation.

### Approved deviations
- None.

### PR
- Link: Not created.

### Decision
- Migration code complete.

## Entry template

```markdown
## Date

### Migration slice
Feature/use case:

### Source evidence reviewed
- 

### Requirements created or updated
- 

### Tests created or updated
- 

### Implementation summary
- 

### Behavior comparison
- Match:
- Differences:

### Unknowns or risks
- 

### Approved deviations
- 

### PR
- Link:

### Decision
- Continue
- Rework
- Blocked
```
