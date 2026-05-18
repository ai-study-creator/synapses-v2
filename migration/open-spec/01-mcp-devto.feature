Feature: MCP Dev.to draft publishing service
  The system exposes a local MCP service that can create draft Dev.to posts.
  The service runs as an independent STDIO subprocess owned by the orchestrator.

  @REQ-MCP-DEVTO-001 @REQ-MCP-DEVTO-007
  Scenario: Start the MCP Dev.to service over STDIO
    Given DEVTO_API_KEY is available in the process environment
    When the MCP Dev.to service starts
    Then it exposes an MCP server named "mcp-devto"
    And it uses version "1.0.0"
    And it communicates over STDIO
    And normal protocol traffic is kept on STDIO
    And startup messages are written outside protocol stdout

  @REQ-MCP-DEVTO-002
  Scenario: Fail fast when the Dev.to API key is missing
    Given DEVTO_API_KEY is not available
    When the MCP Dev.to service starts
    Then the service exits with a startup failure
    And no MCP tool execution is available
    And the failure explains that DEVTO_API_KEY is not set

  @REQ-MCP-DEVTO-003
  Scenario: Expose the create_dev_post tool schema
    Given the MCP Dev.to service is running
    When an MCP client lists available tools
    Then the tool list includes "create_dev_post"
    And the tool requires a post title
    And the tool requires Markdown body content
    And the tool requires a list of tags

  @REQ-MCP-DEVTO-003
  Scenario: Reject invalid draft post input before calling Dev.to
    Given the MCP Dev.to service is running
    And a create_dev_post request is missing required fields
    When the tool is called
    Then the request is rejected
    And no Dev.to article API request is sent
    And the MCP result reports an error

  @REQ-MCP-DEVTO-004
  Scenario: Create a Dev.to draft from valid tool input
    Given the MCP Dev.to service is running
    And the tool input contains a title, Markdown body, and tags
    When create_dev_post is called
    Then the service sends a Dev.to article creation request
    And the request contains the given title
    And the request contains the given Markdown body
    And the request contains the given tags
    And the article is created as an unpublished draft

  @REQ-MCP-DEVTO-005
  Scenario: Return a success result after Dev.to accepts the draft
    Given Dev.to accepts the article creation request
    When create_dev_post completes
    Then the MCP result reports success
    And the result includes the Dev.to response details

  @REQ-MCP-DEVTO-006
  Scenario: Return an MCP error when Dev.to rejects the request
    Given Dev.to returns a non-success response
    When create_dev_post completes
    Then the MCP result reports an error
    And the error includes the upstream failure message
