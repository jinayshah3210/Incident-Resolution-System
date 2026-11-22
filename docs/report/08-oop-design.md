# 8. OOP Design and Design Patterns

This section maps every OOP concept and design pattern to the real classes in `backend/src/main/java/com/resolveit`.
Every pattern class carries a one-line comment naming its pattern.

## 8.1 Layered architecture

`web.controller` (thin REST controllers, DTO records only) → `service` (application services, all business rules) →
`domain` (entities with behaviour, State objects) → `repository` (Spring Data, JPA Specifications) → PostgreSQL via Flyway.
Cross-cutting concerns: `exception.GlobalExceptionHandler`, `event.IncidentEventPublisher` (Observer), `scheduler` jobs,
`security.CurrentUserArgumentResolver`.

The original single `ResolveIqService` god class (470 lines) was split into: `AuthService`, `UserService`,
`CatalogService`, `SettingsService`, `IncidentWorkflowService`, `IncidentQueryService`, `AssignmentService`, `SlaService`,
`NotificationService`, `AuditService`, `ReportService`, `ChatService`, `SimilarityService`, `IncidentCorrelationService`
and `escalation.EscalationService`. Controllers no longer return `Map<String,Object>`. They return records from `web.dto`.
In-memory caches of users, notifications, audit and settings were removed, and everything is read from repositories.

## 8.2 OOP concept → class → file

| Concept | Where | File(s) |
|---|---|---|
| Encapsulation | `Incident` has **no public setters**. Status changes only through `classify`, `offerTo`, `acknowledge`, `decline`, `release`, `startWork`, `resolve`, `approveClosure`, `rejectClosure`. `PortalUser` hides the BCrypt hash and lockout counters behind `verifyPassword` / `recordFailedLogin` / `isLocked`. Collections are exposed read-only (`Collections.unmodifiableList/Set`). A test (`IncidentStateTest.incidentHasNoPublicStatusSetter`) enforces this. | `domain/incident/Incident.java`, `domain/user/PortalUser.java` |
| Abstraction | `IncidentWorkflowService.createIncident` hides a 10-step pipeline (validate, duplicate check, create, classify, resolve service, assess, prioritise, SLA, correlate, assign). `SlaService` hides policy lookup and fallback. `AssignmentService` hides eligibility and strategy selection. | `service/IncidentWorkflowService.java`, `service/SlaService.java`, `service/AssignmentService.java` |
| Inheritance (is-a) | `Employee`, `Technician` and `Administrator` extend abstract `PortalUser` (JPA **JOINED** tables `users`, `employees`, `technicians`, `administrators`). `ImpactAnalyzer`, `UrgencyAnalyzer` and `AffectedUsersAnalyzer` extend abstract `KeywordScaleAnalyzer` (template method `defineRules`). `TechnicianReminderHandler`, `TeamLeadHandler`, `AdminHandler` and `ItManagerHandler` extend `EscalationHandler`. Every domain exception extends `ItsmException`. | `domain/user/*`, `strategy/priority/*Analyzer.java`, `escalation/*`, `exception/*` |
| Polymorphism | `PortalUser.getRole()` and `PortalUser.canView(Incident)` are overridden per subclass and used for IDOR protection (`AccessGuard.requireView`) instead of role `if` chains. Strategy beans (`PriorityStrategy`, `AssignmentStrategy`, `CategoryClassifier`), `IncidentState` objects, `NotificationChannel`s and `EscalationHandler`s are all selected and called through their interface at runtime. | `domain/user/*`, `service/AccessGuard.java`, `strategy/*`, `notify/*` |
| Interfaces | `IncidentState`, `PriorityStrategy`, `AssignmentStrategy`, `CategoryClassifier`, `NamedStrategy`, `NotificationChannel`, `MailGateway`, `IncidentObserver`; repositories are interfaces. | |
| Composition | `Incident` owns `IncidentNote`s and `ChatMessage`s (`cascade = ALL, orphanRemoval = true`; constructors are package-private, so they can only be created through `Incident.addNote` / `addChatMessage`). `Feedback` is created only by `Incident.leaveFeedback`. `IncidentWorkflowService` is composed of classifier, priority and analyser strategies plus SLA, assignment, correlation and publisher collaborators injected through its constructor. | `domain/incident/*`, `service/IncidentWorkflowService.java` |
| Association / aggregation | Incident → reporter and technician (by id), Technician → set of skill categories (`technician_skills`), Service ↔ Technician (`ServiceTechnician`), Category → keywords (`CategoryKeyword` value objects). | `domain/catalog/*`, `domain/user/Technician.java` |
| Generics | `StrategyRegistry<T extends NamedStrategy>` is one registry type reused for three strategy families. | `strategy/StrategyRegistry.java` |
| Exception handling | `ItsmException` (errorCode, httpStatus, userMessage) → `ResourceNotFound`, `ServiceNotFound`, `InvalidCredentials`, `AuthenticationRequired`, `AccountLocked`, `UnauthorizedOperation`, `Validation` (field errors), `InvalidStateTransition` (from state + attempted action), `DuplicateIncident` (existing id), `TechnicianUnavailable`, `WorkloadExceeded`, `Conflict`, `MissingSlaConfiguration` (caught with fallback), `ExternalService` (502). One `@RestControllerAdvice` returns a uniform `ApiError {timestamp, status, code, message, path, fieldErrors, details, correlationId}` and maps DB outages to 503 without stack traces. | `exception/*` |
| Collections | `EnumMap` for state objects, role permissions, SLA defaults and user creators. `TreeSet` for skills. `ConcurrentHashMap<String, AtomicInteger>` for round-robin cursors. `PriorityQueue` for retrying unassigned incidents (P1 first). `LinkedHashMap` / `TreeMap` for ordered report data. `Set` for declined technicians. | `factory/IncidentStateFactory.java`, `service/AssignmentService.java`, `strategy/assignment/RoundRobinStrategy.java` |

## 8.3 Design patterns

| Pattern | Applied to | Classes |
|---|---|---|
| **State** | Incident lifecycle. Each status has one class; every action is illegal by default (`InvalidStateTransitionException`) and a state overrides only its legal moves. `Incident` delegates to `IncidentStateFactory.forStatus(status)`. | `IncidentState`, `NewState`, `ClassifiedState`, `AssignedState`, `AcknowledgedState`, `InProgressState`, `ResolvedState`, `VerifiedState`, `ClosedState` |
| **Strategy** | Priority calculation, technician assignment, category classification. The active strategy is read from the `settings` table on every use, so the admin can switch it without a restart. | `PriorityStrategy` (`MultiplicativePriorityStrategy`, `WeightedPriorityStrategy`), `AssignmentStrategy` (`LeastLoadStrategy`, `RoundRobinStrategy`, `SkillExperienceStrategy`), `CategoryClassifier` (`KeywordCategoryClassifier`), `StrategyRegistry` |
| **Observer** | Services publish an `IncidentEvent` after every step instead of calling audit or notification code directly. Each observer runs in its own try/catch, so a failing observer never fails the main transaction. | `IncidentEventPublisher`, `IncidentObserver`, `AuditObserver`, `NotificationObserver`, `SlaObserver` |
| **Factory** | Creating incidents (next `INC` id), notifications (the recipient matrix), users (EnumMap of creators, no role switch) and state objects. | `IncidentFactory`, `NotificationFactory`, `UserFactory`, `IncidentStateFactory` |
| **Adapter** | `EmailChannel` adapts a `Notification` to an email through the `MailGateway` interface. While mail is disabled it writes `email_outbox` rows with status SIMULATED. A gateway failure is recorded as FAILED and raised as `ExternalServiceException`. | `EmailChannel`, `MailGateway`, `LoggingMailGateway` |
| **Strategy (delivery)** | Every notification goes through every enabled `NotificationChannel`. A new channel is one new bean. | `NotificationChannel`, `InAppChannel`, `EmailChannel` |
| **Chain of Responsibility** | Automatic escalation of unacknowledged P1/P2 incidents: technician → team lead → admin → IT manager, one level per elapsed response window, idempotent. | `EscalationHandler`, `TechnicianReminderHandler`, `TeamLeadHandler`, `AdminHandler`, `ItManagerHandler`, `EscalationService` |
| **Repository** | All persistence. The audit repository extends the bare `Repository` marker and declares only `save` and read methods, so no update or delete path exists. | `repository/*`, `AuditEventRepository` |
| **Specification** | Composable search filters behind `GET /api/incidents/search`. | `IncidentSpecifications` |

## 8.4 Priority formula (with affected users)

Inputs are four 1–4 scales: impact and urgency (derived from the text by `ImpactAnalyzer` / `UrgencyAnalyzer`),
service criticality (from the IT service), and affected users (chosen by the employee or derived by `AffectedUsersAnalyzer`:
1 = just me, 2 = a few people, 3 = a team or department, 4 = everyone or customers).

- **Multiplicative (default):** `score = max(impact, affectedUsers) × urgency × criticality` (1–64). P1 ≥ 36, P2 18–35, P3 8–17, P4 ≤ 7.
  The larger of "how broken" and "how many people" defines the blast radius. Example: payment outage 4 × 4 × 4 = 64 → P1.
- **Weighted:** `score = (0.35·impact + 0.25·urgency + 0.25·criticality + 0.15·affectedUsers) / 4 × 100` (25–100).
  P1 ≥ 85, P2 ≥ 65, P3 ≥ 40, else P4.
