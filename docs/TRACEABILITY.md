# Traceability: assignment requirements → implementation → tests

Paths are relative to `backend/src/main/java/com/resolveit` unless they start with `frontend/` or `docs/`.

| Requirement (assignment PDF / team brief) | Implementation | Tests |
|---|---|---|
| Three user roles with login (accounts created by the administrator) | `domain/user/*` (JOINED inheritance), `service/AuthService`, `web/controller/AuthController`, `frontend/src/pages/auth/*` | UserHierarchyTest, ApiSecurityIntegrationTest (T15, T18, demo logins) |
| Incident create / view / update / assign / resolve / close | `service/IncidentWorkflowService`, `service/IncidentQueryService`, `web/controller/IncidentController`, `TechnicianController` | IncidentWorkflowIntegrationTest (T01, full lifecycle) |
| Priority from impact, urgency, business criticality (and affected users); documented algorithm | `strategy/priority/*`, `docs/report/08-oop-design.md` §8.4 | PriorityStrategyTest (T02–T04) |
| At least two assignment strategies, changeable without redesign | `strategy/assignment/LeastLoadStrategy`, `RoundRobinStrategy`, `SkillExperienceStrategy`, `StrategyRegistry`, `settings` table | AssignmentStrategyTest (T10, T11), roundRobinSettingChangesWhoGetsTheOffer |
| SLA calculation, deadline tracking, warnings, violation detection and records | `service/SlaService`, `scheduler/SlaMonitorJob`, `domain/sla/SlaViolation`, `observer/SlaObserver` | SlaEscalationIntegrationTest (T05–T07, T23) |
| Two notification mechanisms, easy to add another | `notify/NotificationChannel`, `InAppChannel`, `EmailChannel` (Adapter), `service/NotificationService` | ObserverChannelEscalationTest (T27, T29) |
| Complete incident history / audit | `domain/audit/AuditEvent` (immutable), `observer/AuditObserver`, `service/AuditService`, `frontend/src/pages/admin/AuditPage.jsx` | T25, fullLifecycleClosesWithBothApprovalsAndCompleteAudit |
| At least five search/filter criteria | `repository/IncidentSpecifications` (9 criteria), `GET /api/incidents/search`, `frontend/src/components/SearchFilterBar.jsx` | ApiSecurityIntegrationTest (T26) |
| At least two intelligent features | Categorisation (`KeywordCategoryClassifier`), automatic escalation (`escalation/*`), smart recommendation (`AssignmentService.recommend`), SLA risk (`SlaStatus.AT_RISK`), duplicate + related detection (`IncidentCorrelationService`, `SimilarityService`), suggested resolutions | T20, T21, T22, T31, T32, ClassifierAndSimilarityTest |
| Persistence | PostgreSQL, Flyway `db/migration/V1__schema.sql`, `V2__seed_reference.sql`, `ddl-auto=validate` | All integration tests run the same migrations on H2 (PostgreSQL mode) |
| Lifecycle with illegal transitions prevented | State pattern `domain/incident/state/*`, `factory/IncidentStateFactory` | IncidentStateTest (81 cases, T08, T09) |
| Dynamic categories without code changes | `domain/catalog/Category`, `CatalogService`, `/api/admin/categories`, `frontend/src/pages/admin/ConfigPage.jsx` | T34 |
| Encapsulation, abstraction, inheritance, polymorphism, interfaces, composition, aggregation, exceptions, collections | See `docs/report/08-oop-design.md` §8.2 | T37, UserHierarchyTest, IncidentStateTest |
| At least two design patterns | State, Strategy, Observer, Factory, Adapter, Chain of Responsibility, Repository, Specification (`docs/report/08-oop-design.md` §8.3, `docs/uml/patterns.puml`) | ObserverChannelEscalationTest, AssignmentStrategyTest, SlaEscalationIntegrationTest |
| Error handling for the PDF list | invalid login (`InvalidCredentialsException` 401), unauthorized (`UnauthorizedOperationException` 403), invalid incident (`ResourceNotFoundException` 404), invalid transition (409), technician unavailable (409), workload exceeded (409), service not found (404), missing SLA config (fallback), duplicate (409), DB failure (503), network failure (502), invalid input (400): `exception/*`, `GlobalExceptionHandler` | T12, T15, T16, T19, T20, T23, T24, T28, T29, manualReassignmentRespectsWorkload, unknownIncidentIs404, invalidTransitionOverHttpIs409 |
| Security basics | BCrypt (`PortalUser`), lockout, HS256 JWT (`security/JwtTokenService`), role + ownership checks (`service/AccessGuard`, `PortalUser.canView`), validation DTOs, CORS, security headers | ApiSecurityIntegrationTest, UserHierarchyTest |
| Different GUI per role; links between options; back button to the dashboard | `frontend/src/utils/roles.js`, `layouts/Workspace.jsx`, `components/ui.jsx` (`PageShell`) | Manual browser check (TEST_CASES.md) |
| Chat, closure handshake, notification panel, reports with charts, admin tiles | `ChatService`, `Incident.approveClosure`, `frontend/src/components/NotificationPanel.jsx`, `pages/admin/Analytics.jsx` | T14, T36, reportsReflectLiveData, manual check |
| At least 15 test cases | 171 automated tests, 37 documented in `docs/TEST_CASES.md` | — |
| Architecture and UML deliverables | `docs/uml/architecture.puml`, `classes.puml`, `patterns.puml`, `lifecycle.puml` | — |
