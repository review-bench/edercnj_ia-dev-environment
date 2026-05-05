# Threat Model — story-0077-0024

**Story:** story-0077-0024 — x-epic-create --from-feature (v5 obrigatório); drops Sections 2/4/8
**Modeled At:** 2026-05-05T22:00:00Z

## Trust Boundaries

```
[Architect (human)] → [x-epic-create SKILL] → [XEpicCreateCommand (CLI)]
                                                        ↓
                                            [FeatureEpicSourceLoader]
                                               ↓              ↓
                                    [FeatureMarkdownParser]  [capability/product files]
                                                        ↓
                                            [EpicFromFeatureArtifactWriter]
                                                        ↓
                                            [ai/epics/epic-NNNN/ (file system)]
```

User-supplied inputs: `--from-feature <path>`, `--capability-file <path>`,
`--product-file <path>`, `--epic-id <NNNN>`, `--output-dir <path>`.

## Threats Identified

| ID | STRIDE | Threat | Likelihood | Impact | Mitigation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| T-01 | Tampering | Path traversal via `--from-feature <path>` or `--capability-file <path>` allowing reads from outside the intended project directory (e.g., `--from-feature ../../etc/passwd`) | Low | High | `FeatureEpicSourceLoader` uses `Path.of(featureFile)` and then `Files.readString()`. No explicit canonicalization + prefix check. The file is read as UTF-8 markdown text — not executed. Impact is limited to file disclosure, not code execution. Hardening: add `Path.normalize().toAbsolutePath().startsWith(baseDir)` check |
| T-02 | Tampering | Markdown content injection in feature/capability/product files embedding template directives or script tags that survive into the generated epic markdown | Very Low | Low | `FeatureMarkdownParser` extracts sections by `## ` heading markers and copies content as-is. No templating engine evaluation occurs during extraction. Generated epic markdown is a plain text file — not rendered server-side, not executed |
| T-03 | Information Disclosure | `--output-dir` pointing to a sensitive path (e.g., `~/.ssh/`) and the generated epic overwriting a file with a colliding name | Very Low | Medium | `EpicFromFeatureArtifactWriter.write()` creates directories and writes files at a caller-supplied path. No guard against sensitive paths. Operator toolchain, not end-user facing — risk is bounded to authorized operator misuse |
| T-04 | Denial of Service | Extremely large feature/capability/product markdown files causing excessive memory allocation during `Files.readString()` | Very Low | Low | No size limit enforced. JVM heap protects against OOM; picocli process exits on OOM. File sizes are bounded by engineering practice (Markdown docs, not binary blobs) |

## Residual Risks

- T-01 (partial): path traversal is an advisory finding. The skill is operator-facing
  (not user-facing over a network), which significantly reduces the attack surface.
  Hardening with explicit canonicalization + base-dir prefix check is recommended as a
  follow-up improvement but does not block this story's GO verdict.
- T-02: accepted as-is. Markdown injection into a generated text file is negligible
  without a server-side rendering step.

## Verdict

**Acceptable** — No threat rises to a severity that blocks the story. The primary concern
(T-01) is an operator-toolchain path disclosure risk, bounded by the fact that the skill
is invoked by authorized architects and CI agents — not exposed over a network. A
follow-up hardening story can introduce the canonicalization guard without blocking this
delivery.
