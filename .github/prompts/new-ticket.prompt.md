---
agent: agent
description: 'Draft a new ticket (same format as docs/tickets/T*.md) for a feature not covered by T01-T13'
---

# New Ticket

## Purpose

Draft a new ticket file under `docs/tickets/` for a feature/change described by the user, in the same format as the existing tickets.

## Workflow

1. Read 1-2 files under `docs/tickets/T*.md` to match the format exactly.
2. Check: does this feature touch a business rule not already covered by `docs/01-analysis.md §6 Assumptions`? If so, ask the user first — do not decide and then write it into the Acceptance Criteria as if it were already settled.
3. Write the ticket:

```markdown
# T<NN> — <Layer>: <Ticket name>

**Type:** Feature | **Priority:** P1/P2/P3
**Depends on:** ...
**Blocks:** ...

## Description
...

## Acceptance Criteria
- [ ] ...

## Implementation Details
...

## Commit message
feat(...): ...
```

4. Number it sequentially (T14 if T13 is currently the last), update the dependency graph in `docs/tickets/README.md`.

## Constraints

If the "Implementation Details" section needs sample code but the business logic is still unclear — write "needs clarification: ..." instead of inventing a decision.
