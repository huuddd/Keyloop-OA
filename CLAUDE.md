# CLAUDE.md — Unified Service Scheduler

> Keyloop Technical Assessment — Scenario A: The Unified Service Scheduler (Domain: Ownership).
> The application is built from scratch with AI assistance, under human direction and review, following the order: **design doc before code → tests before implementation → implementation with review**.
> This file is read automatically by Claude Code when this directory is opened as the workspace.
> Source of truth for requirements/design: [`docs/01-analysis.md`](./docs/01-analysis.md), [`docs/02-design.md`](./docs/02-design.md), [`docs/03-tech-stack.md`](./docs/03-tech-stack.md). Implementation order: [`docs/tickets/`](./docs/tickets/).

## Mental model — Senior + AI Pair Junior

This principle governs everything below; it is not a slogan.

You (Claude) are a **fast but fallible pair junior**. The user is the **senior engineer** — they own architecture, data model, and business-rule decisions when the spec is ambiguous. You are delegated *execution* against an existing spec, never *decisions* on an open spec.

- **Delegated to AI**: boilerplate generation, writing test bodies for test names already specified in a ticket, implementing a method from an interface/ticket that is already defined, writing migration SQL for a schema already described in `docs/02-design.md`.
- **Never decided by AI** (stop and ask if `docs/01-analysis.md` or the ticket doesn't already answer it):
  - Changing the 4-layer architecture (Clean Architecture — see below, locked in `docs/02-design.md`).
  - Changing the data model from `docs/02-design.md §8`.
  - Adding a Maven dependency outside the list in `docs/03-tech-stack.md`.
  - Interpreting an ambiguous business rule (default service duration, technician skill matching, timezone handling...) — if `docs/01-analysis.md §6 Assumptions` doesn't cover it, STOP and ask instead of guessing.
- **1-reprompt rule**: if the first attempt is wrong or off-convention, the user will reprompt with more detail — the second attempt must get it right; don't repeat the same mistake.
- **Commit discipline**: every commit should state what AI generated vs. what the human reviewed/changed — this is the living "AI Collaboration Narrative" the assessment requires, not something reconstructed afterward. Template:

```
feat(domain): add Appointment aggregate with create() factory

AI-generated: aggregate skeleton + happy-path test from ticket T06.
Human-reviewed: added invariant "endTime must be after startTime" (not stated
in the brief, inferred from the booking business logic) + test case for it.
```

## Read before writing any code

1. [`docs/01-analysis.md`](./docs/01-analysis.md) — FR1-FR8, ERD, API design, business rules, assumptions (read the Assumptions section carefully — this is where every ambiguity resolution is recorded).
2. [`docs/02-design.md`](./docs/02-design.md) — architecture, layer responsibilities, key patterns (CQRS, Result<T>, domain events, validation pipeline), sequence diagrams, deep dives (concurrency, availability algorithm, observability), full ERD, final architecture diagram.
3. [`docs/03-tech-stack.md`](./docs/03-tech-stack.md) — technology choices with justifications, observability stack, target project structure, scalability path.
4. [`docs/tickets/README.md`](./docs/tickets/README.md) + `T01`..`T13` — build order (dependency graph, domain-first).

## Tech stack (per `docs/03-tech-stack.md`)

Java 21 · Spring Boot 3.3.5 · PostgreSQL 16 · Flyway · Maven. Testing: JUnit 5 + AssertJ + Mockito (unit), Testcontainers (integration), ArchUnit 1.3.0 (architecture), Gatling 3.10.5 (load, optional). API docs: SpringDoc OpenAPI/Swagger UI. Observability: Micrometer + Actuator, SLF4J/Logback JSON, `CorrelationIdFilter`.

## Architecture — Clean Architecture, 4 layers (locked in `docs/02-design.md`)

```
api/ -> application/ (CQRS) -> domain/ (pure Java, zero framework)
infrastructure/ -> implements domain repository interfaces (ports)
```

**Dependency rule — write the ArchUnit test early, don't leave it for last**:
- `api` is never imported by any other layer.
- `application` is only imported by `api` and `infrastructure`.
- `infrastructure` is only imported by `api`.
- `domain` never imports `application`/`infrastructure`/`api`, and never depends on `jakarta.persistence..` or `org.springframework..`.
- `*Repository` interfaces live in `domain`; implementations live in `infrastructure.persistence.repository` (adapters).
- Controllers end with `Controller`; JPA entities end with `JpaEntity` and carry `@Entity`.

Per `docs/03-tech-stack.md`, ArchUnit is expected to assert ~149 individual checks across these rules once the architecture test suite is built out (see T-series tickets for where it's introduced).

## Target package layout (build exactly this structure)

```
src/main/java/com/keyloop/scheduler/
├── api/
│   ├── controller/     # AppointmentController, AvailabilityController
│   ├── exception/      # GlobalExceptionHandler (RFC 7807 ProblemDetail)
│   └── request/        # CreateAppointmentRequest
├── application/
│   ├── appointment/
│   │   ├── command/    # CreateAppointmentCommand+Handler, Cancel, CheckIn, Complete, NoShow
│   │   └── query/      # GetAppointmentByIdQuery, SearchAppointmentsQuery, GetAvailableSlotsQuery
│   ├── cqrs/            # Command<R>, Query<R>, CommandHandler, QueryHandler
│   ├── dto/              # AppointmentDto, AvailableSlotDto, PagedResult<T>
│   ├── event/            # domain event handlers
│   ├── port/              # EventPublisher interface
│   └── validation/        # CommandValidator<T>, ValidatingService
├── domain/
│   ├── appointment/       # Appointment aggregate root, AppointmentStatus, TimeSlot, AppointmentErrors, repository
│   │   └── event/          # AppointmentCreatedEvent, Cancelled, CheckedIn, Completed, NoShow
│   ├── servicetype/        # ServiceType, Specialization enum, ServiceTypeRepository
│   ├── customer/            # Customer, Vehicle, typed IDs
│   ├── dealership/           # Dealership, ServiceBay, Technician, repositories
│   └── shared/                # Entity -> AuditableEntity -> SoftDeletableEntity -> AggregateRoot
│                               # Result<T> (sealed: Success|Failure), Error, DomainEvent, typed IDs
└── infrastructure/
    ├── config/          # ClockConfig, JpaAuditConfig
    ├── event/            # DomainEventPublisher (Spring ApplicationEventPublisher)
    ├── filter/            # CorrelationIdFilter, IdempotencyFilter
    ├── job/                # IdempotencyCleanupJob (@Scheduled)
    └── persistence/converter|entity|repository/

src/main/resources/db/migration/   # Flyway V1..V5 — never edit an applied migration, only add Vn+1
src/test/java/com/keyloop/scheduler/
├── architecture/   # ArchUnit — write EARLY, not at the end
├── domain/         # pure unit tests, NO Spring context
├── application/    # handler + validation tests, Mockito
├── infrastructure/ # filter tests
├── integration/    # Testcontainers — full API tests, incl. concurrency
└── loadtest/       # Gatling simulations (optional)
```

## Required conventions

1. **CQRS** — every write use case is a `Command` + `CommandHandler`; every read use case is a `Query` + `QueryHandler`. Place in `application/appointment/command|query`.
2. **Result<T> for domain business-rule outcomes** — aggregate methods (`Appointment.create()`, `.cancel()`, `.checkIn()`, etc.) return `Result<T>` (`Success`/`Failure`) instead of throwing for *business-rule* failures. Error codes follow `"Entity.Reason"` (e.g. `Appointment.SlotNotAvailable`, `Appointment.InvalidStateTransition`).
3. **Exceptions are still used at the application boundary** for not-found and invalid-input cases (`EntityNotFoundException` → 404, `IllegalArgumentException`/Bean Validation → 400) — this is intentional, not a violation of Result<T>. A `Result.Failure` returned from a domain call must be translated by the handler into the correct HTTP outcome per the error-mapping table in `docs/02-design.md §7` (`SlotNotAvailable` → 409, `InvalidStateTransition` → 422). Do not force every exception in the codebase into `Result<T>` — only domain business-rule outcomes.
4. **Strongly-typed IDs with an anti-corruption boundary** — handlers receive raw `Long`/`UUID` from the API/request layer and immediately convert via `XxxId.of(raw)` before calling into the domain; domain methods never accept raw `Long`/`UUID`. Converters for persistence live in `infrastructure.persistence.converter`.
5. **Clock injection** — anything needing "now" takes a `java.time.Clock` via constructor; never call `LocalDateTime.now()`/`Instant.now()` directly in `domain`/`application` (tests must be able to use `Clock.fixed(...)`).
6. **RFC 7807** — every error response goes through `GlobalExceptionHandler`, mapped to `ProblemDetail` per the table in `docs/02-design.md §7`. Don't invent a custom error shape.
7. **Idempotency** — `POST /appointments` requires and respects the `Idempotency-Key` header via `IdempotencyFilter` (24h TTL, cleaned up by `IdempotencyCleanupJob`).
8. **Audit + soft delete** — entities extend `AuditableEntity`/`SoftDeletableEntity` as needed; never hand-roll `createdAt`/`deleted` fields.
9. **Double-booking prevention** — availability/overlap checks go through a native query with `FOR UPDATE` (pessimistic lock) at the repository layer (`findOverlappingForBayWithLock`, `findOverlappingForTechnicianWithLock`). This is core requirement FR2/BR2 — never weaken it to a check-then-act without a lock.
10. **Naming** — handlers: `<Verb><Noun>CommandHandler` / `<Verb><Noun>QueryHandler`; JPA entities: `<Noun>JpaEntity`; controllers: `<Noun>Controller`; tests: `<Class>Test` (unit) or `<Feature>IntegrationTest` (Testcontainers).

## Workflow — design → test → implementation (TDD, ticket-driven, T01→T13)

1. Follow `docs/tickets/T01` → `T13` in order (dependency graph in `docs/tickets/README.md`) — domain before infrastructure before application before API.
2. For each ticket: read Acceptance Criteria → **write the test first** (it must fail red because the class/method doesn't exist yet) → implement → run the test for real to confirm green → check off `[x]` → commit using the "Commit discipline" template above.
3. Write the `ArchitectureTest` (ArchUnit) as soon as domain + 1-2 other layers exist — don't defer it to the end, so every later ticket is automatically guarded against a layer violation.
4. Use `/implement-ticket`, `/new-ticket`, `/fix-bug`, `/verify` (see `.claude/commands/`) to run this workflow repeatedly, one ticket at a time.

## Build / Test / Run (once T01-T02 produce `pom.xml` + `docker-compose.yml`)

```bash
docker compose up -d postgres && ./mvnw spring-boot:run   # local dev
./mvnw test -pl . -Dtest="com.keyloop.scheduler.domain.**"  # fast unit tests, no Docker needed
./mvnw test                                                  # full suite — requires Docker (Testcontainers)
```

App: http://localhost:8080 · Swagger: `/swagger-ui.html` · Health: `/actuator/health`.

## AI Collaboration — assessment context

Keyloop explicitly evaluates "AI Engineering & Verification" — how AI was directed and how its output was verified, not just whether the code runs (see `KeyloopCodingChallange.pdf`). Working principles in this repo:
- Always explain the *decision* behind a change, not just the code — especially for business rules (double-booking, state transitions, priority).
- Never silently change an architecture/pattern decision locked above.
- Every code change ships with a test, and the test is **run for real** before reporting completion — never assume "it should pass."
- `docs/04-genai-collaboration.md` already documents the design-phase narrative. When meaningful implementation work happens, add concrete examples (real commit, what AI did, what the human changed) to `README.md`'s "AI Collaboration Narrative" section (created in ticket T13), following: Strategy → Verification Process → Concrete Examples (real commit hashes, never fabricated).
