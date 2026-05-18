Feature: Application configuration
  The application loads configuration values needed for Slack, LLM, and Dev.to behavior.

  @REQ-CONFIG-001
  Scenario: Load configuration from root and orchestrator dotenv files
    Given a root .env file exists
    And an orchestrator .env file exists
    When the application loads configuration
    Then values from both files are available
    And unknown extra values are ignored
    And later dotenv values can override earlier dotenv values

  @REQ-CONFIG-001
  Scenario: Process environment overrides dotenv files
    Given a value exists in a dotenv file
    And the same value exists in the process environment
    When the application loads configuration
    Then the process environment value is used

  @REQ-CONFIG-002
  Scenario: Require Slack and LLM configuration
    Given configuration is missing SLACK_BOT_TOKEN
    When the application loads configuration
    Then configuration loading fails
    And the failure names the missing value

  @REQ-CONFIG-002
  Scenario: Require all source-mandated settings
    Given configuration is missing one of SLACK_BOT_TOKEN, SLACK_SIGNING_SECRET, or LLM_API_KEY
    When the application loads configuration
    Then configuration loading fails
    And the application does not start normally

  @REQ-CONFIG-003
  Scenario: Default LLM provider to OpenAI
    Given LLM_PROVIDER is not configured
    When the application loads configuration
    Then LLM_PROVIDER is treated as "openai"
