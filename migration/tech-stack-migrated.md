# Migrated Technical Requirements

## Purpose

This document identifies the technology changes required to migrate the source system described in `/home/leonardo/workspaces/synapses/tech-stack.md` into the target stack.

The migration target is:

- Kotlin on the JVM
- Ktor for the HTTP service
- Java `21.0.2` as the runtime base

This is a technology migration only. Runtime behavior must still be extracted, specified, reviewed, tested, and compared through the migration workflow before implementation.

## Source Stack Summary

The source system has two runtime parts:

| Source area | Current technology |
| --- | --- |
| Orchestrator HTTP service | Python, FastAPI, Uvicorn |
| Slack integration | `slack_sdk` for Web API and request signature verification |
| LLM integration | Python `requests` against OpenAI-compatible chat completions endpoints |
| MCP client transport | Python `mcp` package with STDIO client session |
| MCP Dev.to server | Node.js 20, TypeScript, `@modelcontextprotocol/sdk` |
| MCP server validation | `zod` |
| Environment/settings | `pydantic_settings`, `pydantic`, `python-dotenv`, Node `dotenv` |
| Container base images | `python:3.10-slim-bullseye`, `node:20-slim` |
| Local dependency managers | `pip`, `pnpm@10.30.1` |
| Build output | Python source executed directly; TypeScript compiled to `mcp-devto/build/index.js` |
| Main exposed port | `8000` |

## Target Stack Summary

| Target area | Migrated technology |
| --- | --- |
| Primary language | Kotlin |
| Runtime | JVM on Java `21.0.2` |
| HTTP framework | Ktor server |
| HTTP engine | Ktor Netty or CIO, to be selected during target project setup |
| Build system | Gradle with Kotlin DSL |
| Dependency management | Gradle dependency declarations and optional version catalog |
| Configuration | Environment variables read through Ktor/application configuration |
| Slack integration | Kotlin/JVM Slack client or Ktor HTTP client plus source-compatible signature verification |
| LLM integration | Ktor HTTP client calling OpenAI-compatible chat completions endpoints |
| MCP orchestration | Kotlin/JVM MCP client over STDIO, preserving JSON-RPC transport behavior |
| Dev.to MCP tool | Kotlin/JVM MCP server or equivalent in-process/STDIO tool implementation, depending on approved architecture |
| Validation | Kotlin data classes plus validation rules matching source behavior |
| Serialization | `kotlinx.serialization` or Jackson, to be selected consistently for the target project |
| Container base image | Java `21.0.2` JDK/JRE base image |
| Main exposed port | `8000`, unless an approved deviation changes it |

## Technology Replacement Map

| Source technology | Target replacement | Migration note |
| --- | --- | --- |
| Python 3.9+/3.10 | Kotlin on Java `21.0.2` | Source behavior remains authoritative; Python implementation style should not be copied blindly. |
| FastAPI | Ktor server routing | Preserve `/slack/events`, request handling, response statuses, and body behavior. |
| Uvicorn ASGI server | Ktor embedded server | Preserve host/port behavior, especially port `8000`. |
| `pydantic_settings` and `python-dotenv` | Ktor/application configuration and environment variables | Preserve supported variable names and fallback rules unless deviations are approved. |
| `slack_sdk` request verifier | Kotlin/JVM Slack SDK verifier or explicit HMAC verification | Must preserve Slack signature and timestamp validation behavior. |
| `slack_sdk` WebClient | Kotlin/JVM Slack SDK or Ktor HTTP client | Must preserve thread reply behavior and error handling. |
| Python `requests` | Ktor HTTP client | Preserve OpenAI and GitHub Models request/response handling. |
| Python `mcp` STDIO client | Kotlin/JVM MCP STDIO client | Preserve subprocess/STDIO JSON-RPC behavior. |
| Node.js 20 runtime | JVM runtime on Java `21.0.2` | Removes separate Node runtime requirement if the Dev.to MCP server is migrated to Kotlin. |
| TypeScript 5.9.3 | Kotlin | Tool schema and behavior must be specified before replacement. |
| `@modelcontextprotocol/sdk` | Kotlin/JVM MCP implementation | Exact library or internal adapter must be confirmed during implementation planning. |
| `zod` schema validation | Kotlin validation and serialization | Preserve the `create_dev_post` input contract. |
| Node `dotenv` | JVM environment/configuration loading | Preserve `DEVTO_API_KEY` availability to the tool runtime. |
| `pnpm` | Gradle | Removes Node package manager from target if no Node component remains. |
| `pip` | Gradle | Python dependency installation is replaced by Gradle dependency resolution. |
| `python:3.10-slim-bullseye` | Java `21.0.2` base image | Exact image distribution remains to be selected. |
| `node:20-slim` | Java `21.0.2` base image | Applies if the MCP Dev.to server is migrated fully to Kotlin/JVM. |
| Docker Compose with orchestrator and build-only MCP service | Docker Compose with Kotlin service or services | Final service topology depends on whether MCP remains a subprocess or becomes integrated. |

## Behavior Contracts To Preserve

The following source contracts are technology-independent and must be preserved unless a deviation is approved:

- FastAPI endpoint equivalent: `POST /slack/events`.
- Public service port: `8000`.
- Slack request signature verification using `SLACK_SIGNING_SECRET`.
- Slack retry event handling.
- Slack event deduplication behavior.
- Ignoring bot and self-generated Slack events.
- Asynchronous processing of Slack app mentions.
- LLM provider selection through `LLM_PROVIDER`.
- Supported LLM provider aliases:
  - `openai`
  - `github_models`
  - `github`
  - `github-models`
- Default model behavior:
  - OpenAI: `gpt-4.1-mini`
  - GitHub Models: `openai/gpt-4.1`
- Model override environment variables:
  - `AI_MODEL`
  - `OPENAI_MODEL`
  - `GITHUB_MODELS_MODEL`
- External LLM endpoints:
  - `https://api.openai.com/v1/chat/completions`
  - `https://models.github.ai/inference/chat/completions`
- MCP transport over STDIO unless an approved architecture deviation changes it.
- MCP tool name: `create_dev_post`.
- Dev.to API request to `POST https://dev.to/api/articles`.
- Dev.to posts are created as unpublished drafts with `published: false`.
- Dev.to API key is supplied through `DEVTO_API_KEY`.

## Environment Variable Migration

| Source variable | Target status | Notes |
| --- | --- | --- |
| `SLACK_BOT_TOKEN` | Preserve | Required for Slack Web API calls. |
| `SLACK_SIGNING_SECRET` | Preserve | Required for Slack request verification. |
| `LLM_API_KEY` | Preserve | Required for configured LLM provider. |
| `LLM_PROVIDER` | Preserve | Optional, defaults to `openai`. |
| `OPENAI_API_KEY` | Preserve | Fallback for OpenAI provider. |
| `GITHUB_MODELS_TOKEN` | Preserve | Fallback for GitHub Models provider. |
| `GITHUB_TOKEN` | Preserve | Fallback GitHub Models token. |
| `GH_TOKEN` | Preserve | Fallback GitHub Models token. |
| `AI_MODEL` | Preserve | Global model override. |
| `OPENAI_MODEL` | Preserve | OpenAI model override. |
| `GITHUB_MODELS_MODEL` | Preserve | GitHub Models model override. |
| `GITHUB_MODELS_URL` | Preserve | GitHub Models endpoint override. |
| `GITHUB_API_VERSION` | Preserve | GitHub API version header. |
| `DEVTO_API_KEY` | Preserve | Required for Dev.to draft creation. |

## Containerization Changes

| Source container concern | Target replacement |
| --- | --- |
| Root image installs both Python and Node.js | Java `21.0.2` base image with compiled Kotlin application artifact |
| Orchestrator starts with `uvicorn` | Ktor application starts from JVM main entry point |
| MCP server compiled by `pnpm run build` | Kotlin build through Gradle |
| MCP server run by `node mcp-devto/build/index.js` | Kotlin/JVM process or integrated component, pending approved architecture |
| Compose builds `orchestrator` and `mcp-devto` | Compose should build the Kotlin target service topology |

## Development Command Changes

| Source command | Target command |
| --- | --- |
| `pip install -r orchestrator/requirements.txt` | `./gradlew build` or `./gradlew test` |
| `uvicorn orchestrator.main:orchestrator_app --host 0.0.0.0 --port 8000` | `./gradlew run` or containerized Ktor application startup |
| `pnpm install --frozen-lockfile` | Gradle dependency resolution |
| `pnpm run build` | `./gradlew build` |
| `node build/index.js` | JVM entry point for MCP tool server if kept as a subprocess |
| `docker-compose up --build` | Preserve equivalent Compose command after target Docker files exist |

## Testing Migration

The source stack currently has weak or missing automated test coverage:

- The MCP Dev.to package has a placeholder failing test script.
- No Python test framework or tests are declared in the source stack document.
- No CI configuration was identified in the source stack document.

The target Kotlin/Ktor migration should introduce tests from approved requirements:

- Ktor route tests for Slack event contracts.
- Unit tests for provider selection, model fallback, and request validation rules.
- Integration or contract tests for MCP STDIO behavior where practical.
- Tests for Dev.to draft payload construction and error mapping.
- Requirement-to-test mapping in `migration/requirement-test-matrix.md`.

## Open Technical Decisions

These items should be resolved after evaluating source behavior and before implementation:

| ID | Decision | Status |
| --- | --- | --- |
| DEC-STACK-001 | Select Ktor engine: CIO. | DECIDED |
| DEC-STACK-002 | Select JSON serialization library: `kotlinx.serialization`. | DECIDED |
| DEC-STACK-003 | Select Slack integration approach: Slack Java SDK. | DECIDED |
| DEC-STACK-004 | Select MCP Kotlin/JVM implementation approach: library-backed client/server. | DECIDED |
| DEC-STACK-005 | Keep the Dev.to MCP tool as a separate STDIO subprocess. | DECIDED |
| DEC-STACK-006 | Select Java 21 container image distribution: any compatible Java 21 image is acceptable. | DECIDED |
| DEC-STACK-007 | Use a monorepo Gradle multi-module layout with `mcp-devto` as an independent service module/artifact. | DECIDED |

## Non-Goals

- Do not change source behavior because Kotlin/Ktor makes another approach easier.
- Do not remove MCP STDIO transport without an approved deviation.
- Do not change endpoint paths, environment variable names, provider aliases, model defaults, or Dev.to draft behavior without approved deviations.
- Do not introduce unrelated product features during the technology migration.
