# Migration Guide

## Purpose

This migration moves the source system at `<source-repository>` into the target repository at `<target-repository>`.

The migration is behavior-preserving. The implementation technology may change, but externally observable behavior must stay the same unless a difference is recorded in `open-points.md` as an approved `DEVIATION`.

## Migration Workflow

First define the target technical direction, then map the whole source system, then implement in planned order.

### Phase 0: Technical Migration Guidance

1. Read `migration/init.md` for target runtime, language, framework, build, deployment, and constraint guidance.
2. Generate `migration/output/stack/tech-stack-source.md` from the source repository.
3. Generate `migration/tech-stack-migrated.md` with source-to-target replacement decisions.
4. Record unresolved or risky technical decisions in `open-points.md`.
5. Approve `migration/tech-stack-migrated.md` before generating detailed requirements, slice specs, or target implementation.

### Phase 1: Full Discovery And Spec Map

1. Read the whole source repository.
2. Capture system structure in `migration/output/system-map.md`.
3. Use the approved `migration/tech-stack-migrated.md` as target technical guidance.
4. Capture all source-backed requirements in `migration/output/requirements.md`.
5. Capture implementation order and dependencies in `migration/output/migration-plan.md`.
6. Record unknowns, risks, assumptions, and approved deviations in `open-points.md`.

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

## Template Rule

Use `migration/templates/` as the layout source for generated migration files. When a file needs a migration-specific extension, keep the template sections and add the new section at the end unless the team approves a different structure.

## Slice Output Shape

Use one compact file per implementation slice when extra detail is needed.

Standard location:

```text
migration/output/slices/<slice>.md
```

Keep global migration helper files in `migration/output/` and keep slice-specific requirements, tests, decisions, and results isolated in `migration/output/slices/`.

## Files

```text
migration/
  templates/                    # reusable template source
  migration.md                  # this guide
  init.md                       # basic migration context
  open-points.md                # unknowns, risks, assumptions, deviations, blockers
  requirement-test-matrix.md    # requirement-to-test status
  migration-log.md              # short progress log
  tech-stack-migrated.md        # source-to-target stack replacement decisions
  skills/
    ai-*.md                     # optional AI guidance
  output/
    system-map.md               # Phase 1 source inventory
    requirements.md             # Phase 1 global requirement map
    migration-plan.md           # Phase 1 implementation order
    behavior-comparison.md      # source vs target behavior comparison
    stack/
      tech-stack-source.md      # Phase 0 source technical stack inventory
    slices/
      <slice>.md                # optional detailed slice file
  open-spec/
    README.md                   # behavior spec pack overview
    <order>-<feature>.feature   # human-readable behavior specs
```
