# Assumptions and implementation notes

- PostgreSQL 15+ is the only runtime database. Flyway creates the schema (V1) and reference data (V2); Hibernate validates the mapping at start-up. H2 is used only by the automated tests, in PostgreSQL mode, running the same migrations.
- Demo mode (`DEMO_MODE=true`, the default) inserts missing demo accounts and technician-service links, and generates November 2026 demo incidents when the incidents table is empty. Existing data and passwords are never overwritten.
- Employee accounts are created by administrators (Manage Employees); the website has no public sign-up page.
- Email is simulated: every notification is also written to `email_outbox` with status SIMULATED. Real SMTP and external classification services are not configured; classification uses weighted keyword rules stored in the database.
- SLA clocks run on wall-clock time from incident creation (no business hours or holidays). The `demo` profile runs the SLA clock 60 times faster so breaches and escalations can be shown live.
- When no SLA policy exists for a priority, the built-in defaults (P1 5/30, P2 15/120, P3 60/480, P4 240/1440 minutes) are applied and the administrators are notified.
- Team leads are technicians flagged `team_lead`; they receive escalations rather than automatic assignments. The IT manager is an administrator with level `IT_MANAGER`.
- Logout revocation is held in memory; tokens otherwise expire after four hours.
- Incidents keep the service and category names as historical data, so referenced services are archived rather than deleted, and categories are deactivated rather than removed.
