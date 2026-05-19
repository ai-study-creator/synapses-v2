# Migration Initialization

```text
Target repository: /home/leonardo/workspaces/synapses-v2
Source repository/system: /home/leonardo/workspaces/synapses
Migration folder in target repository: /home/leonardo/workspaces/synapses-v2/migration/
Output folder: /home/leonardo/workspaces/synapses-v2/migration/output/
Slice output folder: /home/leonardo/workspaces/synapses-v2/migration/output/slices/
Stack output folder: /home/leonardo/workspaces/synapses-v2/migration/output/stack/
Migration owner:
Reviewers:
Target branch:
Source access method:
```

## Current Agreement

- Preserve source behavior unless a difference is explicitly recorded in `open-points.md`.
- Keep migration docs lightweight.
- Use one compact output file per slice under `migration/output/slices/`.
- Keep global migration helper files in `migration/output/` and slice requirements/specs isolated in `migration/output/slices/`.
- Extract the source technical stack from the configured source path into `migration/output/stack/tech-stack-source.md`.
- Keep AI guidance under `migration/skills/`.
- Track requirements/tests in `requirement-test-matrix.md`.
- Track progress in `migration-log.md`.

## Initialization Actions

1. Read the source repository from the configured `Source repository/system` path.
2. Extract the technical requirements of the source project into Markdown.
3. Include programming languages, runtimes, frameworks, important libraries, build tools, package managers, service dependencies, integrations, and notable configuration mechanisms.
4. Write the result to `migration/output/stack/tech-stack-source.md`.
