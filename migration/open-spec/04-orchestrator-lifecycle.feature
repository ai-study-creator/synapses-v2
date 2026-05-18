Feature: Orchestrator lifecycle
  The HTTP orchestrator starts, exposes health, owns the MCP subprocess, and shuts down cleanly.

  @REQ-ORCH-LIFE-001
  Scenario: Expose the health endpoint
    Given the orchestrator is running
    When a client requests GET /
    Then the response status is 200
    And the response body is {"message":"Orchestrator Brain is running!"}

  @REQ-ORCH-LIFE-002 @REQ-ORCH-LIFE-003
  Scenario: Start the MCP client during application startup
    Given the orchestrator starts
    When startup hooks run
    Then the orchestrator attempts to start the mcp-devto subprocess
    And the subprocess environment inherits the current process environment
    And DOTENV_CONFIG_QUIET is set to "true" when absent
    And the orchestrator attempts to initialize an MCP client over STDIO

  @REQ-ORCH-LIFE-004
  Scenario: Continue running when MCP startup fails
    Given the MCP subprocess cannot be started or initialized
    When the orchestrator starts
    Then the HTTP application remains available
    And the stored MCP client is empty

  @REQ-ORCH-LIFE-005
  Scenario: Close MCP resources during shutdown
    Given the orchestrator has an active MCP client
    When the orchestrator shuts down
    Then the MCP client lifecycle resources are closed
    And the stored MCP client is cleared
