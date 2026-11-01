# Unified Service Scheduler — Copilot Instructions

> Keyloop Technical Assessment — Scenario A. The application is built from scratch using AI assistance under human direction, following design doc → tests → implementation order.
> Full convention/workflow reference: [`../CLAUDE.md`](../CLAUDE.md) — this file is Copilot's summary of the same source of truth.

## Read before writing any code

1. `../docs/01-analysis.md` — FR1-FR8, ERD, API design, business rules, assumptions (ambiguity resolutions live here).
2. `../docs/02-design.md` — architecture, layer responsibilities, key patterns (CQRS, Result<T>, domain events, validation pipeline), deep dives, ERD, error-mapping table.
3. `../docs/03-tech-stack.md` — technology choices + justifications, target project structure.
4. `../docs/tickets/README.md` + `T01..T13` — build order (domain-first dependency graph).

## Tech stack

Java 21 · Spring Boot 3.3.5 · PostgreSQL 16 · Flyway · Maven · JUnit 5 + AssertJ + Mockito · Testcontainers · ArchUnit 1.3.0 · SpringDoc OpenAPI.

## Architecture — Clean Architecture, 4 layers (locked, do not change)

```
api/ -> application/ (CQRS) -> domain/ (pure Java, zero framework)
infrastructure/ -> implements domain repository interfaces (ports)
```

Dependency rule (write the ArchUnit test early, not at the end):
- `api` is never imported by any other layer.
- `application` is only imported by `api` + `infrastructure`.
- `infrastructure` is only imported by `api`.
- `domain` never imports `application`/`infrastructure`/`api`, never depends on `jakarta.persistence..`/`org.springframework..`.
- `*Repository` interfaces live in `domain`; implementations in `infrastructure.persistence.repository`.
- Controllers end with `Controller`; JPA entities end with `JpaEntity` + `@Entity`.

## Required conventions

1. **CQRS** — writes = `Command` + `CommandHandler`; reads = `Query` + `QueryHandler`.
2. **Result<T> for domain business-rule outcomes only** — aggregate methods (`Appointment.create/cancel/checkIn/...`) return `Result<T>`, never throw for business-rule failure. Error codes: `"Entity.Reason"` (e.g. `Appointment.SlotNotAvailable`).
3. **Exceptions remain valid at the application boundary** for not-found/invalid-input (`EntityNotFoundException`→404, bean validation→400) — this is intentional, not a Result<T> violation. A `Result.Failure` from a domain call is translated by the handler to the correct status per `docs/02-design.md §7` (`SlotNotAvailable`→409, `InvalidStateTransition`→422).
4. **Strongly-typed IDs** — handlers convert raw `Long`/`UUID` to `XxxId.of(raw)` immediately at the boundary; domain methods never accept raw IDs.
5. **Clock injection** — never call `LocalDateTime.now()`/`Instant.now()` directly in `domain`/`application`.
6. **RFC 7807** — all errors through `GlobalExceptionHandler` → `ProblemDetail`, per the error-mapping table in `docs/02-design.md`.
7. **Double-booking prevention** — availability checks use native `FOR UPDATE` queries at the repository layer; must have a concurrency integration test.

## Workflow — TDD, following T01→T13

For each ticket: read Acceptance Criteria → write the test first (must fail red) → implement by layer (domain first) → run the test for real to confirm green → check off `[x]` → commit with a message stating "AI-generated:" / "Human-reviewed:" (template in `CLAUDE.md`).

## Guardrails — do not decide unilaterally

Do not change the architecture, do not add a dependency outside `docs/03-tech-stack.md`, do not interpret an ambiguous business rule — if `docs/01-analysis.md §6 Assumptions` doesn't cover it, ask the user before writing code.

## Prompts (`.github/prompts/`)

| Prompt | Use when |
|---|---|
| `implement-ticket.prompt.md` | A specific ticket needs to be turned into code |
| `new-ticket.prompt.md` | A feature not covered by T01-T13 needs to be added |
| `fix-bug.prompt.md` | A bug was found (via `/verify`, manual testing, or review) and needs a regression test + fix |
