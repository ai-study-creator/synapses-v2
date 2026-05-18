# Slice: slack-events

Status: `IMPLEMENTED`

## Source Evidence

- `orchestrator/slack_app.py:18-54`: 60-second in-memory deduplication.
- `orchestrator/slack_app.py:57-62`: bot/self event detection.
- `orchestrator/slack_app.py:166-186`: raw body, UTF-8 handling, and Slack signature validation.
- `orchestrator/slack_app.py:188-198`: Slack retry and invalid JSON handling.
- `orchestrator/slack_app.py:200-205`: URL verification challenge handling.
- `orchestrator/slack_app.py:207-225`: deduplication, event routing, app mention scheduling, default `200`.

## Requirements

- `REQ-SLACK-001`: `POST /slack/events` reads raw body and validates UTF-8.
- `REQ-SLACK-002`: missing or invalid Slack signature returns `403` with `Invalid Slack signature`.
- `REQ-SLACK-003`: Slack retry requests return `200` without processing.
- `REQ-SLACK-004`: invalid JSON returns `400` with `Invalid JSON payload`.
- `REQ-SLACK-005`: URL verification with challenge returns `{"challenge": challenge}`.
- `REQ-SLACK-006`: URL verification without challenge returns `400` with `Missing challenge`.
- `REQ-SLACK-007`: duplicate events within 60 seconds return `200` and skip processing.
- `REQ-SLACK-008`: dedup key uses `event_id`, otherwise event type/channel/timestamp.
- `REQ-SLACK-009`: payloads without `event` return `200`.
- `REQ-SLACK-010`: bot/self `app_mention` events return `200` and skip processing.
- `REQ-SLACK-011`: non-bot `app_mention` events schedule async processing and immediately return `200`.
- `REQ-SLACK-012`: non-`app_mention` events return `200`.

## Tests

- `TEST-SLACK-001` -> `REQ-SLACK-001`, `REQ-SLACK-002`: invalid UTF-8 and invalid/missing signature responses.
- `TEST-SLACK-002` -> `REQ-SLACK-003`: Slack retry headers return `200` without processing.
- `TEST-SLACK-003` -> `REQ-SLACK-004`: invalid JSON returns `400`.
- `TEST-SLACK-004` -> `REQ-SLACK-005`, `REQ-SLACK-006`: URL verification challenge behavior.
- `TEST-SLACK-005` -> `REQ-SLACK-007`, `REQ-SLACK-008`: dedup keys and TTL behavior with controllable clock.
- `TEST-SLACK-006` -> `REQ-SLACK-009`, `REQ-SLACK-012`: no-event and non-app-mention payloads return `200`.
- `TEST-SLACK-007` -> `REQ-SLACK-010`: bot/self app mentions skip processing.
- `TEST-SLACK-008` -> `REQ-SLACK-011`: user app mentions enqueue async processing and return immediately.

## Target Shape

- Add Ktor route `POST /slack/events`.
- Keep signature validation, dedup store, clock, and app-mention scheduler injectable for route tests.
- Leave actual mention processing behavior for the later `mention-processing` slice.

## Decisions / Open Points

- Source code remains the source of truth.
- Prompt-folder behavior is ignored if it conflicts with this route behavior.

## Result

- Implemented: Ktor `POST /slack/events` route with source-compatible request validation, Slack retry handling, JSON/challenge handling, deduplication, bot/self filtering, and app mention scheduling hook.
- Verified: `./gradlew :services:orchestrator:test`.
- Differences: Actual app mention processing remains intentionally deferred to the `mention-processing` slice.
