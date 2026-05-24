# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 2/2 (100.0%)
- **Function parity:** 16/17 matched (target 22) — 94.1%
- **Class/type parity:** 1/1 matched (target 3) — 100.0%
- **Combined symbol parity:** 17/18 matched (target 25) — 94.4%
- **Average inline-code cosine:** 0.64 (function body across 2 matched files)
- **Average documentation cosine:** 0.77 (doc text across 2 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 1 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. client

- **Target:** `lmstudio.Client`
- **Similarity:** 0.48
- **Dependents:** 0
- **Priority Score:** 11705.2
- **Functions:** 15/16 matched (target 21)
- **Missing functions:** `from_host_root`
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_
- **Tests:** 8/9 matched

### 2. lib

- **Target:** `lmstudio.Lib`
- **Similarity:** 0.79
- **Dependents:** 0
- **Priority Score:** 102.1
- **Functions:** 1/1 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present
