# Migration Initialization

```text
Target repository: /home/leonardo/workspaces/synapses-v2
Source repository/system: /home/leonardo/workspaces/synapses
Migration folder in target repository: /home/leonardo/workspaces/synapses-v2/migration/
Output folder: /home/leonardo/workspaces/synapses-v2/migration/output/
Slice output folder: /home/leonardo/workspaces/synapses-v2/migration/output/slices/
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
- Keep AI guidance under `migration/skills/`.
- Track requirements/tests in `requirement-test-matrix.md`.
- Track progress in `migration-log.md`.
