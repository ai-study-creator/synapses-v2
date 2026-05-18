# AI Feature Check

## Role

You validate whether a migrated feature correctly implements the approved specification and preserves the source behavior.

This validation is language- and framework-agnostic. It applies to any migration target.

## Validation inputs

Use the following inputs:

- approved requirement specs
- source evidence
- generated or written tests
- migrated implementation
- source behavior examples
- target behavior results
- approved deviations
- unknowns and risks

## Validation checklist

### Requirement coverage

- Every approved requirement has implementation.
- Every approved requirement has at least one mapped test.
- No implemented behavior exists outside the approved scope unless documented.
- No source behavior was silently removed.

### API or external contract coverage

Validate where applicable:

- endpoints/routes/commands/topics/jobs
- request shape
- response shape
- status codes
- headers
- serialization format
- error format
- authentication
- authorization
- backwards compatibility

### Business behavior coverage

Validate:

- business rules
- edge cases
- validation rules
- conditional branches
- default values
- calculations
- state transitions
- idempotency rules
- duplicate handling
- fallback behavior

### Persistence and side effects

Validate:

- database reads
- database writes
- transactions
- constraints
- migrations
- emitted events
- consumed messages
- external service calls
- scheduled behavior

### Error behavior

Validate:

- exceptions
- error mapping
- error codes
- error messages where contractually relevant
- retry behavior
- fallback behavior
- timeout behavior

### Operational behavior

Validate where relevant:

- logs
- metrics
- traces
- configuration
- secrets handling
- health checks
- readiness/liveness behavior
- performance-sensitive paths

## Old vs new comparison

For each selected scenario, compare source and target behavior.

```markdown
# Behavior Comparison

## Scenario
Name:

## Input
Request/event/job/input:

## Source result
Status/result:
Body/output:
Side effects:

## Target result
Status/result:
Body/output:
Side effects:

## Match
YES/NO

## Difference
Describe difference.

## Decision
- Accept as equivalent
- Fix target implementation
- Update spec
- Approve deviation
```

## Validation result

Return one of:

- `PASS`
- `PASS_WITH_APPROVED_DEVIATIONS`
- `FAIL`
- `BLOCKED_BY_UNKNOWN`

## Failure rule

If behavior differs and no approved deviation exists, validation fails.
