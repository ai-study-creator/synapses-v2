# Open Points

Use this file for unresolved or accepted migration items:

- `UNKNOWN`: behavior or implementation detail not yet known.
- `ASSUMPTION`: inferred behavior that lacks direct evidence.
- `RISK`: accepted or unresolved migration risk.
- `DEVIATION`: approved behavior difference from the source.
- `BLOCKER`: item that prevents progress.

| ID | Type | Description | Source/Context | Decision/Impact | Owner | Status |
|---|---|---|---|---|---|---|
| UNK-MCP-DEVTO-001 | UNKNOWN | Exact MCP SDK error payload for invalid tool arguments is not defined in application code. | `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:52-62` | Decision: source-undefined validation details do not require exact preservation; target should use project-wide validation behavior and must prevent Dev.to calls for invalid input. | Migration owner | Resolved |
| UNK-MCP-DEVTO-002 | UNKNOWN | Exact behavior when Dev.to returns a non-JSON error body is not explicitly handled. | `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:37-40` | Decision: source-undefined upstream error parsing does not require exact preservation; target should use project-wide error handling for this case. | Migration owner | Resolved |
| UNK-MCP-DEVTO-003 | UNKNOWN | Final Gradle project layout and target artifact path. | `migration/tech-stack-migrated.md` DEC-STACK-007; `migration/output/mcp-devto.md` | Decision: monorepo, Gradle multi-level modules, and `mcp-devto` as an independent service module/artifact. | Migration owner | Resolved |
| RISK-MCP-DEVTO-001 | RISK | Source module has no automated tests; package test script intentionally fails. | `/home/leonardo/workspaces/synapses/mcp-devto/package.json:6-8`; `migration/output/mcp-devto.md`; `migration/requirement-test-matrix.md` | Decision: source-untested features are validated from approved requirements and feature description against target automated tests. | Migration owner | Accepted risk |
| RISK-MCP-DEVTO-002 | RISK | Low-level HTTP/network error messages may differ between Node fetch and the target Kotlin HTTP client. | `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:71-80` | Decision: not blocking when the approved error envelope is preserved. | Migration owner | Accepted risk |
| OPEN-ORCH-001 | UNKNOWN | Slack app mention source behavior uses `devto` persona and exposes MCP tools, while the initial prompt describes a summary-only Slack flow without MCP tools. | `orchestrator/slack_app.py:126-137`; `prompts/Orchestrator_Brain.md:24-26` | Decision: source code is the source of truth; preserve `devto` persona plus MCP tools. | Migration owner | Resolved |
| OPEN-ORCH-002 | UNKNOWN | GitHub Models is supported by the LLM client but Slack tool-calling flow returns a message telling users to use OpenAI. | `orchestrator/llm_utils.py:114-148`; `orchestrator/slack_app.py:141-147` | Decision: ignore prompt folder when it conflicts with source; preserve current source behavior. | Migration owner | Resolved |
| OPEN-CONFIG-001 | UNKNOWN | Target local configuration loading strategy: preserve source-style `.env` loading or rely only on process environment. | `orchestrator/config.py:6-13`; `mcp-devto/src/index.ts:7` | Decision: preserve `.env` variable loading behavior even if it is not the ideal Kotlin approach. | Migration owner | Resolved |
