Feature: LLM client behavior
  The orchestrator calls an OpenAI-compatible chat completions API and preserves source provider behavior.

  @REQ-LLM-001
  Scenario Outline: Normalize supported provider aliases
    Given LLM_PROVIDER is "<input>"
    When the LLM client is created
    Then the provider is normalized to "<normalized>"

    Examples:
      | input         | normalized    |
      | openai        | openai        |
      | github_models | github_models |
      | github        | github_models |
      | github-models | github_models |

  @REQ-LLM-001
  Scenario: Reject unsupported providers
    Given LLM_PROVIDER is unsupported
    When the LLM client is created
    Then client creation fails
    And the failure says to use "openai" or "github_models"

  @REQ-LLM-002 @REQ-LLM-003 @REQ-LLM-004
  Scenario: Resolve API keys from provider-specific fallbacks
    Given the configured LLM API key is empty
    When the provider is OpenAI
    Then OPENAI_API_KEY can be used as the API key
    When the provider is GitHub Models
    Then GITHUB_MODELS_TOKEN is tried first
    And GITHUB_TOKEN is tried next
    And GH_TOKEN is tried last

  @REQ-LLM-002
  Scenario: Fail when no provider API key can be resolved
    Given no configured API key or provider fallback key is available
    When the LLM client is created
    Then creation fails with "Missing API key for configured LLM provider."

  @REQ-LLM-005 @REQ-LLM-006
  Scenario: Select default models
    Given no model override is configured
    When the provider is OpenAI
    Then the default model is "gpt-4.1-mini"
    When the provider is GitHub Models
    Then the default model is "openai/gpt-4.1"

  @REQ-LLM-005 @REQ-LLM-006
  Scenario: Apply model overrides
    Given AI_MODEL is configured
    When a chat request is prepared
    Then AI_MODEL is used before provider-specific model defaults
    Given AI_MODEL is not configured
    And OPENAI_MODEL or GITHUB_MODELS_MODEL is configured for the provider
    Then the provider-specific model override is used

  @REQ-LLM-007 @REQ-LLM-008
  Scenario: Build provider request configuration
    Given the provider is OpenAI
    When the client prepares a request
    Then the request goes to the OpenAI chat completions URL
    And the request includes bearer authorization
    And the request content type is JSON
    Given the provider is GitHub Models
    When the client prepares a request
    Then the request goes to the configured GitHub Models URL or the default URL
    And the request includes bearer authorization
    And the request includes GitHub accept and API version headers

  @REQ-LLM-009 @REQ-LLM-014
  Scenario: Validate and send chat payloads
    Given a non-empty list of chat messages
    And each message role is system, user, assistant, or tool
    And non-tool messages have non-empty content
    When a chat completion request is sent
    Then the payload includes the model
    And the payload includes messages
    And the payload includes temperature
    And tools are included when supplied
    And tool choice is included when supplied

  @REQ-LLM-010
  Scenario: Retry transient provider failures
    Given the LLM provider returns a transient status or request exception
    When a chat completion request is sent
    Then the client retries up to three attempts
    And the retry delay follows the source backoff

  @REQ-LLM-011
  Scenario: Report non-retried provider errors
    Given the LLM provider returns a non-retried error response
    When the client handles the response
    Then the client raises an LLM API error
    And the error includes the status code
    And the error includes a flattened and truncated response body

  @REQ-LLM-012
  Scenario: Reject invalid JSON success responses
    Given the LLM provider returns a successful response with invalid JSON
    When the client parses the response
    Then the client raises "LLM provider returned invalid JSON."

  @REQ-LLM-013 @REQ-LLM-015 @REQ-LLM-016
  Scenario: Parse valid LLM output
    Given the LLM provider returns choices with an assistant message
    When the client parses the response
    Then the response must include content or tool calls
    And string content is returned as text
    And list content text fields are joined as text
    And tool calls include function names
    And tool call arguments are preserved from objects or strings

  @REQ-PERSONA-001 @REQ-PERSONA-002 @REQ-PERSONA-003
  Scenario: Provide persona prompts
    Given the context is "devto", "twitter", or "slack_summary"
    When a persona prompt is requested
    Then the matching source persona text is returned
    Given the context is unknown
    When a persona prompt is requested
    Then the request fails with "Unknown persona context: <context>"
    Given a profile override is supplied
    When prompt messages are built
    Then the override is used as the system prompt
    And the user prompt is sent as the user message
