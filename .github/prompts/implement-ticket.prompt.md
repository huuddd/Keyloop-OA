---
agent: agent
description: 'Implement a specific ticket from docs/tickets/ following TDD, domain-first, per CLAUDE.md'
---

# Implement Ticket

## Purpose

Turn a specified ticket from `docs/tickets/T*.md` into code, following the test-before-implementation, domain-first workflow defined in `../CLAUDE.md`.

## Workflow

1. Read `../CLAUDE.md` (if not already in context) — especially the required conventions and guardrails.
2. Read the specified ticket — Description, Acceptance Criteria, Implementation Details.
3. If there is a genuine ambiguity (a business rule not covered by `docs/01-analysis.md §6 Assumptions`) — STOP and ask the user; do not decide unilaterally.
4. Write the test for each Acceptance Criterion FIRST — confirm it fails red (class/method doesn't exist yet).
5. Implement by layer: domain → infrastructure (if needed) → application → api. Do not skip ahead.
6. Run the tests for real — confirm green.
7. Check off `[x]` the Acceptance Criteria that are now complete, directly in the ticket file.
8. Propose a commit message in the "AI-generated / Human-reviewed" format from `CLAUDE.md`.

## Constraints

- Do not change the layer dependency rule.
- Do not add a Maven dependency outside `docs/03-tech-stack.md` without asking.
- Do not interpret an ambiguous business rule.

## Output

A short report: which ticket, which files were created/changed, which tests were written + real pass/fail results, and anything that needs the user's decision.
