# Migration Guide

## Purpose

This migration moves the source system at `/home/leonardo/workspaces/synapses` into the target repository at `/home/leonardo/workspaces/synapses-v2`.

The migration is behavior-preserving. The implementation technology may change, but externally observable behavior must stay the same unless a difference is recorded in `open-points.md` as an approved `DEVIATION`.

## Two-Phase Workflow

First map the whole source system, then implement in planned order.

### Phase 1: Full Discovery And Spec Map

1. Read the whole source repository.
2. Capture system structure in `migration/output/system-map.md`.
3. Capture all source-backed requirements in `migration/output/requirements.md`.
4. Capture implementation order and dependencies in `migration/output/migration-plan.md`.
5. Record unknowns, risks, assumptions, and approved deviations in `open-points.md`.

### Phase 2: Implementation

For each planned slice:

1. Implement target tests and code together.
2. Run tests and compare important source/target behavior.
3. Update `requirement-test-matrix.md`, `open-points.md`, and `migration-log.md`.

## Evidence Rule

Every requirement should point to source evidence such as:

- source file and line
- function, route, handler, tool, command, or job
- configuration
- schema or migration
- existing test
- external integration call

If behavior is not proven by source evidence, mark it as `UNKNOWN`, `ASSUMPTION`, or `RISK` in `open-points.md`.

## Slice Output Shape

Use one compact file per implementation slice when extra detail is needed.

Standard location:

```text
migration/output/slices/<slice>.md
```

Keep global migration helper files in `migration/output/` and keep slice-specific requirements, tests, decisions, and results isolated in `migration/output/slices/`.

```markdown
# Slice: <name>

## Source Evidence
- file:line - behavior

## Requirements
- REQ-001: behavior

## Tests
- TEST-001 -> REQ-001

## Decisions / Open Points
- UNKNOWN/RISK/DEVIATION:

## Result
- Implemented:
- Verified:
- Differences:
```

Existing slice files may be more detailed, but new slices should stay compact and should live under `migration/output/slices/`.

## Files

```text
migration/
  migration.md                  # this guide
  init.md                       # basic migration context
  open-points.md                # unknowns, risks, assumptions, deviations, blockers
  requirement-test-matrix.md    # requirement-to-test status
  migration-log.md              # short progress log
  skills/
    ai-*.md                     # optional AI guidance
  output/
    system-map.md               # Phase 1 source inventory
    requirements.md             # Phase 1 global requirement map
    migration-plan.md           # Phase 1 implementation order
    slices/
      <slice>.md                # optional detailed slice file
```
