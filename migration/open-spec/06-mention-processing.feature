Feature: Slack app mention processing
  The orchestrator turns a Slack app mention into an LLM response and optional Dev.to draft tool execution.

  @REQ-MENTION-001
  Scenario: Ignore bot or self events during mention processing
    Given mention processing receives an event from a bot or without a user
    When the event is processed
    Then no LLM request is sent
    And no Slack reply is posted

  @REQ-MENTION-002 @REQ-MENTION-003
  Scenario: Notify the Slack thread when MCP is unavailable
    Given mention processing receives a human app mention event
    And the MCP client is unavailable
    When the event is processed
    Then the Slack reply is posted to the event channel
    And the Slack reply uses thread_ts when present
    And the Slack reply falls back to event ts when thread_ts is absent
    And the Slack reply says "MCP client is not available. Please check orchestrator startup logs."

  @REQ-MENTION-004
  Scenario: Build the Dev.to persona LLM prompt
    Given mention processing receives a human app mention event
    And the MCP client is available
    When the LLM request is built
    Then the system message uses the "devto" persona
    And the user message contains the Slack event text

  @REQ-MENTION-005 @REQ-MCP-CLIENT-001
  Scenario: Send available MCP tools to the LLM
    Given the MCP client lists available tools
    When mention processing calls the LLM
    Then each MCP tool is converted to an OpenAI function schema
    And each function schema contains the tool name
    And each function schema contains the tool description
    And each function schema contains the tool input schema
    And the LLM request uses tool_choice "auto"

  @REQ-MENTION-006 @REQ-MCP-CLIENT-003 @REQ-MCP-CLIENT-004
  Scenario: Execute OpenAI tool calls through MCP
    Given the LLM provider is OpenAI
    And the LLM response includes a valid tool call
    When mention processing handles the response
    Then the tool call arguments are parsed from JSON
    And the MCP client calls the named tool with those arguments
    And the tool result text says "Executed `<function_name>` successfully: <tool_result>"

  @REQ-MCP-CLIENT-002
  Scenario: Skip tool calls with invalid JSON arguments
    Given the LLM response includes a tool call with invalid JSON arguments
    When mention processing handles the response
    Then that tool call is skipped
    And no MCP call is made for that invalid tool call
    And processing continues without throwing the parsing error

  @REQ-MENTION-008
  Scenario: Prefer tool results over LLM text
    Given the LLM response contains assistant text
    And one or more MCP tool calls are executed successfully
    When the final Slack output is selected
    Then the tool result text replaces the assistant text

  @REQ-MENTION-007
  Scenario: Warn when non OpenAI providers are used for the tool-calling flow
    Given the configured LLM provider is not OpenAI
    When mention processing handles the LLM response
    Then the final Slack output says "Configured LLM provider does not support this tool-calling flow yet. Use `LLM_PROVIDER=openai`."

  @REQ-MENTION-009
  Scenario: Provide a fallback when no response is generated
    Given mention processing has no tool result
    And the LLM response has no final text
    When the final Slack output is selected
    Then the final Slack output is "No response was generated."

  @REQ-MENTION-010
  Scenario: Post the final output to Slack
    Given mention processing has selected final output
    When processing completes
    Then the output is posted with chat_postMessage
    And the message is posted to the event channel
    And the message is posted in the event thread

  @REQ-MENTION-011
  Scenario: Do not rethrow Slack or unexpected processing errors
    Given Slack posting fails or unexpected processing fails
    When mention processing handles the error
    Then the error is logged
    And the error is not rethrown to the Slack Events request path
