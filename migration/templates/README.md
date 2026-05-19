# Migration Templates

Use these templates when starting or repeating a migration so each run keeps the same shape even when different people or LLMs generate the artifacts.

## Template Groups

- `initial/`: initial repository-specific configuration.
- `process/`: reusable migration workflow and rules.
- `artifacts/`: generated or continuously updated migration outputs.

## Usage

1. Copy the needed template into the matching migration location.
2. Replace placeholders such as `<source-repository>`, `<target-repository>`, `<slice-name>`, and `<date>`.
3. Keep generated artifact files close to the template shape unless the migration explicitly needs an approved extension.
4. Put slice-specific output files under `migration/output/slices/`.
5. Put stack inventory output files under `migration/output/stack/`.

## Artifact Map

| Generated file | Template |
|---|---|
| `migration/init.md` | `initial/init.md` |
| `migration/migration.md` | `process/migration.md` |
| `migration/open-points.md` | `artifacts/open-points.md` |
| `migration/requirement-test-matrix.md` | `artifacts/requirement-test-matrix.md` |
| `migration/migration-log.md` | `artifacts/migration-log.md` |
| `migration/tech-stack-migrated.md` | `artifacts/tech-stack-migrated.md` |
| `migration/output/system-map.md` | `artifacts/output/system-map.md` |
| `migration/output/requirements.md` | `artifacts/output/requirements.md` |
| `migration/output/migration-plan.md` | `artifacts/output/migration-plan.md` |
| `migration/output/behavior-comparison.md` | `artifacts/output/behavior-comparison.md` |
| `migration/output/stack/tech-stack-source.md` | `artifacts/output/stack/tech-stack-source.md` |
| `migration/output/slices/<slice>.md` | `artifacts/output/slices/slice.md` |
| `migration/open-spec/README.md` | `artifacts/open-spec/README.md` |
| `migration/open-spec/<order>-<feature>.feature` | `artifacts/open-spec/feature.feature` |
