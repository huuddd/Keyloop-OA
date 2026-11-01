---
description: Reproduce a bug with a regression test BEFORE fixing it, identify the real root cause, fix minimally
argument-hint: <bug description, or paste the error output/stack trace>
---

# Fix Bug

## Required workflow, in order

1. Read the bug description/evidence: $ARGUMENTS. If it's a stack trace, identify the exact class/method/line involved.
2. **Classify how it was found**:
   - Found via `/verify` (an existing test already fails) → read the assertion + stack trace, skip to step 4.
   - Found manually (manual curl/Swagger testing, suspicious code on review) → this means the test suite is missing coverage. Write a NEW test case that reproduces this exact bug first — it must **fail red for real** (proving the bug exists) — then proceed to step 4.
3. Do not skip the regression-test step — "I see the bug so I'll just fix it" is not an acceptable shortcut here.
4. **Identify the real root cause** — read the relevant code, cross-reference `CLAUDE.md`: is this a convention violation (missing `Clock` injection breaking determinism, a business error thrown instead of returned via `Result<T>`, a missing pessimistic lock letting double-booking through, a layer-rule violation...)? Don't guess — if unsure, say "root cause not yet confirmed, needs more logging/debugging" instead of fixing blindly.
5. Fix the code **minimally**, exactly at the root cause — do not expand scope to unrelated areas.
6. Re-run the test from step 2 — confirm it goes from FAIL to PASS.
7. Run `/verify` in full — confirm the fix didn't break anything else (no regression elsewhere).
8. Propose a commit message:

```
fix(<module>): <one-line bug description>

Bug: <observed symptom>
Root cause: <verified cause, not guessed>
Fix: <what changed>
Regression test: <new test name, proving it won't recur>
```

## 1-reprompt rule

If the first fix attempt is wrong (test still fails, or fails differently), the user will reprompt with more detail — the second attempt must get it right. If the second attempt is still wrong, **stop**, report what was tried and why it didn't work, and let the user decide the next direction instead of continuing to guess.

## Do not

- Do not weaken/delete a test to dodge a failure (e.g. changing `assertEquals` to `assertTrue(true)`) — if you suspect the test itself is wrong (not the code), explain clearly and ask the user before changing it.
- Do not catch-and-swallow an exception (silent failure) unless that is the intended behavior.
- Do not fold unrelated refactoring into the same bug-fix pass.
