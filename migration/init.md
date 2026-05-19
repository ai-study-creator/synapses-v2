# Migration Initialization

```text
Target repository: <target-repository>
Source repository/system: <source-repository>
Migration folder in target repository: <target-repository>/migration/
Output folder: <target-repository>/migration/output/
Slice output folder: <target-repository>/migration/output/slices/
Stack output folder: <target-repository>/migration/output/stack/
Migration owner: <owner>
Reviewers: <reviewers>
Target branch: <target-branch>
Source access method: <local-path / git-url / archive / other>
Target runtime/language guidance: <runtime, language, version constraints>
Target framework/library guidance: <frameworks, libraries, or approved replacements>
Target build/deployment guidance: <build tool, module shape, deployment constraints>
```

## Current Agreement

- Preserve source behavior unless a difference is explicitly recorded in `open-points.md`.
- Keep migration docs lightweight.
- Use templates from `migration/templates/` for generated files.
- Use one compact output file per slice under `migration/output/slices/`.
- Keep global migration helper files in `migration/output/` and slice requirements/specs isolated in `migration/output/slices/`.
- Extract the source technical stack from the configured source path into `migration/output/stack/tech-stack-source.md`.
- Define and approve the target technical migration guidance in `migration/tech-stack-migrated.md` before generating detailed requirements or implementation slices.
- Keep AI guidance under `migration/skills/`.
- Track requirements/tests in `requirement-test-matrix.md`.
- Track progress in `migration-log.md`.

## Initialization Actions

1. Read the source repository from the configured `Source repository/system` path.
2. Extract the technical requirements of the source project into Markdown.
3. Include programming languages, runtimes, frameworks, important libraries, build tools, package managers, service dependencies, integrations, and notable configuration mechanisms.
4. Write the result to `migration/output/stack/tech-stack-source.md`.
5. Use the source stack and the target guidance from this file to draft `migration/tech-stack-migrated.md`.
6. Record target stack replacements, rejected alternatives, version constraints, and open technical decisions.
7. Approve `migration/tech-stack-migrated.md` before generating detailed requirements, slices, or target specs.
8. Record any missing access, unclear ownership, or unavailable source evidence in `open-points.md`.
