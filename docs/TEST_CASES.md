# ResolveIT Test Cases

**How to run:** `cd backend && ./mvnw test` (Windows: `./mvnw.cmd test`).
**Latest real run (22 Nov 2026):** `Tests run: 171, Failures: 0, Errors: 0, Skipped: 0`. All results below come from that run.

| Class | Tests | Kind |
|---|---:|---|
| `domain.IncidentStateTest` | 81 | Unit: every legal (12) and illegal (52) lifecycle move, per-state objects, invariants |
| `domain.UserHierarchyTest` | 7 | Unit: inheritance, polymorphism, BCrypt, lockout, UserFactory |
| `strategy.PriorityStrategyTest` | 16 | Unit: both priority strategies, boundaries, analysers, registry |
| `strategy.AssignmentStrategyTest` | 5 | Unit: least-load, round-robin, skill-experience |
| `strategy.ClassifierAndSimilarityTest` | 5 | Unit: keyword classifier, Jaccard similarity |
| `pattern.ObserverChannelEscalationTest` | 6 | Unit: observer ordering/isolation, email Adapter, network failure, JWT |
| `integration.IncidentWorkflowIntegrationTest` | 20 | Spring + H2 (Flyway, schema validation): pipeline, lifecycle, intelligent features |
| `integration.SlaEscalationIntegrationTest` | 6 | Spring: SLA warning/violation idempotency, escalation chain |
| `integration.ApiSecurityIntegrationTest` | 17 | MockMvc: authentication, authorisation, validation, error mapping, search |
| `integration.AdminManagementIntegrationTest` | 8 | Spring: directory, catalog, settings, categories, reports |

## Documented cases

| ID | Description | Type | Steps | Expected | Actual | Result |
|---|---|---|---|---|---|---|
| T01 | Creating an incident runs the whole pipeline | Normal | E101 submits the payment outage text | INC id, Application, Payment, P1, score 64, ASSIGNED, audit CREATED/CLASSIFIED/PRIORITY_CALCULATED/OFFER_CREATED, technician notified | As expected | Pass |
| T02 | Payment outage scores 64 → P1 | Business rule | Multiplicative strategy with impact 4, urgency 4, criticality 4, affected users 3 | 64, P1 | 64, P1 | Pass |
| T03 | Multiplicative band boundaries | Boundary | Scores 36, 35, 18, 17, 8, 7 | P1, P2, P2, P3, P3, P4 | As expected (9 parameterised cases) | Pass |
| T04 | Weighted strategy keeps the same ordering | Business rule | Four worked examples | Non-increasing scores, 100 = P1, lowest = P4 | As expected | Pass |
| T05 | P1 deadlines are +5 / +30 minutes | Normal | Create P1 | responseDue = created+5m, resolutionDue = created+30m | As expected | Pass |
| T06 | 80 % warning then one violation row | Business rule | Evaluate at 81 % of the window, then after the deadline | APPROACHING + one SLA_WARNING, then VIOLATED + exactly one row | As expected | Pass |
| T07 | Monitor run twice does not duplicate | Business rule | Run `monitorOpenIncidents` twice after both deadlines | 2 violation rows, unchanged notification count, one audit row each | As expected | Pass |
| T08 | NEW → CLOSED rejected | Exception | `transitionTo(CLOSED)` on NEW | InvalidStateTransitionException (fromState NEW, action close) | As expected | Pass |
| T09 | ASSIGNED → RESOLVED rejected | Exception | `resolve()` on ASSIGNED | InvalidStateTransitionException | As expected | Pass |
| T10 | Least-load picks the lowest load; ties go to experience | Business rule | Candidates with loads 3/1/2, then equal loads | T302, then the more experienced T303 | As expected | Pass |
| T11 | Round robin rotates | Business rule | Three selections over two technicians | T301, T303, T301 | As expected | Pass |
| T12 | Nobody eligible → stays unassigned, admin alerted | Exception | Every qualified technician declines | CLASSIFIED, INCIDENT_UNASSIGNABLE audited, admin notified | As expected | Pass |
| T13 | Rejecting an offer reassigns | Business rule | Assigned technician declines | Different technician, ASSIGNED, declined set contains the first | As expected | Pass |
| T14 | Closure needs both approvals; rejection reopens | Business rule | Technician resolves, employee rejects | IN_PROGRESS, technician notified | As expected | Pass |
| T15a | Wrong password and wrong portal give the same 401 | Invalid input | POST /auth/login twice | 401 INVALID_LOGIN, identical message | As expected | Pass |
| T15b | Five failures lock the account | Exception | Five bad logins, then the correct password | Fifth returns 423, then 423 ACCOUNT_LOCKED | As expected | Pass |
| T16 | Employee cannot read another's incident (IDOR) | Authorisation | E102 GETs E101's incident | 403 UNAUTHORIZED_OPERATION; owner gets 200 | As expected | Pass |
| T17a | Employee blocked from admin/technician endpoints | Authorisation | 8 admin/technician GETs with an employee token | 403 each | As expected | Pass |
| T17b | Technician cannot act on someone else's incident | Authorisation | Other technician accepts; employee starts | 403 both | As expected | Pass |
| T18 | Registration ignores a role field | Authorisation | Register with "role":"ADMIN" | Account created as EMPLOYEE with an E id | As expected | Pass |
| T19a | Invalid input returns field errors | Invalid input | POST /incidents with short title, empty description | 400 with fieldErrors.title and fieldErrors.description | As expected | Pass |
| T19b | HTML is stripped | Invalid input | Title/description containing tags and script | Stored text contains no tags | As expected | Pass |
| T20 | Duplicate blocked unless forced | Exception | Same reporter repeats the text; then force=true | 409 DUPLICATE_INCIDENT with existingIncidentId; forced copy records duplicateOf | As expected | Pass |
| T21 | Three similar reports are correlated | Business rule | E101, E102, E301 report VPN drops on Network | Same correlation group, MAJOR_INCIDENT_SUSPECTED once, 2 related | As expected | Pass |
| T22 | Unacknowledged P1 escalates up the chain | Business rule | Evaluate at +6m, +6m10s, +11m, +60m | Level 1 once, then 2, then 4 (TECHNICIAN, TEAM_LEAD, ADMIN, IT_MANAGER); IT manager notified | As expected | Pass |
| T23 | Missing SLA policy falls back | Exception | Delete sla.P1.response.minutes, create P1 | 5-minute default, SLA_POLICY_FALLBACK audited, creation succeeds | As expected | Pass |
| T24 | Unknown service → 404; auto-detection → General IT | Exception | Service "Teleportation"; then unmatched text | ServiceNotFoundException; General IT | As expected | Pass |
| T25 | Audit has no update/delete path | Business rule | Reflect on AuditEventRepository | No delete/update methods; not a CrudRepository | As expected | Pass |
| T26 | Search filters are role-scoped | Normal | id, text, status, priority, category, service, technician, SLA, date range | Correct matches; employee sees only own; bad SLA value → 400 | As expected | Pass |
| T27 | Failing observer does not stop the others | Business rule | Publisher with a throwing observer and a healthy one | No exception; healthy observer receives the event | As expected | Pass |
| T28 | Database down → 503 without stack trace | Exception | Handler receives CannotCreateTransactionException | 503 DATABASE_UNAVAILABLE, generic message, correlation id | As expected | Pass |
| T29 | Network failure on email | Exception | EmailChannel with a failing gateway | Outbox row FAILED, ExternalServiceException NETWORK_FAILURE | As expected | Pass |
| T30 | Feedback once per incident after resolution | Business rule | Before resolve, after resolve, twice, other employee | 409 state, saved 4/5, 409 conflict, 403 | As expected | Pass |
| T31 | Suggested resolutions from similar closed incidents | Normal | Close an Excel incident, open a similar one | Top suggestion is the closed incident | As expected | Pass |
| T32 | Smart recommendation score is transparent | Normal | Recommendations for a P1 | score = 0.4·skill + 0.3·load + 0.2·experience + 0.1·history | As expected | Pass |
| T33 | Strategy switch without restart | Business rule | Set priority.strategy = WEIGHTED | New incident scored 81 → P2 by WEIGHTED | As expected | Pass |
| T34 | New category classifies without code change | Business rule | Add "Facilities" with keywords via the admin service | Next matching incident classified Facilities | As expected | Pass |
| T35 | Technician deactivation reassigns open work | Business rule | Deactivate the assignee | Incident offered to someone else; deactivated user gets 401 | As expected | Pass |
| T36 | Notifications removable only by the owner | Authorisation | DELETE as another user, as owner, clear all | 403, 200, 200, unread 0 | As expected | Pass |
| T37 | Incident has no public status setter | Business rule | Reflect on Incident methods | No public set* method | As expected | Pass |

## Manual browser check (22 Nov 2026, local PostgreSQL 18)

| Flow | Result |
|---|---|
| Employee E101: sign in, create ticket (classified Network, P2, auto-assigned), stepper, chat, notification panel (remove, clear) | Pass |
| Technician T300: accept, start, add note, request information through chat, resolve, approve closure → "waiting for the employee" | Pass |
| Admin A001: ongoing list with recommendation table, analytics charts, scrollable sidebar | Pass |
| Browser console errors during the above | None |
