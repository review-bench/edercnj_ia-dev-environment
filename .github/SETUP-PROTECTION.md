# GitHub Branch Protection Setup Guide

This guide documents the required GitHub branch protection configuration for
`ia-dev-environment` as defined by EPIC-0059 (Zero-Bypass Lifecycle Enforcement).

Branch protection is the final layer (bypass surface `K`) of the 4-layer enforcement
stack — it ensures that no merge can bypass CI audits even if an admin attempts it.

## Prerequisites

- `gh` CLI authenticated: `gh auth status`
- `jq` installed: `jq --version`
- Repository admin access (required to modify branch protection)

## Required Status Checks

The canonical list of required check run contexts lives in `audits/required-checks.txt`.
These correspond to GitHub Actions job `name:` fields in `.github/workflows/`.

| Check Run Context          | Script / Command                            | Story  |
|:---------------------------|:--------------------------------------------|:-------|
| `audit-execution-integrity`| `scripts/audit-execution-integrity.sh`      | 0059-0001 |
| `audit-bypass-flags`       | `scripts/audit-bypass-flags.sh`             | EPIC-0058 |
| `audit-phase-gates`        | `scripts/audit-phase-gates.sh`              | EPIC-0055 |
| `audit-task-hierarchy`     | `scripts/audit-task-hierarchy.sh`           | EPIC-0055 |
| `audit-model-selection`    | `scripts/audit-model-selection.sh`          | EPIC-0050 |
| `audit-pr-evidence`        | `scripts/audit-pr-evidence.sh`              | 0059-0007 |
| `audit-baseline-immutability` | `scripts/audit-baseline-immutability.sh` | 0059-0011 |
| `audit-flow-version`       | `scripts/audit-flow-version.sh`             | 0059-0012 |
| `lifecycle-integrity-audit`| `mvn test -Dtest=LifecycleIntegrityAuditTest`| EPIC-0046 |
| `ra9-audit`                | `scripts/audit-ra9.sh`                      | EPIC-0056 |
| `pre-commit-chain`         | format/lint/compile chain                   | 0059-0006 |

## How to Configure Branch Protection

### Step 1: Dry-run (verify payload)

```bash
./scripts/setup-branch-protection.sh --dry-run
```

This prints the JSON payload and lists all 11 required checks without making any API calls.

### Step 2: Apply to develop and main

```bash
./scripts/setup-branch-protection.sh
```

The script is **idempotent** — running it multiple times produces the same result.
You can also target specific branches:

```bash
./scripts/setup-branch-protection.sh --branches develop
./scripts/setup-branch-protection.sh --branches main
./scripts/setup-branch-protection.sh --branches develop,main
```

### Step 3: Verify configuration

```bash
# Check required status checks on develop
gh api repos/edercnj/ia-dev-environment/branches/develop/protection \
  | jq '.required_status_checks.contexts'

# Check enforce_admins
gh api repos/edercnj/ia-dev-environment/branches/develop/protection \
  | jq '.enforce_admins'

# Full protection summary
gh api repos/edercnj/ia-dev-environment/branches/develop/protection | jq '{
  required_checks: .required_status_checks.contexts | length,
  strict: .required_status_checks.strict,
  enforce_admins: .enforce_admins.enabled,
  required_reviews: .required_pull_request_reviews.required_approving_review_count
}'
```

Expected output for the check count query: `["audit-execution-integrity", "audit-bypass-flags", ...]` (11 entries).

## CODEOWNERS Protection

The file `.github/CODEOWNERS` requires `@edercnj` review for changes to:

- `.claude/rules/` — normative rules loaded in every conversation
- `.claude/hooks/` — runtime enforcement hooks
- `scripts/audit-*.sh` — CI audit scripts
- `audits/*-baseline.txt` — immutable audit baselines (RULE-059-04)
- `audits/required-checks.txt` — canonical check list (this guide's source of truth)
- `.github/CODEOWNERS` — self-referential protection
- `.github/pull_request_template.md` — PR evidence template
- `.github/workflows/` — CI workflow definitions

## When to Re-run setup-branch-protection.sh

Re-run the script whenever:

1. A new audit check is added to `audits/required-checks.txt`
2. A GitHub Actions job is renamed (the check context must match exactly)
3. Branch protection settings are accidentally removed via the GitHub UI
4. A new protected branch is added to the project

## Enforcement Stack Reference

```
Camada 1 — Normative     : .claude/rules/ (loaded in every conversation)
Camada 2 — Runtime hook  : .claude/hooks/verify-*.sh (Stop/PreToolUse events)
Camada 3 — CI audit      : scripts/audit-*.sh (GitHub Actions on PR open/sync)
Camada 4 — Branch protect: GitHub required status checks + CODEOWNERS (this guide)
```

Branch protection (Camada 4) is the "glass ceiling": even if all other layers are
bypassed, a merge cannot happen without all required checks passing and CODEOWNERS
approval granted.
