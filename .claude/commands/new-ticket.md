---
description: Draft a new ticket (same format as docs/tickets/T*.md) for a feature not covered by T01-T13
argument-hint: <short description of the feature to add>
---

Draft a new ticket file under `docs/tickets/` for: $ARGUMENTS

Required format (read 1-2 existing `docs/tickets/T*.md` files as templates first):

```markdown
# T<NN> — <Layer>: <Ticket name>

**Type:** Feature | **Priority:** P1/P2/P3
**Depends on:** <tickets>
**Blocks:** <tickets>

## Description
<1-2 sentences>

## Acceptance Criteria
- [ ] ...

## Implementation Details
<packages, classes, sample code IF the business logic is unambiguous — if it's still open, write "needs clarification: ..." instead of inventing a decision>

## Commit message
feat(...): ...
```

Before writing, check: does this feature touch a business rule not covered by `docs/01-analysis.md §6 Assumptions`? If so, ask the user first.

Number it sequentially (T14 if T13 is currently last), update the dependency graph in `docs/tickets/README.md`.
