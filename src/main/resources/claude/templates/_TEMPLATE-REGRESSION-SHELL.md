# Regression Shell Report — {{STORY_ID}}

**Story:** {{STORY_ID}}
**Epic:** {{EPIC_ID}}
**Mode:** {{REGRESSION_MODE}}
**Generated:** {{GENERATED_AT}}
**Gate:** {{GATE_STATUS}} — {{PASSED_COUNT}}/{{TOTAL_COUNT}} scenarios passed

---

## Summary

| Metric | Value |
| :--- | :--- |
| Mode | {{REGRESSION_MODE}} |
| Scenarios file | {{SCENARIOS_FILE}} |
| Total scenarios | {{TOTAL_COUNT}} |
| Passed | {{PASSED_COUNT}} |
| Failed | {{FAILED_COUNT}} |
| Baseline updated | {{BASELINE_UPDATED}} |

---

## Scenario Results

| # | Scenario ID | Type | Status | Duration |
|---|-------------|------|--------|----------|
{{SCENARIO_ROWS}}

---

## Divergences

{{#each DIVERGENCES}}
### {{scenario_id}} — {{status}}

**Command:** `{{command}}`

**Expected (baseline):**
```
{{expected}}
```

**Actual:**
```
{{actual}}
```

**Diff:**
```diff
{{diff}}
```

---
{{/each}}

{{#if NO_DIVERGENCES}}
No divergences detected. All scenarios matched baseline.
{{/if}}

---

## Baseline

- **Baseline file:** `governance/baselines/regression-baseline.json`
- **Baseline hash:** `{{BASELINE_HASH}}`
- **Exempt list:** `governance/baselines/regression-self-baseline.txt`

## Next Steps

{{#if REGRESSION_DETECTED}}
1. Review divergences above — determine if they represent regressions or expected changes.
2. If expected: run `/x-execute-shell-regression-tests {{STORY_ID}} --update-baseline` to record new baseline.
3. If regression: investigate and fix before merging.
{{/if}}

{{#if GATE_PASSED}}
All scenarios matched baseline. Gate passed.
{{/if}}
