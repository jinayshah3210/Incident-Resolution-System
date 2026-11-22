# ResolveIT demo walkthrough (15–20 minutes)

## Pre-flight

- Start PostgreSQL with `docker compose up -d` (or use the default H2 profile).
- Start the Spring Boot API and Vite frontend as in `README.md`.
- Open one browser window per role; use `Password@123` for each seeded account.
- Confirm `/api/health` responds and reset local data if you want a clean run.
- Assign the five suggested speaker areas to team members. Replace names and group number before recording.

## Run of show

1. **Introductions and problem (1 minute per member):** each member names their contribution and the operational issue ResolveIT addresses.
2. **Architecture and OOP tour (3 minutes):** show the React role portals, REST API, `Incident` aggregate, `PortalUser` inheritance, lifecycle guard, repository, and strategy/configuration concepts.
3. **Workflow 1 — report and route (3–4 minutes):** sign in as E101; submit “Payment application is unavailable and users are unable to complete transactions”; show APPLICATION, Payment, score 64/P1, target timers and assignment. Switch to technician and admin windows to show their incident views and notifications.
4. **Workflow 2 — investigate and close (4–5 minutes):** as T201, accept the offer, start work, add a note, request information, resolve with a summary, and approve closure. As E101, approve the resolution. Return to analytics and audit views.
5. **Workflow 3 — intelligence and oversight (3–4 minutes):** describe duplicate blocking and automatic categorisation; show SLA monitoring and admin control pages. Correlation grouping and a full escalation chain are planned extensions in this prototype; do not present them as implemented.
6. **Tests and documentation (2 minutes):** show the Maven test report, run commands, architecture notes and traceability matrix.

## Examiner prompts and short answers

- **Why Java/Spring?** Java makes inheritance, interfaces, encapsulation and runtime polymorphism explicit; Spring supplies dependency injection, REST and JPA.
- **How is invalid lifecycle movement prevented?** `Incident.transition` checks legal next states and returns a structured conflict.
- **How is priority chosen?** Impact × urgency × service criticality; scores of at least 36 become P1.
- **How does assignment work?** Category routing chooses a support engineer; full load-aware dynamic strategies remain future work.
- **What is persisted?** Incident rows use H2 locally or PostgreSQL. Demo users, settings, notifications and audit records are held in memory.
- **What would scale next?** Persist all aggregates, use signed JWTs and strong password hashing, move notifications to a queue, and coordinate scheduled work across instances.

## Human submission checklist

- [ ] Fill in group number and member names in the report and demo plan.
- [ ] Record a 15–20 minute video with every member introducing themselves and explaining a module.
- [ ] Name and upload the video and report to the instructor’s shared location before the deadline.
