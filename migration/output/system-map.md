# System Map

Status: `APPROVED`

## Source Repository

`/home/leonardo/workspaces/synapses`

## Target Repository

`/home/leonardo/workspaces/synapses-v2`

## Runtime Components

| Component | Source technology | Role | Target direction |
|---|---|---|---|
| `orchestrator` | Python, FastAPI, Uvicorn | HTTP service for Slack Events, LLM orchestration, MCP client owner | Kotlin/JVM, Ktor CIO server |
| `mcp-devto` | Node.js, TypeScript, MCP SDK | STDIO MCP server exposing Dev.to draft publishing tool | Kotlin/JVM independent STDIO MCP service module |
| Docker Compose | Docker Compose v3.8 | Builds both components and runs orchestrator | Gradle/JVM service topology, still containerized |

## Source Files

| Area | Files |
|---|---|
| Orchestrator app | `orchestrator/main.py`, `orchestrator/slack_app.py`, `orchestrator/llm_utils.py`, `orchestrator/config.py` |
| Orchestrator config/deps | `orchestrator/.env.example`, `orchestrator/requirements.txt` |
| MCP Dev.to | `mcp-devto/src/index.ts`, `mcp-devto/package.json`, `mcp-devto/tsconfig.json`, `mcp-devto/Dockerfile`, `mcp-devto/.env.example` |
| Runtime | `Dockerfile`, `docker-compose.yml`, `README.md`, `tech-stack.md` |
| Initial design prompts | `prompts/Dev2_MCP_Server.md`, `prompts/Orchestrator_Brain.md` |

## Public Entrypoints

| Entrypoint | Source evidence | Behavior |
|---|---|---|
| `GET /` | `orchestrator/main.py:73-75` | Returns `{"message": "Orchestrator Brain is running!"}`. |
| `POST /slack/events` | `orchestrator/main.py:70-71`, `orchestrator/slack_app.py:166-225` | Handles Slack URL verification and Event API payloads. |
| MCP STDIO server | `mcp-devto/src/index.ts:84-88` | Serves MCP over STDIO. |
| MCP tool `create_dev_post` | `mcp-devto/src/index.ts:52-82` | Creates Dev.to draft posts. |

## External Integrations

| Integration | Source evidence | Purpose |
|---|---|---|
| Slack Events API | `orchestrator/slack_app.py:166-225` | Receives Slack webhook events. |
| Slack Web API | `orchestrator/slack_app.py:22-26`, `orchestrator/slack_app.py:154-158` | Posts thread replies. |
| OpenAI Chat Completions | `orchestrator/llm_utils.py:13`, `orchestrator/llm_utils.py:135-140` | LLM provider. |
| GitHub Models chat completions | `orchestrator/llm_utils.py:14`, `orchestrator/llm_utils.py:142-148` | Alternate LLM provider. |
| Dev.to Articles API | `mcp-devto/src/index.ts:21-35` | Creates draft articles. |
| MCP STDIO | `orchestrator/main.py:35-47`, `mcp-devto/src/index.ts:84-86` | Tool discovery/execution boundary. |

## Environment Variables

| Variable | Source evidence | Required by source | Notes |
|---|---|---|---|
| `SLACK_BOT_TOKEN` | `orchestrator/config.py:15`, `orchestrator/.env.example:1` | Yes | Slack Web API token. |
| `SLACK_SIGNING_SECRET` | `orchestrator/config.py:16`, `orchestrator/.env.example:2` | Yes | Slack request signature verification. |
| `LLM_API_KEY` | `orchestrator/config.py:17`, `orchestrator/.env.example:3` | Yes by settings model | May be empty only if provider-specific fallback exists after settings load. |
| `LLM_PROVIDER` | `orchestrator/config.py:18`, `orchestrator/.env.example:4` | No | Defaults to `openai`. |
| `OPENAI_API_KEY` | `orchestrator/llm_utils.py:69-70` | Fallback | Used when provider is `openai` and `LLM_API_KEY` is empty. |
| `GITHUB_MODELS_TOKEN` | `orchestrator/llm_utils.py:72-78` | Fallback | GitHub Models token fallback. |
| `GITHUB_TOKEN` | `orchestrator/llm_utils.py:72-78`, `docker-compose.yml:19` | Fallback | GitHub Models token fallback. |
| `GH_TOKEN` | `orchestrator/llm_utils.py:72-78` | Fallback | GitHub Models token fallback. |
| `AI_MODEL` | `orchestrator/llm_utils.py:125-128` | No | Global model override. |
| `OPENAI_MODEL` | `orchestrator/llm_utils.py:130-131` | No | OpenAI model override. |
| `GITHUB_MODELS_MODEL` | `orchestrator/llm_utils.py:133` | No | GitHub Models model override. |
| `GITHUB_MODELS_URL` | `orchestrator/llm_utils.py:142` | No | GitHub Models endpoint override. |
| `GITHUB_API_VERSION` | `orchestrator/llm_utils.py:147` | No | Header defaults to `2022-11-28`. |
| `DEVTO_API_KEY` | `mcp-devto/.env.example:1`, `mcp-devto/src/index.ts:9-14` | Yes for MCP service | Required before MCP server starts. |
| `DOTENV_CONFIG_QUIET` | `orchestrator/main.py:31-33` | No | Set for MCP subprocess to keep STDIO clean. |

## Build And Runtime

| Concern | Source evidence | Behavior |
|---|---|---|
| Root Dockerfile | `Dockerfile:1-34` | Python 3.10 image, installs Node 20, installs Python deps, exposes `8000`, runs Uvicorn. |
| MCP Dockerfile | `mcp-devto/Dockerfile:2-20` | Node 20 image, pnpm install, TypeScript build, runs `node build/index.js`. |
| Compose services | `docker-compose.yml:3-34` | Builds `orchestrator` and `mcp-devto`; only orchestrator stays running. |
| Orchestrator port | `docker-compose.yml:8-9`, `Dockerfile:23-24` | Host/container port `8000`. |
| MCP subprocess command | `orchestrator/main.py:35-40` | `node mcp-devto/build/index.js`. |

## Prompt Intent Not Used As Source Truth

The initial prompt files mention behavior not implemented in the current source code. Source code is the source of truth; prompt-only or conflicting behavior is not a migration requirement.

| Prompt item | Source prompt evidence | Current code status |
|---|---|---|
| GitHub daily cron trigger | `prompts/Orchestrator_Brain.md:23` | Not implemented in source code. |
| APScheduler dependency | `prompts/Orchestrator_Brain.md:8` | Not in `requirements.txt`. |
| Anthropic provider | `prompts/Orchestrator_Brain.md:9` | Not implemented; source supports `openai` and `github_models`. |
| Slack flow explicitly forbids MCP tools | `prompts/Orchestrator_Brain.md:24-26` | Current code does expose MCP tools for Slack app mentions. |
