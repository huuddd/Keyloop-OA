---
description: 'Testing conventions for the Unified Service Scheduler — tests are written BEFORE implementation'
applyTo: 'src/test/**/*.java'
---

# Java Testing

Full reference: `../../CLAUDE.md`.

## Core principle: tests are written BEFORE implementation

For every Acceptance Criterion in a ticket, write the corresponding test case BEFORE the class/method it tests exists — the test must fail red first (compile error or assertion failure because nothing is implemented yet). That is the proof the test genuinely exercises behavior, not something written after the fact to match working code.

## Test layers (place in the matching package)

| Package | Kind | Framework | Notes |
|---|---|---|---|
| `test/.../architecture/` | ArchUnit | archunit-junit5 | Write EARLY (as soon as domain + 1-2 other layers exist), not last |
| `test/.../domain/` | Pure unit | JUnit 5 + AssertJ | **No Spring context** — domain is pure Java, tests must run without `@SpringBootTest` |
| `test/.../application/` | Handler + validation | JUnit 5 + Mockito | Mock repository interfaces, verify orchestration logic |
| `test/.../integration/` | Full API | Testcontainers (PostgreSQL) | Real HTTP + real DB — required for the double-booking concurrency test |

## What each core requirement needs (per `docs/01-analysis.md`, do not skip)

| Requirement | Required tests |
|---|---|
| FR1 — Check availability | Correct slot count with no bookings; slots excluded when overlapping an existing appointment; dealership/serviceType not found → 404; date in the past → 400 |
| FR2 — Create appointment (resource-constrained booking) | Happy path (bay + tech free → created); conflict (overlapping slot → `SlotNotAvailable`); no qualified technician → `SlotNotAvailable` |
| Double-booking prevention (BR2) | **Concurrency integration test**: N threads racing to book the same bay/slot — only the number of available bays succeed, the rest receive 409, never a 500 |
| State transitions (FR3-FR6) | Each valid transition succeeds; each invalid transition returns `Appointment.InvalidStateTransition` (422) |
| Idempotency | Same `Idempotency-Key` replayed → identical cached response, no duplicate row created |

## General rules

- Use JUnit 5 timeouts for anything that can hang (concurrency, Testcontainers).
- Domain tests use `Clock.fixed(...)` — never depend on the real system clock.
- Integration tests use real Testcontainers PostgreSQL (not H2) — `FOR UPDATE` behavior must be verified against the real database engine.
- If a refactor changes implementation but not behavior, existing tests must stay green unchanged — if an assertion was genuinely wrong, fix it and state why; never silently weaken it to avoid a failure.
