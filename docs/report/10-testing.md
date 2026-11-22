# 10. Testing

Run with `cd backend && ./mvnw test` (Windows `./mvnw.cmd test`), and `cd frontend && npm run build`.

**Latest run (22 Nov 2026): `Tests run: 172, Failures: 0, Errors: 0, Skipped: 0`**; frontend production build succeeded.

The suite is split into focused classes:

- **Unit tests (no Spring):** `IncidentStateTest` (all 12 legal and 52 illegal lifecycle moves plus invariants), `UserHierarchyTest`, `PriorityStrategyTest`, `AssignmentStrategyTest`, `ClassifierAndSimilarityTest`, `ObserverChannelEscalationTest` (observer isolation, email adapter, network failure, JWT).
- **Integration tests:** Spring Boot on H2 in PostgreSQL mode, running the same Flyway migrations and Hibernate schema validation as production; each test rolls back. They cover `IncidentWorkflowIntegrationTest`, `SlaEscalationIntegrationTest` (pinned clock), `ApiSecurityIntegrationTest` (MockMvc) and `AdminManagementIntegrationTest` (including the November demo seeder).

Coverage by category: normal flows, boundary values (priority bands, SLA warning at 80 %), invalid input (field errors, HTML stripping, weak passwords), every error in the assignment's list (invalid login, unauthorised, invalid incident, invalid transition, technician unavailable, workload exceeded, service not found, missing SLA configuration, duplicate, database failure, network failure, invalid input), authorisation per role (including IDOR), both priority strategies, all three assignment strategies, observers, channels, the escalation chain, SLA idempotency, search filters, similarity and feedback.

37 cases are documented with steps and real results in `docs/TEST_CASES.md`, together with a manual browser check of the employee, technician and administrator flows against PostgreSQL.
