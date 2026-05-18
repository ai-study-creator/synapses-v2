Feature: Slack Events endpoint
  The orchestrator receives and validates Slack Events API requests.

  @REQ-SLACK-001
  Scenario: Reject a non UTF-8 request body
    Given a Slack events request body is not valid UTF-8
    When the request is posted to /slack/events
    Then the response status is 400
    And the response says "Invalid request encoding"

  @REQ-SLACK-002
  Scenario: Reject missing or invalid Slack signatures
    Given a Slack events request has no valid Slack signature
    When the request is posted to /slack/events
    Then the response status is 403
    And the response says "Invalid Slack signature"

  @REQ-SLACK-003
  Scenario: Ignore Slack retry deliveries
    Given a valid Slack events request includes x-slack-retry-num
    When the request is posted to /slack/events
    Then the response status is 200
    And the event is not processed again

  @REQ-SLACK-004
  Scenario: Reject invalid JSON payloads
    Given a validly signed Slack events request contains invalid JSON
    When the request is posted to /slack/events
    Then the response status is 400
    And the response says "Invalid JSON payload"

  @REQ-SLACK-005
  Scenario: Answer Slack URL verification
    Given a validly signed Slack URL verification request contains a challenge
    When the request is posted to /slack/events
    Then the response contains the same challenge

  @REQ-SLACK-006
  Scenario: Reject URL verification without a challenge
    Given a validly signed Slack URL verification request does not contain a challenge
    When the request is posted to /slack/events
    Then the response status is 400
    And the response says "Missing challenge"

  @REQ-SLACK-007 @REQ-SLACK-008
  Scenario: Deduplicate repeated Slack events
    Given a Slack event has already been seen within 60 seconds
    When the same event is received again
    Then the response status is 200
    And the event is not processed again
    And event_id is used as the preferred deduplication key
    And event type, channel, and timestamp are used when event_id is absent

  @REQ-SLACK-009
  Scenario: Accept payloads without an event
    Given a validly signed Slack payload does not include an event
    When the request is posted to /slack/events
    Then the response status is 200
    And no mention processing is scheduled

  @REQ-SLACK-010
  Scenario: Ignore bot or self app mentions at the route
    Given a validly signed Slack app_mention event comes from a bot or has no user
    When the request is posted to /slack/events
    Then the response status is 200
    And no mention processing is scheduled

  @REQ-SLACK-011
  Scenario: Schedule user app mention processing
    Given a validly signed app_mention event comes from a human user
    When the request is posted to /slack/events
    Then the response status is 200
    And mention processing is scheduled asynchronously
    And the request does not wait for LLM or Dev.to work to complete

  @REQ-SLACK-012
  Scenario: Accept non app mention events
    Given a validly signed Slack event is not an app_mention
    When the request is posted to /slack/events
    Then the response status is 200
    And no mention processing is scheduled
