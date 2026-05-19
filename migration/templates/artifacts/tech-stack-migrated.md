# Migrated Tech Stack

Status: `<DRAFT / APPROVED>`

This file records the approved technical migration guidance. It must be drafted from the source stack inventory and the target guidance captured during initialization, then approved before detailed requirements, slice specs, or implementation begin.

## Source Stack Summary

See `migration/output/stack/tech-stack-source.md`.

## Target Guidance Inputs

| Input | Value | Source |
|---|---|---|
| Runtime/language guidance | `<runtime, language, version constraints>` | `migration/init.md` |
| Framework/library guidance | `<frameworks, libraries, or approved replacements>` | `migration/init.md` |
| Build/deployment guidance | `<build tool, module shape, deployment constraints>` | `migration/init.md` |
| Other constraints | `<security, hosting, organization, licensing, or compatibility constraints>` | `<source>` |

## Target Stack Summary

| Area | Target choice | Version / constraint | Reason |
|---|---|---|---|
| Runtime | `<runtime>` | `<version>` | `<reason>` |
| Language | `<language>` | `<version>` | `<reason>` |
| Framework | `<framework>` | `<version>` | `<reason>` |
| Serialization | `<library>` | `<version>` | `<reason>` |
| Build tool | `<tool>` | `<version>` | `<reason>` |

## Replacement Map

| ID | Source technology | Target technology | Reason | Decision owner | Status |
|---|---|---|---|---|---|
| `DEC-STACK-001` | `<source>` | `<target>` | `<why this replacement is valid>` | `<owner>` | `<DRAFT / APPROVED>` |

## Compatibility Rules

| Rule ID | Rule | Applies to | Status |
|---|---|---|---|
| `RULE-STACK-001` | `<technical rule that all target implementation must follow>` | `<module, layer, or all>` | `<DRAFT / APPROVED>` |

## Rejected Alternatives

| Alternative | Reason rejected | Decision owner |
|---|---|---|
| `<alternative>` | `<reason>` | `<owner>` |

## Open Technical Decisions

| ID | Decision needed | Impact | Owner | Status |
|---|---|---|---|---|
| `<OPEN-STACK-001>` | `<decision>` | `<impact>` | `<owner>` | `<Open / Resolved>` |

## Notes

- `<important compatibility, runtime, or packaging note>`
