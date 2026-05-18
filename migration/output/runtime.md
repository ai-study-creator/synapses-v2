# Slice: runtime

Status: `IMPLEMENTED`

## Source Evidence

- `Dockerfile:23-34`: source exposes port `8000` and runs the orchestrator on `0.0.0.0:8000`.
- `docker-compose.yml:8-9`: source maps host port `8000` to container port `8000`.
- `docker-compose.yml:22-34`, `orchestrator/main.py:35-47`: source builds `mcp-devto`, but the orchestrator owns MCP subprocess startup.

## Requirements

- `REQ-RUNTIME-001`: container exposes port `8000` and runs the orchestrator on host `0.0.0.0`, port `8000`.
- `REQ-RUNTIME-002`: compose maps host port `8000` to container port `8000`.
- `REQ-RUNTIME-003`: runtime includes `mcp-devto`, but orchestrator starts it as a subprocess.

## Tests

- `TEST-RUNTIME-001`: `./gradlew clean build :services:mcp-devto:installDist :services:orchestrator:installDist`.
- `TEST-RUNTIME-002`: local installed orchestrator starts on port `8000` and `GET /` returns the source health payload.
- `TEST-RUNTIME-003`: Docker image `synapses-v2:runtime-smoke` builds successfully.
- `TEST-RUNTIME-004`: container smoke run returns the source health payload.

## Result

- Implemented: root `Dockerfile`, root `docker-compose.yml`, orchestrator application entrypoint, Gradle application packaging, and runtime env example.
- Verified: Gradle build/install, local runtime smoke, Docker build, and container smoke.
- Differences: target uses Gradle/Ktor application packaging instead of Python/Uvicorn; external behavior preserves the approved source runtime requirements.
