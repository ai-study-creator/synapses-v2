Feature: End-to-end Slack to Dev.to publishing flow
  The complete migrated behavior lets a Slack user request a Dev.to draft and receive the result in the Slack thread.

  @E2E @REQ-SLACK-011 @REQ-MENTION-004 @REQ-MENTION-005 @REQ-MENTION-006 @REQ-MCP-CLIENT-003 @REQ-MCP-DEVTO-004 @REQ-MENTION-010
  Scenario: Create a Dev.to draft from a Slack app mention
    Given the orchestrator is running
    And Slack sends a valid signed app_mention event from a human user
    And the MCP Dev.to service is available
    And the LLM provider is OpenAI
    And Dev.to credentials are valid
    When the Slack event is posted to /slack/events
    Then the orchestrator immediately returns 200 to Slack
    And mention processing runs asynchronously
    And the LLM receives the devto persona and Slack mention text
    And the LLM receives the create_dev_post tool schema
    When the LLM returns a create_dev_post tool call
    Then the orchestrator executes the MCP tool
    And the MCP tool creates an unpublished Dev.to draft
    And the Slack thread receives the tool execution result

  @E2E @REQ-SLACK-010 @REQ-MENTION-001
  Scenario: Ignore bot-originated Slack app mentions end to end
    Given the orchestrator is running
    And Slack sends a valid signed app_mention event from a bot
    When the event is posted to /slack/events
    Then the orchestrator returns 200
    And no LLM request is sent
    And no Dev.to draft is created
    And no Slack reply is posted by mention processing

  @E2E @REQ-MENTION-003
  Scenario: Tell the user when publishing tools are unavailable
    Given the orchestrator is running
    And Slack sends a valid signed app_mention event from a human user
    And the MCP Dev.to service is unavailable
    When the event is posted to /slack/events
    Then the orchestrator returns 200
    And the Slack thread receives "MCP client is not available. Please check orchestrator startup logs."

  @E2E @REQ-MENTION-007
  Scenario: Warn when the configured provider cannot run the tool-calling flow
    Given the orchestrator is running
    And Slack sends a valid signed app_mention event from a human user
    And the configured LLM provider is GitHub Models
    When the event is processed
    Then the Slack thread receives "Configured LLM provider does not support this tool-calling flow yet. Use `LLM_PROVIDER=openai`."

  @E2E @REQ-SLACK-007 @REQ-SLACK-008
  Scenario: Prevent duplicate Slack deliveries from creating duplicate drafts
    Given the orchestrator has processed a Slack event
    And Slack delivers the same event again within 60 seconds
    When the duplicate event is posted to /slack/events
    Then the orchestrator returns 200
    And no second LLM request is sent
    And no second Dev.to draft is created
