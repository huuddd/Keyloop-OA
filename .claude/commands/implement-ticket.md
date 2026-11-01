---
description: Implement a specific ticket under docs/tickets/ following TDD, domain-first, per CLAUDE.md
argument-hint: <path to ticket, e.g. docs/tickets/T01-project-scaffold.md>
---

Use the `ticket-implementer` subagent to implement the ticket at: $ARGUMENTS

Before starting, confirm with the user whether this ticket depends on another ticket that isn't finished yet (see the dependency graph in `docs/tickets/README.md`) — warn before proceeding if so.

Once `ticket-implementer` reports back, summarize for the user: which files were created/changed, which tests passed, anything that needs review/decision, and a suggested commit message in the "AI-generated / Human-reviewed" format from `CLAUDE.md`.
