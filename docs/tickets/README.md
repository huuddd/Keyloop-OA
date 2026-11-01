# 📋 Ticket Board

> Tổng: **13 tickets** | Small commits, mỗi ticket = 1 commit

## Dependency Graph

```
T01 (Scaffold) ──┬──→ T02 (Docker) ──→ T03 (Flyway)
                  │
                  └──→ T04 (Enums/VOs) ──→ T05 (Entities) ──→ T06 (Aggregate)
                                                                     │
                                                                     ▼
                                           T07 (Repo Interfaces) ◄───┘
                                                     │
                                                     ▼
                  T03 ──────────────────→ T08 (JPA Impls) ◄──── T07
                                                     │
                                          ┌──────────┴──────────┐
                                          ▼                     ▼
                                   T09 (Availability)    T10 (Booking)
                                          │                     │
                                          └──────────┬──────────┘
                                                     ▼
                                            T11 (API Controllers)
                                                     │
                                                     ▼
                                          T12 (Integration Tests)
                                                     │
                                                     ▼
                                          T13 (Observability + README)
```

## Tickets

| # | Ticket | Type | Layer | Status |
|---|--------|------|-------|--------|
| T01 | [Project Scaffold](./T01-project-scaffold.md) | Chore | — | ⬜ |
| T02 | [Docker Compose](./T02-docker-compose.md) | Chore | Infra | ⬜ |
| T03 | [Flyway Migrations](./T03-flyway-schema.md) | Feature | Infra | ⬜ |
| T04 | [Enums + Value Objects](./T04-domain-enums-valueobjects.md) | Feature | Domain | ⬜ |
| T05 | [Domain Entities](./T05-domain-entities.md) | Feature | Domain | ⬜ |
| T06 | [Appointment Aggregate](./T06-domain-aggregate-appointment.md) | Feature | Domain | ⬜ |
| T07 | [Repository Interfaces](./T07-domain-repository-interfaces.md) | Feature | Domain | ⬜ |
| T08 | [JPA Implementations](./T08-infrastructure-jpa.md) | Feature | Infra | ⬜ |
| T09 | [AvailabilityQueryService](./T09-application-availability.md) | Feature | App | ⬜ |
| T10 | [AppointmentAppService](./T10-application-booking.md) | Feature | App | ⬜ |
| T11 | [REST Controllers](./T11-api-controllers.md) | Feature | API | ⬜ |
| T12 | [Integration Tests](./T12-integration-tests.md) | Test | Test | ⬜ |
| T13 | [Observability + README](./T13-observability-readme.md) | Chore | — | ⬜ |

## Layer Order (Bottom → Up)

```
1. Domain    (T04 → T05 → T06 → T07)   — pure Java, no framework
2. Infra     (T03, T08)                  — JPA, Flyway, PostgreSQL
3. App       (T09, T10)                  — use cases, orchestration
4. API       (T11)                       — controllers, exception handler
5. Test      (T12)                       — integration verification
6. Packaging (T13)                       — Docker, README
```
