# Specification: MCP Dev.to Module

## Source Evidence Summary

- `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:7-14`: loads `.env`, requires `DEVTO_API_KEY`, writes missing-key error to stderr, and exits with code `1`.
- `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:21-35`: sends `POST https://dev.to/api/articles` with JSON payload and `published: false`.
- `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:37-44`: maps non-OK Dev.to responses to an error and returns parsed JSON on success.
- `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:47-60`: creates MCP server `mcp-devto` version `1.0.0` and registers `create_dev_post`.
- `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:63-80`: returns MCP success/error envelopes.
- `/home/leonardo/workspaces/synapses/mcp-devto/src/index.ts:84-93`: connects server to STDIO and logs startup/fatal errors to stderr.
- `/home/leonardo/workspaces/synapses/mcp-devto/package.json:6-8`: build script exists; test script is a placeholder failure.
- `/home/leonardo/workspaces/synapses/mcp-devto/Dockerfile:2-20`: builds TypeScript and runs `node build/index.js`.
- `/home/leonardo/workspaces/synapses/orchestrator/main.py:31-47`: orchestrator starts the MCP server as a subprocess over STDIO.

## Scope

Pilot migration slice: full `mcp-devto` module.

Status: `APPROVED`

The target implementation must preserve source behavior while migrating the module to Kotlin/JVM using the selected target stack decisions:

- Java 21 runtime.
- Kotlin.
- Library-backed MCP client/server implementation.
- Separate STDIO subprocess for the Dev.to MCP tool.
- `kotlinx.serialization` for JSON serialization.

## REQ-MCP-DEVTO-001

## Status

`APPROVED`

## Title

Start an MCP server named `mcp-devto` over STDIO.

## Description

The module must start an MCP server with name `mcp-devto` and version `1.0.0`, connect it to STDIO transport, and keep stdout reserved for MCP JSON-RPC traffic.

## Source evidence

```text
File/class: /home/leonardo/workspaces/synapses/mcp-devto/src/index.ts
Method/function: server construction and main
Route/topic/job: MCP STDIO server
Annotation/configuration: none
Exception/error handler: main().catch
Database/schema/migration: none
Existing test: none
Evidence lines: index.ts:47-50, index.ts:84-93, README.md:69-79
```

## Inputs

- STDIO input from MCP client.

## Business rules

- Server name is `mcp-devto`.
- Server version is `1.0.0`.
- Transport is STDIO.
- Normal runtime logs from this module must not be written to stdout.

## Outputs

- MCP JSON-RPC over stdout/stdin through the selected MCP library.
- Startup message `MCP Dev.to Server Started` written to stderr.
- Fatal startup failure message written to stderr and process exit code `1`.

## Error cases

```text
Condition: MCP server startup or transport connection fails.
Error/exception: Rejected main() promise in the source.
Mapped result: stderr message "MCP Dev.to Server failed: <message>".
Status code: process exit code 1.
Error body: none.
Side effect: process exits.
```

## Security behavior

- No user-level authorization is implemented inside this module.
- STDIO must remain valid MCP traffic; logs are written to stderr.

## Unknowns

- None for the application-level startup contract.

## Assumptions

- ASSUMPTION: Target MCP library exposes equivalent server name/version metadata.

## Tests required

```text
Test ID: TEST-MCP-DEVTO-001
Requirement ID: REQ-MCP-DEVTO-001
Scenario: Server starts with valid DEVTO_API_KEY.
Given: DEVTO_API_KEY is set.
When: The MCP server process starts.
Then: It initializes over STDIO and writes startup information to stderr, not stdout.
Expected result: Process remains running and stdout remains available for MCP JSON-RPC.
```

## Migration notes

- Implement as a separate Kotlin/JVM STDIO subprocess.
- Preserve stderr logging behavior.

## Approved deviations

None.

## REQ-MCP-DEVTO-002

## Status

`APPROVED`

## Title

Require `DEVTO_API_KEY` at startup.

## Description

The module must load configuration, require `DEVTO_API_KEY`, and fail fast when it is absent.

## Source evidence

```text
File/class: /home/leonardo/workspaces/synapses/mcp-devto/src/index.ts
Method/function: module initialization
Route/topic/job: MCP process startup
Annotation/configuration: /home/leonardo/workspaces/synapses/mcp-devto/.env.example
Exception/error handler: direct process.exit
Database/schema/migration: none
Existing test: none
Evidence lines: index.ts:7-14, .env.example:1, README.md:44-52
```

## Inputs

- Environment variable `DEVTO_API_KEY`.
- Optional local `.env` file for standalone development.

## Business rules

- `DEVTO_API_KEY` is required before server startup completes.
- Missing `DEVTO_API_KEY` prevents the MCP server from running.

## Outputs

- On missing API key, stderr receives `DEVTO_API_KEY is not set in the environment variables.`
- Process exits with code `1`.

## Error cases

```text
Condition: DEVTO_API_KEY is absent.
Error/exception: Startup configuration failure.
Mapped result: stderr message.
Status code: process exit code 1.
Error body: none.
Side effect: MCP server does not start.
```

## Security behavior

- The Dev.to API key is not logged.
- The API key is used later only as the Dev.to `api-key` request header.

## Unknowns

- None. Target configuration loading may use environment variables and project-standard local development support as long as `DEVTO_API_KEY` is preserved.

## Assumptions

- ASSUMPTION: Target may use equivalent environment loading as long as the public variable name remains `DEVTO_API_KEY`.

## Tests required

```text
Test ID: TEST-MCP-DEVTO-002
Requirement ID: REQ-MCP-DEVTO-002
Scenario: Missing DEVTO_API_KEY fails startup.
Given: DEVTO_API_KEY is unset.
When: The MCP server process starts.
Then: It writes the missing-key message to stderr and exits with code 1.
Expected result: No MCP server is available.
```

## Migration notes

- Preserve the variable name exactly.
- Do not print the secret value.

## Approved deviations

None.

## REQ-MCP-DEVTO-003

## Status

`APPROVED`

## Title

Expose the `create_dev_post` MCP tool with the source input schema.

## Description

The MCP server must register a tool named `create_dev_post` with description `Creates a new draft post on Dev.to.` and inputs `title`, `body_markdown`, and `tags`.

## Source evidence

```text
File/class: /home/leonardo/workspaces/synapses/mcp-devto/src/index.ts
Method/function: server.registerTool
Route/topic/job: MCP tool create_dev_post
Annotation/configuration: zod input schema
Exception/error handler: MCP SDK validation behavior
Database/schema/migration: none
Existing test: none
Evidence lines: index.ts:52-62, README.md:73-79, tech-stack.md:150-160
```

## Inputs

- `title: string`
- `body_markdown: string`
- `tags: string[]`

## Business rules

- Tool name is `create_dev_post`.
- `title` is a string.
- `body_markdown` is a string.
- `tags` is an array of strings.

## Outputs

- Tool metadata and input schema are exposed through MCP.

## Error cases

```text
Condition: Invalid MCP tool input type.
Error/exception: SDK/schema validation error.
Mapped result: UNKNOWN; application code relies on MCP SDK and zod.
Status code: Not applicable.
Error body: Project-standard validation response; exact source payload is not defined.
Side effect: Tool handler should not create a Dev.to post for invalid input.
```

## Security behavior

- No secret is accepted as tool input.
- Tool input is passed to the Dev.to article payload.

## Unknowns

- None. Exact invalid-argument MCP error payload is not defined by source application code; target should use project-wide validation behavior and must not call Dev.to for invalid input.

## Assumptions

- ASSUMPTION: The selected Kotlin MCP library can expose equivalent JSON schema/tool metadata.

## Tests required

```text
Test ID: TEST-MCP-DEVTO-003
Requirement ID: REQ-MCP-DEVTO-003
Scenario: Tool schema is listed through MCP.
Given: The MCP server is running.
When: An MCP client lists tools.
Then: create_dev_post is present with title, body_markdown, and tags inputs.
Expected result: Input contract matches the source.
```

```text
Test ID: TEST-MCP-DEVTO-004
Requirement ID: REQ-MCP-DEVTO-003
Scenario: Invalid tool input is rejected before Dev.to API call.
Given: The MCP server is running.
When: create_dev_post is called with tags that are not an array of strings.
Then: The call fails validation.
Expected result: No Dev.to HTTP request is sent.
```

## Migration notes

- Use `kotlinx.serialization` models plus MCP library schema support.
- Preserve field names, especially `body_markdown`.

## Approved deviations

None.

## REQ-MCP-DEVTO-004

## Status

`APPROVED`

## Title

Create Dev.to posts as unpublished drafts.

## Description

When `create_dev_post` is called with valid input, the module must call the Dev.to Articles API and create an unpublished draft article.

## Source evidence

```text
File/class: /home/leonardo/workspaces/synapses/mcp-devto/src/index.ts
Method/function: createDevPost
Route/topic/job: MCP tool create_dev_post
Annotation/configuration: DEVTO_API_KEY
Exception/error handler: non-OK response handling
Database/schema/migration: none
Existing test: none
Evidence lines: index.ts:16-35, tech-stack.md:162-168
```

## Inputs

- `title`
- `body_markdown`
- `tags`
- `DEVTO_API_KEY`

## Business rules

- HTTP method is `POST`.
- URL is `https://dev.to/api/articles`.
- Request header `Content-Type` is `application/json`.
- Request header `api-key` is the configured `DEVTO_API_KEY`.
- Request body shape is:

```json
{
  "article": {
    "title": "<title>",
    "body_markdown": "<body_markdown>",
    "tags": ["<tag>"],
    "published": false
  }
}
```

- `published` is always `false`.

## Outputs

- Dev.to article is created as a draft when the external API succeeds.
- Raw Dev.to response JSON is returned to the tool handler.

## Error cases

```text
Condition: Dev.to returns a non-OK HTTP response.
Error/exception: Error("Failed to create Dev.to post: <status> <statusText> - <errorJson>").
Mapped result: Tool handler catches the error and returns MCP error content.
Status code: Not directly exposed as HTTP status by the MCP tool.
Error body: Included in generated error message if response body parses as JSON.
Side effect: No successful draft creation is reported.
```

## Security behavior

- Dev.to API key is sent in the `api-key` header.
- API key must not be included in response content or logs.

## Unknowns

- None. Non-JSON Dev.to error body behavior is not defined by source application code; target should use project-wide error handling for undefined upstream response formats.

## Assumptions

- ASSUMPTION: Network failures are caught by the tool handler and returned using the same MCP error shape.

## Tests required

```text
Test ID: TEST-MCP-DEVTO-005
Requirement ID: REQ-MCP-DEVTO-004
Scenario: Valid tool call sends Dev.to draft payload.
Given: DEVTO_API_KEY is configured and Dev.to API is stubbed.
When: create_dev_post is called with valid title, body_markdown, and tags.
Then: The target sends POST https://dev.to/api/articles with api-key header and published false.
Expected result: Dev.to draft payload matches source behavior.
```

## Migration notes

- Keep `published: false` hardcoded unless a deviation is approved.
- Do not rename `body_markdown`.

## Approved deviations

None.

## REQ-MCP-DEVTO-005

## Status

`APPROVED`

## Title

Return source-compatible MCP success content.

## Description

On successful Dev.to article creation, the tool must return MCP content and structured content containing `success: true`, `postId`, and `url`.

## Source evidence

```text
File/class: /home/leonardo/workspaces/synapses/mcp-devto/src/index.ts
Method/function: create_dev_post handler
Route/topic/job: MCP tool create_dev_post
Annotation/configuration: none
Exception/error handler: none for success path
Database/schema/migration: none
Existing test: none
Evidence lines: index.ts:63-70, tech-stack.md:167
```

## Inputs

- Dev.to success response JSON with expected fields:
  - `id`
  - `url`

## Business rules

- `postId` is copied from Dev.to response field `id`.
- `url` is copied from Dev.to response field `url`.
- Text content is `JSON.stringify(structuredContent)` in the source.

## Outputs

```json
{
  "content": [
    {
      "type": "text",
      "text": "{\"success\":true,\"postId\":123,\"url\":\"https://dev.to/...\"}"
    }
  ],
  "structuredContent": {
    "success": true,
    "postId": 123,
    "url": "https://dev.to/..."
  }
}
```

## Error cases

```text
Condition: Dev.to success response omits id or url.
Error/exception: None in application code.
Mapped result: Source builds structuredContent with undefined values.
Status code: Not applicable.
Error body: none.
Side effect: JSON text may omit undefined properties.
```

## Security behavior

- Success response does not expose `DEVTO_API_KEY`.

## Unknowns

- UNKNOWN: Whether Dev.to always returns `id` and `url` for article creation.

## Assumptions

- ASSUMPTION: Target should preserve source mapping from `id` to `postId`.

## Tests required

```text
Test ID: TEST-MCP-DEVTO-006
Requirement ID: REQ-MCP-DEVTO-005
Scenario: Successful Dev.to response returns MCP success content.
Given: Dev.to API stub returns id and url.
When: create_dev_post completes.
Then: MCP content text and structuredContent contain success true, postId, and url.
Expected result: Source-compatible success response shape.
```

## Migration notes

- Preserve both `content` and `structuredContent`.

## Approved deviations

None.

## REQ-MCP-DEVTO-006

## Status

`APPROVED`

## Title

Return source-compatible MCP error content.

## Description

When tool execution fails, the handler must catch the error, write an error line to stderr, and return an MCP error response with `success: false`, `error`, and `isError: true`.

## Source evidence

```text
File/class: /home/leonardo/workspaces/synapses/mcp-devto/src/index.ts
Method/function: create_dev_post handler
Route/topic/job: MCP tool create_dev_post
Annotation/configuration: none
Exception/error handler: catch block
Database/schema/migration: none
Existing test: none
Evidence lines: index.ts:71-80, tech-stack.md:168
```

## Inputs

- Error thrown by Dev.to request handling or another tool execution failure.

## Business rules

- If the caught value is an `Error`, use `error.message`.
- If the caught value is not an `Error`, use `String(error)`.
- Write `Error creating Dev.to post: <message>` to stderr.
- Return MCP response with `isError: true`.

## Outputs

```json
{
  "content": [
    {
      "type": "text",
      "text": "{\"success\":false,\"error\":\"<message>\"}"
    }
  ],
  "structuredContent": {
    "success": false,
    "error": "<message>"
  },
  "isError": true
}
```

## Error cases

```text
Condition: Dev.to returns non-OK JSON response.
Error/exception: Failed to create Dev.to post message.
Mapped result: MCP error content with isError true.
Status code: Not applicable.
Error body: JSON error body is embedded in the message.
Side effect: stderr log.
```

```text
Condition: Network or unexpected runtime error occurs.
Error/exception: Source catches Error or unknown thrown value.
Mapped result: MCP error content with isError true.
Status code: Not applicable.
Error body: message string.
Side effect: stderr log.
```

## Security behavior

- Error messages may expose Dev.to response details.
- API key must not be included unless an upstream error body unexpectedly echoes it.

## Unknowns

- UNKNOWN: Exact message for lower-level runtime/network failures depends on runtime and HTTP client.

## Assumptions

- ASSUMPTION: Target can preserve the visible error envelope even if low-level exception messages differ slightly.

## Tests required

```text
Test ID: TEST-MCP-DEVTO-007
Requirement ID: REQ-MCP-DEVTO-006
Scenario: Dev.to non-OK response returns MCP error.
Given: Dev.to API stub returns non-OK JSON error response.
When: create_dev_post is called.
Then: Tool response has success false, error message, and isError true.
Expected result: Source-compatible error envelope.
```

## Migration notes

- Preserve the MCP error envelope.
- Compare exact low-level error text during behavior comparison where practical.

## Approved deviations

None.

## REQ-MCP-DEVTO-007

## Status

`APPROVED`

## Title

Build and run as a standalone subprocess.

## Description

The source module is built separately and run as a standalone Node process. The target module must remain a separate STDIO subprocess per the approved stack decision.

## Source evidence

```text
File/class: /home/leonardo/workspaces/synapses/mcp-devto/Dockerfile
Method/function: container CMD
Route/topic/job: subprocess launched by orchestrator
Annotation/configuration: docker-compose
Exception/error handler: orchestrator MCP startup catch
Database/schema/migration: none
Existing test: none
Evidence lines: Dockerfile:2-20, docker-compose.yml:27-34, orchestrator/main.py:31-47, README.md:108-113
```

## Inputs

- Built server artifact.
- Parent process environment, including `DEVTO_API_KEY`.

## Business rules

- The MCP Dev.to module runs as a separate process.
- The orchestrator connects to it through STDIO.
- The target must keep this separate subprocess boundary.

## Outputs

- Standalone executable JVM process or equivalent runnable artifact.
- STDIO MCP server available to orchestrator.

## Error cases

```text
Condition: Subprocess cannot start or initialize.
Error/exception: Startup/connect failure.
Mapped result: Orchestrator-side MCP client startup failure.
Status code: Not applicable.
Error body: none.
Side effect: MCP client unavailable.
```

## Security behavior

- Parent environment may pass secrets into the subprocess.
- The subprocess must not print secrets.

## Unknowns

- None. Target is a monorepo with Gradle multi-level modules; this service must be an independent module/artifact managed by the Gradle build.

## Assumptions

- ASSUMPTION: Target orchestrator will launch the Kotlin/JVM MCP subprocess instead of `node build/index.js`.

## Tests required

```text
Test ID: TEST-MCP-DEVTO-008
Requirement ID: REQ-MCP-DEVTO-007
Scenario: Built MCP server runs as a separate STDIO subprocess.
Given: The target artifact is built.
When: The orchestrator or a test MCP client starts the subprocess.
Then: The client can initialize an MCP session and list create_dev_post.
Expected result: Separate-process STDIO contract is preserved.
```

## Migration notes

- Use a Gradle multi-module layout suitable for a monorepo.
- The `mcp-devto` service must remain independently buildable and runnable as its own STDIO subprocess artifact.
- No approved deviation exists for integrating the tool into the orchestrator process.

## Approved deviations

None.
