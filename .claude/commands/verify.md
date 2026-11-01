---
description: Run build + ArchUnit + full test suite for real, report honest results (never assumed)
---

Run in order, report the REAL result of each command (never assume "it should pass"):

1. `./mvnw compile` — confirm a clean compile.
2. `./mvnw test -Dtest="com.keyloop.scheduler.architecture.ArchitectureTest"` — ArchUnit layer rules.
3. `./mvnw test -pl . -Dtest="com.keyloop.scheduler.domain.**"` — domain unit tests (no Docker needed).
4. If Docker is running: `./mvnw test` — full suite including Testcontainers integration tests.
5. If step 4 can't run (Docker unavailable) — say so explicitly, do not report a fake "pass".

Summary: a short table of each step's PASS/FAIL + test count, and explicitly list which Acceptance Criteria (for the ticket currently in progress) are covered by a test and which are NOT — don't silently skip this.
