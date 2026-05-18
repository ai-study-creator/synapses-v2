Feature: Runtime and container behavior
  The migrated system can be built, packaged, and run as a JVM/Ktor application while preserving source runtime behavior.

  @REQ-RUNTIME-001
  Scenario: Run the orchestrator on port 8000
    Given the application runtime starts
    When the HTTP server binds
    Then it listens on host "0.0.0.0"
    And it uses port 8000 by default
    And port 8000 is exposed from the container

  @REQ-RUNTIME-002
  Scenario: Map host port 8000 to container port 8000
    Given the system is started with Docker Compose
    When the orchestrator service is created
    Then host port 8000 maps to container port 8000

  @REQ-RUNTIME-003
  Scenario: Include MCP Dev.to while letting the orchestrator own subprocess startup
    Given the runtime image is built
    Then the orchestrator artifact is available
    And the mcp-devto artifact is available
    When the orchestrator starts
    Then it starts mcp-devto as an MCP STDIO subprocess
    And mcp-devto does not need to run as a separate long-lived HTTP container

  @REQ-RUNTIME-001 @REQ-RUNTIME-003
  Scenario: Smoke test the packaged application
    Given the install distributions are built
    When the orchestrator starts with required environment variables
    Then GET / returns {"message":"Orchestrator Brain is running!"}

  @REQ-RUNTIME-001 @REQ-RUNTIME-002
  Scenario: Smoke test the container image
    Given the Docker image is built
    When the container starts with required environment variables
    Then GET / returns {"message":"Orchestrator Brain is running!"}
