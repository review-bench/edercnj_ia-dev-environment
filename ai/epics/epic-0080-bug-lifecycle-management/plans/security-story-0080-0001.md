---
generated-by: x-internal-build-story-plan@0b3ac24e2eef2f8390557b05844ffd36f7176489
generated-at: 2026-05-08T00:27:32Z
story-id: story-0080-0001
---

# Security Assessment — story-0080-0001: /x-create-bug Skill Scaffolding

> **Epic:** EPIC-0080 — Bug Lifecycle Management
> **Date:** 2026-05-08
> **Author:** Security Engineer Agent (AI-assisted)
> **Scope:** `/x-create-bug` SKILL.md, `_TEMPLATE-BUG.md`, `bug-lifecycle.yaml` capability

---

## Risk Level: HIGH

Three HIGH findings and two MEDIUM findings require remediation before implementation begins.
Verdict: **REQUEST CHANGES**.

---

## Threat Surface Inventory

| Surface | Entry Point | Trust Level | Notes |
| :--- | :--- | :--- | :--- |
| `description` arg | CLI user input | Untrusted | Free-text string; 8–120 chars per schema |
| `severity` arg | CLI user input | Semi-trusted | Enum; validated by schema allowlist |
| `scope` arg | CLI user input | Semi-trusted | Enum; validated by schema allowlist |
| Filesystem scan (`ls ai/bugs/`) | Local FS | Trusted | Used to derive next bug ID |
| `mkdir ai/bugs/bug-XXXXXX/` | Local FS | Trusted | Atomic ID allocation |
| `git commit -m "..."` | Shell subprocess | Depends on quoting | Raw description injected if unquoted |
| `gh pr create --title "..."` | Shell subprocess | Depends on quoting | Raw description injected if unquoted |
| Telemetry sink (`events.ndjson`) | Append-only file | Trusted | Description may appear in span attributes |

---

## STRIDE Analysis

### Threat: Description input (`/x-create-bug "<description>"`)

| STRIDE | Applicable | Threat | Mitigation Status |
| :--- | :--- | :--- | :--- |
| **Spoofing** | No | Local dev tool; no identity stake | N/A |
| **Tampering** | Yes | Path traversal in description → slug → branch/folder (CWE-22) | Partially mitigated by slug pipeline (step 3 strips non-`[A-Za-z0-9]`); folder uses numeric ID only — structural mitigation. Branch name validated by regex. See CWE-22 section. |
| **Repudiation** | Low | Developer could deny filing a bug | Mitigated: git commit message contains description; immutable audit trail in git history. |
| **Information Disclosure** | Medium | Description may contain PII (e.g., "John Smith's payment failing"); lands in git history permanently and potentially in telemetry | Partially mitigated by design; no masking requirement documented. See Finding MEDIUM-1. |
| **Denial of Service** | Low | Concurrent invocations with different descriptions may allocate the same bug ID if TOCTOU mitigation is incorrect | Partially mitigated by `flock` + exit 19 BUSY (arch plan resilience table). See Finding HIGH-2. |
| **Elevation of Privilege** | High | Shell injection via raw description passed to `git commit -m` / `gh pr create --title` without explicit quoting requirement | Not mitigated. See Finding HIGH-3. |

---

## CWE-22 Mitigation Analysis (Path Traversal: Description → Slug → Branch/Folder)

### Architecture of the slug pipeline (arch mini-ADR-003, line 108)

```
NFKD-normalize → drop combining marks → replace non-[A-Za-z0-9] with "-"
  → lowercase → collapse "-+" → trim leading/trailing "-" → truncate 40
  → fallback to "untitled" if empty   ← CONFLICT with AC-4
```

### CWE-22 risk on the FOLDER path

The folder created is `ai/bugs/bug-XXXXXX/` where `XXXXXX` is the 6-digit numeric ID.
The slug does NOT appear in the folder name. This is a structural mitigation: the
folder path cannot contain user-controlled path components. CWE-22 on the folder is
**MITIGATED BY DESIGN**.

### CWE-22 risk on the BRANCH name

The branch name is `bug/XXXXXX-<slug>`. The slug is constrained to `[a-z0-9-]{1,40}`
by the pipeline and enforced by `x-create-git-branch`'s branch-name validator regex
`^bug/[0-9]{6}-[a-z0-9-]{1,40}$`. The `bug/` prefix is a literal, not derived from
user input. Path traversal on branch names (e.g., `bug/../master`) requires `/` in
the slug, which is eliminated in step 3 of the pipeline before any path resolution.
CWE-22 on the branch name is **MITIGATED** when the pipeline is correctly implemented.

### Residual risk: empty-slug fallback

When the post-pipeline slug is empty (e.g., input `///`, `....`, all combining marks),
the arch plan falls back to `untitled`, creating `bug/XXXXXX-untitled`. This silently
bypasses AC-4's explicit rejection requirement (`exit 2 ARGS_INVALID`). See HIGH-1.

### Test vectors for the slug pipeline

| Input | Expected slug | Expected action |
| :--- | :--- | :--- |
| `../../../etc/passwd injection` | `etc-passwd-injection` | Continue |
| `///` | *(empty)* | Exit 2 ARGS_INVALID |
| `\windows\system32\cmd` | `windowssystem32cmd` | Continue |
| `$(rm -rf .)` | `rm-rf` | Continue |
| `` `whoami` `` | `whoami` | Continue |
| `....` | *(empty)* | Exit 2 ARGS_INVALID |
| All Unicode combining marks (`̀́`) | *(empty after NFKD+drop)* | Exit 2 ARGS_INVALID |
| `foo\nbar` (newline) | `foo-bar` | Continue |
| 200-char input | truncated to 120 at arg level, then slug ≤ 40 | Continue |
| 7-char input (`abcdefg`) | — | Exit 2 ARGS_INVALID (minLength) |
| `%2e%2e%2fetc` | `2e2e2fetc` | Continue |
| `\x00null` (NUL byte) | `null` | Continue |

---

## Findings

### CRITICAL

*(none)*

### HIGH (must fix before merge)

#### HIGH-1 — Spec Conflict: Empty-Slug Behavior (AC-4 vs Arch mini-ADR-003)

**Location:**
- `arch-story-0080-0001.md` line 108: `"fallback to 'untitled' if empty"`
- `story-0080-0001.md` AC-4 line 88: `"exit code 2 ARGS_INVALID"`

**Description:**
The architecture plan's slug pipeline specifies a `fallback to 'untitled'` when the
normalized slug is empty. AC-4 explicitly requires `exit 2 ARGS_INVALID` for this case
(example input: `///`). If the arch plan is implemented as written, an attacker or
confused developer supplying an all-separator input (e.g., `///`, `....`, all Unicode
combining marks) will silently create `bug/XXXXXX-untitled` instead of receiving the
documented rejection. This bypasses the explicit security gate defined in AC-4.

**Remediation:**
1. Remove `"fallback to 'untitled' if empty"` from mini-ADR-003 in `arch-story-0080-0001.md`.
2. Replace with: `"empty slug after pipeline → exit 2 ARGS_INVALID (per AC-4)"`.
3. Update the resilience table row (line 168): change `"Fallback slug 'untitled'; emit WARNING event"` to `"Exit 2 ARGS_INVALID; stderr: 'description produces empty slug after normalization'"`.
4. Add explicit test case to task-0080-0001-004: input `"///"` → exit 2.

**ASVS mapping:** ASVS L1 V5.1.3 — Input validation failures must return defined error codes.

---

#### HIGH-2 — TOCTOU Mitigation Mis-Described in Architecture Plan

**Location:**
- `arch-story-0080-0001.md` lines 102–103: `"Race is mitigated by branch-name collision detection in x-create-git-branch"`
- `story-0080-0001.md` §5.2 lines 133–139: atomic-mkdir with 5 retries + 50ms backoff

**Description:**
The architecture plan claims TOCTOU is closed at branch creation (branch-name collision
detection). This is incorrect for the common concurrent invocation scenario where two
callers have **different descriptions**. Example: Caller A files `"checkout broken"`
and Caller B files `"login fails"` simultaneously. Both callers scan the `ai/bugs/`
directory, observe `N` existing bugs, and compute candidate ID `N+1`. Both attempt
`mkdir ai/bugs/bug-000N+1/`. One mkdir succeeds; the other fails. Branch names are
`bug/000N+1-checkout-broken` and `bug/000N+1-login-fails` — **different names, no
collision** at the branch layer. If the implementation follows the arch plan's
description (relying on branch collision), the losing mkdir caller will proceed without
a retry and create a bug with a duplicate ID or fail silently.

The story schema (§5.2) correctly specifies the mitigation: atomic mkdir failure →
retry up to 5 times with re-scan. The arch plan's description contradicts this and
creates implementation risk.

**Remediation:**
1. Correct mini-ADR-002 in `arch-story-0080-0001.md`: replace `"Race is mitigated by branch-name collision detection"` with `"Race is mitigated by atomic mkdir: mkdir failure triggers re-scan and retry (up to 5 attempts, 50ms exponential backoff per §5.2)"`.
2. Clarify that branch-name collision guard is a **separate**, secondary protection for idempotent invocations (same description, same ID).
3. Ensure task-0080-0001-003 acceptance criteria include: concurrent invocations with different descriptions must both receive unique bug IDs.

**ASVS mapping:** ASVS L1 V11.1.6 — Race condition protections on resource allocation.

---

#### HIGH-3 — Shell Injection via Raw Description in git/gh Bash Calls

**Location:**
- `story-0080-0001.md` AC-1 line 54: commit message format includes raw description:
  `"bug: scaffold bug-000001 — checkout total wrong when promo applied"`
- `arch-story-0080-0001.md` line 80: skill uses `Bash` tool for git/gh operations

**Description:**
The slug pipeline sanitizes the description into a safe slug for use in branch names
and folder names. However, the **raw description** (pre-slug) is used directly in:
1. `git commit -m "bug: scaffold $BUG_ID — $DESCRIPTION"` (AC-1)
2. `gh pr create --title "bug: scaffold $BUG_ID — $DESCRIPTION"` (implied)

If the Bash tool call passes `$DESCRIPTION` via unquoted shell expansion, a crafted
description containing `$(...)`, backticks, or `"` characters can execute arbitrary
shell commands on the developer's workstation. Example:

```
/x-create-bug '$(cat ~/.ssh/id_rsa > /tmp/leaked)'
```

If the skill body constructs the commit message as:
```bash
git commit -m "bug: scaffold $BUG_ID — $DESCRIPTION"
```
the subshell `$(cat ~/.ssh/id_rsa > /tmp/leaked)` executes.

**Remediation:**
1. Add explicit quoting requirement to task-0080-0001-003 acceptance criteria: "Description MUST be passed to all shell commands via properly quoted variable expansion or `--` argument separator. Never via inline shell concatenation."
2. Require the skill body to use one of these safe patterns:
   - `git commit -m "$(printf 'bug: scaffold %s — %s' "$BUG_ID" "$DESCRIPTION")"` (double-quote with printf)
   - Assign to variable first: `MSG="bug: scaffold ${BUG_ID} — ${DESCRIPTION}"` then `git commit -m "$MSG"` (still vulnerable to description containing `"`)
   - Safest: write message to temp file and use `git commit -F tmpfile`
3. Add shell-injection test vector to task-0080-0001-004: input `'$(echo INJECTED)'` → commit message must contain literal `$(echo INJECTED)`, not the output of the subshell.

**ASVS mapping:** ASVS L1 V5.3.4 — Output encoding for OS command contexts.

---

### MEDIUM (should fix, may be deferred with documented justification)

#### MEDIUM-1 — PII Leakage: Raw Description in Git History and Telemetry

**Location:**
- `story-0080-0001.md` AC-1 line 54: commit message includes raw description
- `arch-story-0080-0001.md` line 155: `skill_end{exit_code, bug_id, branch, pr_url}` — description not listed in `skill_end` event, but unspecified for `phase_start` span attributes

**Description:**
The bug description (e.g., "John Smith's checkout payment failing with card 4111...") is
written verbatim into the git commit message and potentially into telemetry span
attributes. Git commit messages are permanent and synced to GitHub. If the description
contains PII (customer names, card fragments, internal user IDs), this data is
permanently exposed in the repository history.

The telemetry spec (arch line 155) lists `skill_end{bug_id, branch, pr_url}` without
description — this is safe. However, the spec does not explicitly prohibit description
from being included in `phase_start` or other events, leaving it to implementer
discretion.

**Remediation:**
1. Add to task-0080-0001-003 acceptance criteria: "Telemetry events MUST NOT include the raw description as a span attribute. Permitted: `description_length` (integer)."
2. Add a skill output warning: `"NOTE: bug description will appear verbatim in git history. Do not include PII, credentials, or sensitive customer data."` (printed to stdout after scaffold completes).
3. Document in skill frontmatter: `data-classification: Internal — description is written to git history`.

**ASVS mapping:** ASVS L1 V8.3.4 — Personal data must not be logged unless necessary.

---

#### MEDIUM-2 — minLength 8 Enforcement Point Unspecified

**Location:**
- `story-0080-0001.md` §5.2 line 116: `minLength: 8` declared in input schema

**Description:**
The skill input schema declares `minLength: 8` for the description argument, but no
step in the skill body specification or task acceptance criteria specifies where and
how this length check is enforced. If the check is absent from the skill body, a
1-character description like `"a"` passes through and produces a valid single-character
slug, bypassing the documented minimum. This is a defence-in-depth gap.

**Remediation:**
1. Add to task-0080-0001-003 acceptance criteria: "Validate `len(description) >= 8` and `len(description) <= 120` as the first step after argument parsing, before any slug generation. Failure → exit 2 ARGS_INVALID with stderr: `'description too short: minimum 8 characters'`."
2. Add boundary test cases to task-0080-0001-004:
   - 7-char input → exit 2 ARGS_INVALID
   - 8-char input → continue (happy path)
   - 120-char input → continue
   - 121-char input → exit 2 ARGS_INVALID

**ASVS mapping:** ASVS L1 V5.1.1 — All user-controllable input validated for length bounds.

---

### LOW (informational)

#### LOW-1 — JSON stdout Envelope Exposes User-Derived Data Without Encoding Note

**Location:**
- `story-0080-0001.md` §5.3 — stdout envelope includes `scaffoldPath`, `branch`, `bugId`

**Description:**
The JSON stdout envelope contains user-derived values (`branch` includes the slug,
`scaffoldPath` contains the bug ID). If downstream scripts consume this output via
`eval $(x-create-bug ...)` rather than `jq`, secondary shell injection is possible.
This is a documentation gap, not a code defect.

**Remediation:**
Document in skill output spec and README: "stdout is JSON. Parse with `jq`. Never use `eval` on skill output."

---

## 20-Point Security Checklist Results

| # | Check | Result | Notes |
| :--- | :--- | :--- | :--- |
| 1 | Classified data never in logs | PASS | No PII required by design; MEDIUM-1 notes risk |
| 2 | Classified data never stored plain text | N/A | No sensitive data stored |
| 3 | Classified data never returned unmasked | N/A | No API response |
| 4 | Classified data never in trace spans | OPEN | Telemetry spec incomplete; see MEDIUM-1 |
| 5 | Masking functions consistent/irreversible | N/A | No masking layer |
| 6 | All external inputs validated before processing | PARTIAL | minLength enforcement point unspecified; see MEDIUM-2 |
| 7 | Size limits enforced on all input channels | PARTIAL | maxLength 120 declared; enforcement point unspecified |
| 8 | Validation uses allowlists, not denylists | PASS | Slug pipeline allowlist `[A-Za-z0-9]`; enum validation for severity/scope |
| 9 | Bean Validation annotations on DTOs | N/A | CLI skill (Markdown), not Java DTO |
| 10 | SQL injection prevented | N/A | No database |
| 11 | API endpoints protected with auth | N/A | Local CLI skill, no network endpoint |
| 12 | Authorization checks at correct layer | N/A | Local dev tool; authorization = local git access |
| 13 | Credentials/API keys from secrets management | PASS | No hardcoded credentials; `gh` CLI uses OS credential store |
| 14 | Error responses never expose stack traces | PASS | Errors go to stderr with exit code only; JSON envelope on stdout |
| 15 | Catch blocks follow fail-secure | FAIL | Arch plan specifies `untitled` fallback on empty slug instead of fail-secure exit 2; see HIGH-1 |
| 16 | Exception messages contain context, not sensitive data | PASS | Exit codes with short messages; no stack traces |
| 17 | No reflection or dynamic class loading | N/A | Markdown skill, no Java runtime |
| 18 | Containers run as non-root | N/A | Local dev tool, no container |
| 19 | Filesystem read-only where possible | N/A | Local dev tool |
| 20 | Network policies restrict communication | N/A | Local dev tool; only outbound to GitHub via `gh` CLI |

---

## Input Validation Requirements (ASVS Mapping)

| Requirement | ASVS Reference | Status | Finding |
| :--- | :--- | :--- | :--- |
| Description minLength 8 enforced before slug gen | V5.1.1 | OPEN | MEDIUM-2 |
| Description maxLength 120 enforced before slug gen | V5.1.1 | OPEN | MEDIUM-2 |
| Empty slug after normalization → exit 2 | V5.1.3 | FAIL | HIGH-1 |
| Slug allowlist `[a-z0-9-]{1,40}` | V5.1.2 | PASS | arch mini-ADR-003 |
| Branch name regex enforced by downstream skill | V5.1.2 | PASS | `x-create-git-branch` validator |
| Shell quoting for description in git/gh calls | V5.3.4 | FAIL | HIGH-3 |
| Telemetry excludes raw description | V8.3.4 | OPEN | MEDIUM-1 |

---

## TOCTOU Risk Assessment: 6-Digit ID Allocation

### Mechanism per story §5.2 (correct)

```
count = ls ai/bugs/ | grep -cE '^bug-[0-9]{6}$'
candidate = zero-pad(count + 1, 6)
attempt mkdir ai/bugs/bug-${candidate}/
  → success: proceed with candidate
  → failure: re-scan, retry up to 5 times with 50ms exponential backoff
  → 5 failures: exit 1 COUNTER_COLLISION
```

The `mkdir` failure on the losing process forces a re-scan, which now sees `count + 1`
directories, computes a new candidate, and retries with a collision-free ID. This is
the correct TOCTOU mitigation.

### Arch plan claim (incorrect, HIGH-2)

Arch mini-ADR-002 states: "Race is mitigated by branch-name collision detection in
`x-create-git-branch`." This is wrong: branch names are description-dependent.
Two concurrent callers with different descriptions produce different branch names,
so branch collision detection provides NO protection against ID duplication.

### Capacity and exhaustion

- Capacity: 999,999 IDs (6 digits). Sufficient for any realistic usage.
- Exhaustion vector: An automated script creating bugs in a tight loop could exhaust IDs
  in ~17 hours at 1 bug/minute. Not a realistic threat for a developer CLI tool.
- Recommendation: no change needed; log a WARNING if ID > 900,000.

---

## Shell Injection Risk Analysis

### Attack surface

The skill uses the `Bash` allowed-tool to execute `git` and `gh` CLI commands.
The raw `description` argument is included in the commit message (AC-1) and PR title.

### Vulnerable pattern

```bash
git commit -m "bug: scaffold ${BUG_ID} — ${DESCRIPTION}"
```

If DESCRIPTION = `'$(cat /etc/passwd)'`, the shell expands it. Even with double-quotes,
a description containing `"` can break the quoting context.

### Safe patterns (required)

```bash
# Option 1: printf with %s (recommended)
MSG=$(printf 'bug: scaffold %s — %s' "$BUG_ID" "$DESCRIPTION")
git commit -m "$MSG"

# Option 2: temp file (safest, handles all edge cases)
printf 'bug: scaffold %s — %s\n' "$BUG_ID" "$DESCRIPTION" > /tmp/commit-msg-$$
git commit -F /tmp/commit-msg-$$
rm -f /tmp/commit-msg-$$

# Option 3: git -m with -- separator does NOT help here; use option 1 or 2
```

For `gh pr create --title`:
```bash
# Safe
gh pr create --title "$(printf 'bug: %s — %s' "$BUG_ID" "$DESCRIPTION")" --body "..."
```

---

## Data Classification

| Data Element | Classification | Stored Where | Transmission | Retention |
| :--- | :--- | :--- | :--- | :--- |
| Bug description | Internal (potential PII) | git history (permanent), bug.md | Pushed to GitHub | Permanent in git history |
| Bug ID | Internal | `ai/bugs/bug-XXXXXX/`, branch name, PR title | GitHub PR | Until branch deleted |
| Severity / Scope | Internal | bug.md frontmatter | GitHub PR | Until branch deleted |
| Telemetry events | Internal | `events.ndjson` (local) | Not transmitted | Local only |
| GitHub token | Restricted | OS credential store (gh CLI) | HTTPS to GitHub | Per gh CLI config |

---

## Compliance Assessment

No compliance frameworks active (no pci-dss, lgpd, gdpr, hipaa, sox in `settings.json`).
Compliance checks: **NOT APPLICABLE**.

---

## Risk Matrix

| Risk ID | Description | Likelihood | Impact | Risk Level | Owner |
| :--- | :--- | :--- | :--- | :--- | :--- |
| RISK-001 | Empty-slug fallback bypasses AC-4 rejection gate (HIGH-1) | Likely | Moderate | HIGH | Implementer (task-0080-0001-003) |
| RISK-002 | TOCTOU arch plan misleads implementer into omitting mkdir retry (HIGH-2) | Possible | Moderate | HIGH | Arch lead |
| RISK-003 | Shell injection via raw description in git/gh Bash calls (HIGH-3) | Possible | Major | HIGH | Implementer (task-0080-0001-003) |
| RISK-004 | PII in git history via description (MEDIUM-1) | Likely | Minor | MEDIUM | Implementer + docs |
| RISK-005 | minLength 8 not enforced in skill body (MEDIUM-2) | Possible | Minor | MEDIUM | Implementer (task-0080-0001-003) |
| RISK-006 | Downstream `eval` of JSON stdout (LOW-1) | Unlikely | Minor | LOW | Docs only |

---

## Security Test Cases for task-0080-0001-004

The following test cases MUST be added to `CreateBugSlugGenerationTest.java`
(parametrized unit test, NFR-3 specifies ≥ 12 CWE-22 payloads):

### Slug generation tests (CWE-22 payloads)

```java
@ParameterizedTest
@MethodSource("pathTraversalVectors")
void slugGeneration_stripsPathSeparatorsAndUnsafeChars(String input, String expectedSlug) {
    assertThat(SlugGenerator.slugify(input)).isEqualTo(expectedSlug);
}

static Stream<Arguments> pathTraversalVectors() {
    return Stream.of(
        // CWE-22 vectors
        Arguments.of("../../../etc/passwd injection", "etc-passwd-injection"),
        Arguments.of("\\windows\\system32\\cmd",      "windowssystem32cmd"),
        Arguments.of("%2e%2e%2fetc",                  "2e2e2fetc"),
        Arguments.of("foo/../bar",                     "foo-bar"),
        Arguments.of("foo\nbar",                       "foo-bar"),       // newline
        Arguments.of("foo\x00bar",                     "foobar"),        // NUL byte
        // Shell injection (slug safety)
        Arguments.of("$(rm -rf .)",                   "rm-rf"),
        Arguments.of("`whoami`",                       "whoami"),
        Arguments.of("foo;rm -rf .",                  "foo-rm-rf"),
        // Unicode combining marks
        Arguments.of("̀́̂",            ""),               // empty → exit 2
        // Normal cases
        Arguments.of("checkout total wrong when promo applied",
                     "checkout-total-wrong-when-promo"),                 // truncated at 40
        Arguments.of("a-valid-slug-exactly-40-chars-long-x", "a-valid-slug-exactly-40-chars-long-x")
    );
}
```

### Empty-slug rejection tests (AC-4 / HIGH-1)

```java
@ParameterizedTest
@ValueSource(strings = {"///", "....", "̀́", "   ", "\t\n"})
void slugGeneration_emptySlugAfterNormalization_throwsArgsInvalid(String input) {
    assertThatThrownBy(() -> SlugGenerator.slugify(input))
        .isInstanceOf(ArgsInvalidException.class)
        .hasMessageContaining("empty slug");
}
```

### Length boundary tests (MEDIUM-2)

```java
@Test
void descriptionValidation_sevenChars_rejectsWithArgsInvalid() {
    assertThatThrownBy(() -> DescriptionValidator.validate("abcdefg"))
        .isInstanceOf(ArgsInvalidException.class)
        .hasMessageContaining("too short");
}

@Test
void descriptionValidation_eightChars_accepts() {
    assertThatCode(() -> DescriptionValidator.validate("abcdefgh"))
        .doesNotThrowAnyException();
}

@Test
void descriptionValidation_121Chars_rejectsWithArgsInvalid() {
    String tooLong = "a".repeat(121);
    assertThatThrownBy(() -> DescriptionValidator.validate(tooLong))
        .isInstanceOf(ArgsInvalidException.class)
        .hasMessageContaining("too long");
}
```

### Shell injection test (HIGH-3)

```java
@Test
void commitMessage_shellInjectionPayload_literalInMessage() {
    String description = "$(cat /etc/passwd)";
    String msg = CommitMessageBuilder.build("bug-000001", description);
    // Must contain the literal string, not the output of the subshell
    assertThat(msg).contains("$(cat /etc/passwd)");
    // And must not contain content that would indicate subshell execution
    assertThat(msg).doesNotContain("root:");
}
```

---

## Checklist Results Summary

- **Passed:** 8 / 20 applicable checks
- **Failed:** 2 (checks 6, 15 — HIGH-1 and MEDIUM-2)
- **Open:** 2 (checks 4, 7 — MEDIUM-1, MEDIUM-2)
- **N/A:** 8 (checks 2, 3, 5, 9, 10, 11, 17, 18, 19, 20)

---

## Verdict: REQUEST CHANGES

Three HIGH findings require remediation before implementation proceeds:

1. **HIGH-1** — Remove `untitled` fallback from arch mini-ADR-003; implement exit 2 per AC-4.
2. **HIGH-2** — Correct TOCTOU description in arch mini-ADR-002 to specify mkdir-retry as primary guard.
3. **HIGH-3** — Add explicit shell-quoting requirement for raw description in task-0080-0001-003.

Both MEDIUM findings (MEDIUM-1, MEDIUM-2) should be addressed in task-0080-0001-003 and task-0080-0001-004.
LOW-1 is documentation only and may be addressed in task-0080-0001-006.

