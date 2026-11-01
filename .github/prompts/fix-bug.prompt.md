---
agent: agent
description: 'Reproduce a bug with a regression test BEFORE fixing it, identify the real root cause, fix minimally'
---

# Fix Bug

## Workflow

1. Read the bug description/evidence the user provides (description, log, stack trace).
2. If the bug was caught by an already-failing test → read the assertion + stack trace, go to step 4.
3. If the bug was found manually (manual testing, code review) → write a NEW test case that reproduces the bug first, confirm it fails red for real, then go to step 4. Do not skip this step.
4. Identify the real root cause — cross-reference `../CLAUDE.md` (missing Clock injection, Result<T> not used where it should be, missing pessimistic lock, layer-rule violation...). Do not guess — if unsure, say so instead of fixing blindly.
5. Fix the code minimally, exactly at the root cause — do not expand scope.
6. Re-run the test written in step 3 — confirm PASS.
7. Run the full test suite — confirm nothing else broke.
8. Propose a commit message:

```
fix(<module>): <one-line bug description>

Bug: <observed symptom>
Root cause: <verified cause, not guessed>
Fix: <what changed>
Regression test: <new test name>
```

## Constraints

- Do not weaken/delete a test to avoid a failure — if you suspect the test itself is wrong (not the code), explain why and ask before changing it.
- Do not catch-and-swallow an exception unless that is the intended behavior.
- Do not fold unrelated refactoring into the same fix.
