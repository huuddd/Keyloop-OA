---
description: 'Clean Architecture + DDD conventions for the Unified Service Scheduler'
applyTo: 'src/main/**/*.java'
---

# Java Development

Full reference: `../../CLAUDE.md`. This file is auto-injected when editing `src/main/**/*.java`.

## Layer rule (ArchUnit-enforced — see `src/test/java/.../architecture/ArchitectureTest.java` once it exists; until then, this IS the spec to write that test from)

- `domain` never imports `application`/`infrastructure`/`api`, never `jakarta.persistence..`/`org.springframework..`.
- `application` never imports `infrastructure`/`api`.
- `infrastructure` is only called by `api`.
- `*Repository` interfaces live in `domain`; implementations live in `infrastructure.persistence.repository`.

## Required patterns

- **CQRS**: write use case → `Command` record + `CommandHandler` in `application/<aggregate>/command`; read use case → `Query` record + `QueryHandler` in `application/<aggregate>/query`.
- **Result<T>** (domain business-rule outcomes only): sealed interface `Success<T>`/`Failure` on aggregate methods (`create`, `cancel`, `checkIn`, `complete`, `markNoShow`). Do NOT use it for not-found/invalid-input at the application boundary — those remain exceptions (see below).
- **Exceptions at the application boundary**: `EntityNotFoundException` (→404 via `GlobalExceptionHandler`), Bean Validation / `IllegalArgumentException` (→400). A domain `Result.Failure` is translated by the handler into the correct mapped outcome (409 for `SlotNotAvailable`, 422 for `InvalidStateTransition`) — see `docs/02-design.md §7` for the full table.
- **Strongly-typed IDs with anti-corruption boundary**: `record AppointmentId(UUID value) {}`-style wrapper. Handlers convert raw `Long`/`UUID` from requests into `XxxId.of(raw)` immediately — domain methods never accept raw IDs. Persistence converters live in `infrastructure.persistence.converter`.
- **Clock injection**: constructors take `java.time.Clock`, call `Instant.now(clock)` — never call `Instant.now()`/`LocalDateTime.now()` bare in `domain`/`application` (tests need `Clock.fixed(...)`).
- **Entity hierarchy**: `Entity<TId>` → `AuditableEntity<TId>` (createdAt/By, updatedAt/By) → `SoftDeletableEntity<TId>` (deleted, deletedAt) → `AggregateRoot<TId>` (domain events list). New entities extend the layer they actually need — don't hand-roll audit fields.
- **RFC 7807**: `GlobalExceptionHandler` (`@RestControllerAdvice`) maps every error type to `ProblemDetail` per the error table in `docs/02-design.md`. Don't invent a custom error shape in a controller.
- **Double-booking prevention**: overlap checks use a native query with `SELECT ... FOR UPDATE` at the repository (`findOverlappingForBayWithLock`, `findOverlappingForTechnicianWithLock`) — never check-then-act without a lock.

## Naming

- Command/Query handler: `<Verb><Noun>CommandHandler` / `<Verb><Noun>QueryHandler`.
- JPA entity: `<Noun>JpaEntity`, annotated `@Entity`.
- Controller: `<Noun>Controller`.
- API DTO: `<Noun>Dto`, implemented as a `record`.

## Do not

- Do not force not-found/invalid-input exceptions into `Result<T>` — only domain business-rule outcomes use it.
- Do not call `LocalDateTime.now()` directly in `domain`/`application`.
- Do not edit an already-applied Flyway migration — only add `Vn+1__*.sql`.
- Do not add a Maven dependency outside `docs/03-tech-stack.md` without asking first.
