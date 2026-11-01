---
description: Plan a new feature before ticketing — analyze impact, surface ambiguity, then draft the ticket (same format as docs/tickets/T*.md)
argument-hint: <short description of the feature request>
---

# Plan Ticket

## Purpose

For a feature request not covered by T01-T13, think through impact BEFORE writing Acceptance Criteria — so the ticket that comes out doesn't quietly bake in an architecture or business-rule decision nobody approved. This is the planning step that runs before `/implement-ticket`.

Feature request: $ARGUMENTS

## Workflow

1. **Restate the request** in 1-2 sentences to confirm scope.
2. **Impact analysis** — read `docs/01-analysis.md`, `docs/02-design.md`, and `docs/tickets/README.md`, then answer explicitly:
   - Which layer(s) does this touch (domain / application / infrastructure / api)? Any new aggregate, command/query, or domain event?
   - Does it change the data model (`docs/02-design.md §8` ERD)? If so, does it need a new Flyway migration (`Vn+1__*.sql`, never editing an applied one)?
   - Does it change the API surface (new/changed endpoint, request/response shape, new error code)?
   - Does it touch a business rule NOT already covered by `docs/01-analysis.md §6 Assumptions`? List each one explicitly — don't skip this even if the feature looks simple.
3. **Stop if there's a genuine ambiguity.** If step 2 surfaced a business rule or architecture choice not already settled in the docs, present the open question(s) to the user and get a decision BEFORE drafting Acceptance Criteria. Do not guess and write it into the ticket as if it were already decided.
4. **Check dependencies** — does this depend on an existing ticket (T01-T13 or a previously planned one) that isn't done yet? Note it; does it block anything?
5. **Draft the ticket**, once scope and open questions are resolved, in this format (read 1-2 existing `docs/tickets/T*.md` as reference first):

```markdown
# T<NN> — <Layer>: <Ticket name>

**Type:** Feature | **Priority:** P1/P2/P3
**Depends on:** <tickets>
**Blocks:** <tickets>

## Description
<1-2 sentences>

## Impact
- Layers touched: <...>
- Data model change: <yes/no — new migration Vn needed?>
- API change: <yes/no — new/changed endpoint>
- Business rules confirmed with user: <list, or "none — fully covered by docs/01-analysis.md">

## Acceptance Criteria
- [ ] ...

## Implementation Details
<packages, classes, sample code — only for decisions already settled in step 2/3; if something is still open, write "needs clarification: ..." instead of inventing a decision>

## Commit message
feat(...): ...
```

6. Number it sequentially (next after the current last ticket) and update the dependency graph in `docs/tickets/README.md`.

## Constraints

- Do not skip step 2/3 even for a "simple-looking" feature — a small-looking change can still hide a data-model or business-rule decision.
- Never write an Acceptance Criterion that encodes an undecided business rule as if it were already settled.
- This command plans and drafts the ticket only — it does not implement it. Once the user approves the plan, run `/implement-ticket` on the resulting ticket file.
