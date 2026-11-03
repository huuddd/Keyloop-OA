# Video Walkthrough Script (~8 minutes)

> Unified Service Scheduler — Keyloop Technical Assessment, Scenario A
> Structure: **Core requirements first (4 min)** → Extensions that go beyond the brief (3.5 min) → Close

---

## [0:00] Intro (30 seconds)

**Screen:** GitHub README (title + description visible)

> "Hi, I'm Huu. I chose Scenario A — the Unified Service Scheduler for automotive dealerships.
>
> The brief asks for three things: resource-constrained booking, real-time availability checking, and a confirmed appointment record. I'll demo each of those first on Swagger UI, then walk through the extensions I built beyond the brief."

---

## PART 1: CORE REQUIREMENTS

---

### [0:30] Core 1 — Resource Constrained Booking (2 minutes)

**Screen:** Swagger UI → POST `/api/v1/appointments`

> "The first requirement is: allow a user to request a service appointment for a specific vehicle, service type, and dealership at a desired time."

**Action:** Fill in the request body:

```json
{
  "customerId": 1,
  "vehicleId": 1,
  "dealershipId": 1,
  "serviceTypeId": 1,
  "startTime": "2026-11-05T09:00:00"
}
```

Set header: `Idempotency-Key: demo-key-001`

**Action:** Execute → Show 201 Created response

> "The system receives the request and automatically:
> - Calculates the end time from the service type's duration — Oil Change is 60 minutes, so 9:00 to 10:00.
> - Finds an available service bay from the dealership's 3 bays.
> - Finds a technician with the right specialization — Oil Change requires GENERAL, so any of the 5 technicians qualifies.
> - Returns the confirmed appointment with all associations."

**Action:** Point to the response fields: `serviceBayId`, `technicianId`, `startTime`, `endTime`, `status: CONFIRMED`

> "The booking is resource-constrained — the system won't double-book a bay or a technician. I'll prove that in the availability demo next."

---

### [2:30] Core 2 — Real-Time Availability Check (1.5 minutes)

**Screen:** Swagger UI → GET `/api/v1/dealerships/1/availability`

> "The second requirement is: before confirming, check availability of both a service bay and a qualified technician for the entire service duration."

**Action:** Set parameters: `dealershipId: 1`, `date: 2026-11-05`, `serviceTypeId: 1`

**Action:** Execute → Show available slots

> "This returns all available time slots for that date — which bays are free, which technicians can do this service type, and what times work. Customers or front-desk staff use this to pick a slot."

**Now demo the constraint enforcement:**

> "Let me prove the system actually prevents double-booking. I have 3 bays. I'll book 3 appointments at the same time to fill them all."

**Action:** POST 3 appointments with different keys (`demo-key-002`, `demo-key-003`, `demo-key-004`), all at `2026-11-06T10:00:00`

> "All 3 returned 201 — each got a different bay. Now watch what happens when I try a 4th:"

**Action:** POST with `demo-key-005`, same time → **409 Conflict**

> "409 Conflict — no bay available. This is enforced at the database level with SELECT FOR UPDATE on service bay rows, not application-level checks. Even under concurrent requests, exactly one thread wins the lock."

---

### [4:00] Core 3 — Confirmed Appointment Record (30 seconds)

**Screen:** Swagger UI → GET `/api/v1/appointments/{id}`

> "The third requirement is: create a persistent appointment record associating customer, vehicle, technician, and service bay."

**Action:** Paste the UUID from Core 1's response → Execute

> "Here's the persisted record — customer John Doe, his Toyota Camry, assigned to Bay A with technician Alex Johnson, Oil Change service, 9:00 to 10:00, status CONFIRMED. All foreign key associations are in PostgreSQL with proper constraints."

**Action:** Optionally show GET `/api/v1/appointments?status=CONFIRMED` (search/pagination)

> "I also built paginated search — filter by status, customer, date range. That covers all 3 core requirements. Now let me show what I added beyond the brief."

---

## PART 2: EXTENSIONS (what sets this apart)

---

### [4:30] Extension 1 — Architecture & Design-First Approach (1.5 minutes)

**Screen:** GitHub README → scroll to Architecture diagram

> "I wrote a full system design document before writing any code — requirements analysis, architecture decisions, technology choices — all in the docs folder."

**Action:** Show the architecture diagram (Clean Architecture, 4 layers)

> "The system follows Clean Architecture with 4 layers:
> - Domain is pure Java — zero framework dependencies. No Spring, no JPA annotations.
> - Application holds CQRS command and query handlers.
> - Infrastructure implements the repository interfaces defined in domain.
> - API is the REST layer.
>
> The dependency rule is enforced by 9 ArchUnit rules that run on every test execution. If anyone adds a Spring import to the domain layer, the build fails."

**Action:** Briefly scroll to show docs/02-design.md exists

> "I also use Result sealed types for business outcomes instead of throwing exceptions, and strongly-typed IDs with an anti-corruption boundary — the handler converts raw Long to CustomerId before calling into the domain."

---

### [6:00] Extension 2 — Full Appointment Lifecycle (30 seconds)

**Screen:** Swagger UI

> "A real dealership doesn't just book — they need to track the full lifecycle. I extended the state machine beyond CONFIRMED:"

**Action:** PATCH `/{id}/check-in` → 204 (CONFIRMED → IN_PROGRESS)

**Action:** PATCH `/{id}/complete` → 204 (IN_PROGRESS → COMPLETED)

> "Check-in when the customer arrives, complete when the service is done. There's also cancel and no-show."

**Action:** Try PATCH `/{id}/check-in` on the COMPLETED appointment → **422**

> "Invalid state transition — you can't check in a completed appointment. The domain enforces the state machine rules."

---

### [6:30] Extension 3 — Testing & Concurrency (1 minute)

**Screen:** Terminal or README test pyramid diagram

> "98 tests across 5 categories: 55 domain unit tests, 18 application handler tests, 9 ArchUnit architecture tests, 3 infrastructure tests, and 13 integration tests using Testcontainers with a real PostgreSQL."

**Action:** Show README double-booking sequence diagram

> "The most important test is the concurrency integration test. It spawns 6 threads competing for 3 service bays simultaneously. Exactly 3 succeed with 201, exactly 3 are rejected with 409. This validates the pessimistic locking strategy.
>
> The lock targets service bay rows, not appointment rows — because when no appointments exist for a time slot, there's nothing to lock. That would be a phantom read vulnerability. Bay rows always exist, so the lock serializes access correctly."

---

### [7:30] Extension 4 — AI Collaboration (1 minute)

**Screen:** README → "Bugs Discovered" section

> "I used AI — Claude Code — as a pair programming junior. I directed architecture and business rules; AI executed the implementation; I verified through testing and review."

> "Integration testing uncovered 3 bugs in AI-generated code:
> 1. The repository method accepted String instead of a Specialization enum — every appointment creation failed with 500.
> 2. The original double-booking prevention locked appointment rows instead of bay rows — the phantom read issue I just described. I directed the fix.
> 3. An error code mapping used substring matching that didn't catch 'NoBayAvailable', returning 400 instead of 409.
>
> These bugs demonstrate why human verification is essential. AI is fast but fallible — the subtle bugs that require domain understanding were caught through testing and review, not generated correctly the first time."

---

## [8:30] Close (20 seconds)

**Screen:** GitHub README (top)

> "To summarize: I fulfilled the 3 core requirements — resource-constrained booking, real-time availability check, and confirmed appointment records. Beyond that, I added Clean Architecture with ArchUnit enforcement, a full appointment lifecycle, 98 tests including concurrency testing, observability with Prometheus and Grafana, and a design-first workflow with AI as a verified pair junior.
>
> Thank you for watching."

---

## Preparation Checklist

- [ ] `docker compose up -d postgres` (PostgreSQL running)
- [ ] `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"` (Java 21)
- [ ] `mvn clean spring-boot:run` (app running on :8080)
- [ ] Open tabs: Swagger UI, GitHub README, integration test file
- [ ] Test all API calls beforehand so you know the UUIDs
- [ ] Recording tool: OBS Studio or Loom (screen + mic)
- [ ] Target: 7-8 minutes. Under 5 = rushed. Over 10 = too long.
