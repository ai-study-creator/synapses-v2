# AI Workflow

## Purpose

This file defines the repeatable AI-assisted migration workflow.

The workflow is not one-time use and is not tied to a specific language or framework. It can be reused for migrations between versions, frameworks, languages, platforms, or architectures.

## Main workflow

For each selected feature or migration unit:

1. Read the existing source code and understand the current feature behavior.
2. Identify the source technology stack only from the evaluated codebase.
3. Identify the target project conventions only from the target repository.
4. Extract behavior into specifications.
5. Link every requirement to source evidence.
6. Identify unknowns, assumptions, risks, and missing evidence.
7. Generate tests from the approved specifications using the same use cases observed in the source system.
8. Wait for review confirmation before implementation.
9. Generate migrated implementation.
10. Run tests.
11. Compare source and target behavior.
12. Open a PR or prepare the PR-ready change set.
13. Review the PR against specs, tests, and behavior comparison.
14. Update the migration log.

## Workflow gates

The workflow must stop at the following gates unless explicitly approved.

### Gate 1: Source understanding

Before writing specs, AI must identify:

- feature entry points
- public contracts
- business rules
- persistence behavior
- integration behavior
- error handling
- security behavior
- configuration dependencies
- unknowns

### Gate 2: Specification review

Before generating implementation, AI must provide:

- requirement specification
- source evidence for each requirement
- list of unknowns
- list of assumptions
- proposed tests
- migration risks

Implementation must not start before review confirmation.

### Gate 3: Test review

Before implementation is accepted, tests must prove the approved behavior.

Every approved requirement must be mapped to one or more tests.

### Gate 4: Behavior comparison

Before PR approval, source and target behavior must be compared for the selected scenarios.

Compare:

- HTTP status
- response body
- headers where relevant
- validation errors
- database side effects
- messages/events
- external calls
- authentication/authorization behavior
- relevant operational signals

### Gate 5: PR review

The PR must be reviewed against:

- approved specs
- source evidence
- target project conventions
- tests
- behavior comparison
- security impact
- operational impact

## Prohibited workflow

Do not follow this workflow:

```text
source code -> AI rewrite -> migrated code
```

This is unsafe because it skips specification, review, and behavior validation.

## Required workflow

Follow this workflow:

```text
source code
  -> source analysis
  -> source-evidenced specs
  -> human review
  -> tests from specs
  -> migrated implementation
  -> old vs new comparison
  -> PR review
```

## Completion rule

A migration unit is not complete because code compiles.

It is complete only when the approved behavior is implemented, tested, compared, reviewed, and logged.
