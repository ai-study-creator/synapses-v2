# Synapses V2 Migrated Application

This README describes the migrated application from a functional point of view: what it does, how to run it, and how to exercise each feature.

## What The Application Does

Synapses V2 is a Slack-driven publishing assistant.

At runtime, the orchestrator receives Slack Events API requests. When a user mentions the Slack app, the orchestrator asks an LLM to respond using the Dev.to publishing persona. The LLM can choose to call an MCP tool. That tool is provided by the local `mcp-devto` service and creates a draft article in Dev.to.

The app has two runtime pieces:

- `orchestrator`: a Kotlin/Ktor HTTP service that receives Slack events, talks to the LLM, owns the MCP client, and posts replies back to Slack.
- `mcp-devto`: a Kotlin MCP STDIO service that exposes the `create_dev_post` tool and talks to the Dev.to API.

The `mcp-devto` service is not a separate always-running HTTP service. The orchestrator starts it as a subprocess and communicates with it over MCP STDIO.

## Implemented Functionalities

### Health Check

`GET /` returns:

```json
{"message":"Orchestrator Brain is running!"}
```

Use this to confirm the orchestrator is running.

### Slack Events Endpoint

`POST /slack/events` receives Slack Events API requests.

Implemented behaviors:

- Validates the raw request body is UTF-8.
- Validates Slack request signatures.
- Ignores Slack retry deliveries.
- Handles Slack URL verification challenges.
- Deduplicates repeated events for 60 seconds.
- Ignores bot/self events.
- Schedules app mention processing asynchronously and immediately returns `200`.
- Returns `200` for unsupported/non-app-mention events.

### Slack App Mention Processing

When a non-bot `app_mention` event is received:

- The reply is posted in the same channel and thread.
- If no MCP client is available, Slack receives:

```text
MCP client is not available. Please check orchestrator startup logs.
```

- The LLM prompt uses the `devto` persona.
- The Slack mention text is sent as the user prompt.
- Available MCP tools are converted to OpenAI-compatible function schemas.
- The LLM is called with `tool_choice=auto`.
- If the provider is `openai`, returned tool calls are executed through MCP.
- Tool results replace the plain LLM text when tool calls are executed.
- If the provider is not OpenAI, Slack receives:

```text
Configured LLM provider does not support this tool-calling flow yet. Use `LLM_PROVIDER=openai`.
```

- Empty output becomes:

```text
No response was generated.
```

### Dev.to Draft Tool

The MCP tool is named:

```text
create_dev_post
```

It accepts:

- `title`: post title
- `body_markdown`: post body in Markdown
- `tags`: list of tags

It creates a draft Dev.to article. Draft mode is preserved by the target implementation.

### LLM Providers

Supported providers:

- `openai`
- `github_models`

Aliases:

- `github` -> `github_models`
- `github-models` -> `github_models`

Default provider:

```text
openai
```

Default models:

- OpenAI: `gpt-4.1-mini`
- GitHub Models: `openai/gpt-4.1`

Tool-calling mention flow is only supported for `openai`, matching the migrated source behavior.

## Required Environment

The app loads config from:

1. Root `.env`
2. `orchestrator/.env`
3. Process environment

Process environment wins over `.env` files.

Required:

```bash
SLACK_BOT_TOKEN=...
SLACK_SIGNING_SECRET=...
LLM_API_KEY=...
DEVTO_API_KEY=...
```

Common optional values:

```bash
LLM_PROVIDER=openai
PORT=8000
OPENAI_MODEL=gpt-4.1-mini
AI_MODEL=...
```

GitHub Models optional values:

```bash
LLM_PROVIDER=github_models
GITHUB_MODELS_TOKEN=...
GITHUB_TOKEN=...
GH_TOKEN=...
GITHUB_MODELS_MODEL=openai/gpt-4.1
GITHUB_MODELS_URL=https://models.github.ai/inference/chat/completions
GITHUB_API_VERSION=2022-11-28
```

## Run Locally

From the repository root:

```bash
./gradlew :services:mcp-devto:installDist :services:orchestrator:installDist
./services/orchestrator/build/install/orchestrator/bin/orchestrator
```

The app starts on:

```text
http://127.0.0.1:8000
```

Check health:

```bash
curl http://127.0.0.1:8000/
```

Expected:

```json
{"message":"Orchestrator Brain is running!"}
```

If port `8000` is already in use:

```bash
ss -ltnp | grep ':8000'
kill <pid>
```

Or run on another port:

```bash
PORT=8001 ./services/orchestrator/build/install/orchestrator/bin/orchestrator
```

## Run With Docker

Build the image:

```bash
docker build -t synapses-v2 .
```

Run:

```bash
docker run --rm -p 8000:8000 \
  -e SLACK_BOT_TOKEN="$SLACK_BOT_TOKEN" \
  -e SLACK_SIGNING_SECRET="$SLACK_SIGNING_SECRET" \
  -e LLM_API_KEY="$LLM_API_KEY" \
  -e LLM_PROVIDER="${LLM_PROVIDER:-openai}" \
  -e DEVTO_API_KEY="$DEVTO_API_KEY" \
  synapses-v2
```

Check:

```bash
curl http://127.0.0.1:8000/
```

## Run With Docker Compose

With environment values exported or present in your shell:

```bash
docker compose up --build
```

Then:

```bash
curl http://127.0.0.1:8000/
```

## Expose With Nginx On A Public IP

Use this when the app is running on a public VM and you want Nginx to forward public HTTP traffic to the local orchestrator on port `8000`.

Install Nginx on the host if needed:

```bash
sudo apt update
sudo apt install nginx
```

Start the app first with Docker Compose or the local Gradle install. Confirm the local health check works:

```bash
curl http://127.0.0.1:8000/
```

Then configure Nginx:

```bash
sudo ./scripts/configure-nginx-public-ip.sh --server-name <public-ip>
```

The script writes an Nginx site named `synapses-v2`, proxies requests to `http://127.0.0.1:8000`, validates the Nginx config, and reloads Nginx.

Optional values:

```bash
sudo ./scripts/configure-nginx-public-ip.sh \
  --server-name <public-ip-or-domain> \
  --app-port 8000 \
  --listen-port 80 \
  --open-firewall
```

If the machine uses a cloud firewall or security group, allow inbound TCP `80`. After Nginx reloads, check the public HTTP endpoint:

```bash
curl http://<public-ip>/
```

For Slack, configure an HTTPS URL with a valid SSL certificate. Slack verifies the SSL certificate during URL verification, so use a DNS name with TLS, or use a tunnel such as ngrok or Cloudflare Tunnel for temporary development.

The Slack request URL path remains:

```text
https://<public-host>/slack/events
```

## Configure Slack

In the Slack app configuration:

1. Enable Event Subscriptions.
2. Set the request URL to your public orchestrator URL:

```text
https://<public-host>/slack/events
```

For local development, expose port `8000` with a tunnel such as ngrok or Cloudflare Tunnel, then use the tunnel URL.

3. Subscribe to bot events:

```text
app_mention
```

4. Ensure the bot token has permission to post messages in channels where it is used.

5. Install or reinstall the Slack app after changing scopes/events.

Slack URL verification should succeed because the app supports the `url_verification` challenge response.

## Exercise The Main Feature

After the app is running and Slack is configured:

1. Invite the Slack bot to a channel.
2. Mention it with a publishing request, for example:

```text
@your-bot Write a Dev.to draft about migrating a Node MCP service to Kotlin with Ktor. Include tradeoffs and practical steps.
```

Expected behavior:

- Slack sends the event to `/slack/events`.
- The orchestrator validates the signature and immediately returns `200`.
- The app processes the mention asynchronously.
- The LLM receives the `devto` persona and the Slack text.
- The LLM can call `create_dev_post`.
- If a tool call is returned, the MCP Dev.to subprocess creates a draft article.
- The Slack thread receives either the tool execution result or the LLM response.

## Direct Endpoint Checks

Health:

```bash
curl -i http://127.0.0.1:8000/
```

Invalid Slack signature:

```bash
curl -i -X POST http://127.0.0.1:8000/slack/events \
  -H 'content-type: application/json' \
  -d '{"type":"event_callback"}'
```

Expected:

```text
403 Invalid Slack signature
```

Real Slack requests must include valid `x-slack-request-timestamp` and `x-slack-signature` headers.

## Validation Status

Automated and smoke validation completed:

- Gradle full build
- MCP Dev.to tests
- Config tests
- LLM tests
- Orchestrator lifecycle tests
- Slack events route tests
- Slack mention processing tests
- Local runtime smoke on port `8000`
- Docker image build
- Container smoke

Not validated without real credentials:

- Real Slack posting
- Real LLM provider response
- Real Dev.to draft creation

Those paths require valid external account credentials.
