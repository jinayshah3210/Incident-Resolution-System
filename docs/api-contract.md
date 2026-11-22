# ResolveIT REST API contract

Base URL `http://localhost:8080/api`. JSON in and out; timestamps are ISO-8601 UTC. Every endpoint except
`/health`, `/auth/login`, `/auth/register` and `/catalog/*` needs `Authorization: Bearer <token>` (HS256 JWT, 4-hour expiry).
Role and ownership rules are enforced on the server (403 otherwise).

## Error shape (every failure)

```json
{ "timestamp": "2026-11-22T09:00:00Z", "status": 409, "code": "DUPLICATE_INCIDENT",
  "message": "Similar open ticket already exists: INC1004", "path": "/api/incidents",
  "fieldErrors": { "title": "Title must be between 5 and 120 characters." },
  "details": { "existingIncidentId": "INC1004" }, "correlationId": null }
```

| Status | Codes |
|---|---|
| 400 | `INVALID_INPUT` (with `fieldErrors`) |
| 401 | `INVALID_LOGIN`, `UNAUTHENTICATED` |
| 403 | `UNAUTHORIZED_OPERATION` |
| 404 | `RESOURCE_NOT_FOUND`, `SERVICE_NOT_FOUND`, `NOT_FOUND` |
| 409 | `INVALID_STATE_TRANSITION`, `DUPLICATE_INCIDENT`, `TECHNICIAN_UNAVAILABLE`, `WORKLOAD_EXCEEDED`, `CONFLICT`, `CONCURRENT_UPDATE` |
| 423 | `ACCOUNT_LOCKED` |
| 500 | `INTERNAL_ERROR` (generic message + `correlationId`, never a stack trace) |
| 502 | `NETWORK_FAILURE` |
| 503 | `DATABASE_UNAVAILABLE` |

## Auth and system

| Method | Path | Body / query | Response |
|---|---|---|---|
| GET | `/health` | | `{status, service, database, time}` |
| POST | `/auth/login` | `{login, password, role}` (login = email or ID; role = EMPLOYEE/TECHNICIAN/ADMIN) | `{token, user}` |
| POST | `/auth/register` | `{name, email, password, department}` (any role field is ignored) | `{token, user}` (EMPLOYEE) |
| GET | `/auth/me` | | `{user}` |
| POST | `/auth/logout` | | `{success}` |
| GET | `/catalog/services` | | `ServiceDto[]` (active) |
| GET | `/catalog/categories` | | `string[]` (active) |
| GET | `/support/contact` | | `{email, phone, hours, message}` |

`user` (UserDto): `{id, name, email, role, department, phone, active, skills?, maxWorkload?, workload?, availability?, experienceYears?, teamLead?, adminLevel?, joinDate?, incidentsReported?, incidentsResolved?}`. Null fields are omitted.

## Incidents

| Method | Path | Who | Notes |
|---|---|---|---|
| POST | `/incidents` | Employee | `{title, description, service?, affectedUsers? (1-4), force?}` → `IncidentDto`; 409 duplicate unless `force` |
| GET | `/incidents/mine?scope=ongoing\|past\|all` | Employee | own incidents |
| GET | `/incidents/search` | All (role-scoped) | `q, status, priority, category, service, technician, slaStatus (ok/at-risk/violated), from, to (ISO), scope` |
| GET | `/incidents/{id}` | Owner / assignee / admin | `{incident, audit[], related[], violations[], escalations[]}` |
| GET | `/incidents/{id}/audit` | Owner / assignee / admin | `AuditEventDto[]` |
| GET / POST | `/incidents/{id}/chat` | Owner / assignee / admin | POST `{content}` → messages `{id, at, senderId, senderName, content}[]` |
| POST | `/incidents/{id}/{action}` | see below | `accept`, `reject {reason?}`, `start`, `note {content}`, `info {content}`, `request-info {content}`, `resolve {summary, rootCause?, steps?}`, `approve`, `reject-closure {reason?}` |
| POST | `/incidents/{id}/closure/approve` and `/closure/reject` | Reporter / assignee | closure handshake; both approvals → VERIFIED → CLOSED |
| POST | `/incidents/{id}/feedback` | Reporter | `{rating 1-5, comment?}`, once, after RESOLVED |
| GET | `/incidents/{id}/suggestions` | Assignee / admin | up to 3 `{publicId, title, resolutionSummary, rootCause, resolutionSteps, similarity}` |
| GET | `/chat/contacts` | All | people sharing an open incident conversation |

Allowed actions: employee `approve, reject-closure, info`; technician `accept, reject, start, note, info, request-info, resolve, approve, reject-closure`; admin none (admin uses the admin endpoints).

`IncidentDto`: `{id, publicId, title, description, reporterId, reporterName, technicianId, technicianName, category, classificationConfidence, serviceName, impact, urgency, criticality, affectedUsers, priorityScore, priorityMaxScore, priority, priorityStrategy, status, createdAt, classifiedAt, assignedAt, acknowledgedAt, startedAt, resolvedAt, closedAt, responseDueAt, resolutionDueAt, responseSla, resolutionSla, slaStatus, slaViolated, responseSlaViolated, resolutionSlaViolated, escalationLevel, resolutionSummary, rootCause, resolutionSteps, technicianApproved, employeeApproved, notes, noteEntries[], correlationGroup, duplicateOf, feedbackRating, feedbackComment}`.

## Technician

| Method | Path | Notes |
|---|---|---|
| GET | `/technician/offers` | incidents offered (ASSIGNED) to me |
| GET | `/technician/incidents?scope=` | my incidents |
| GET | `/technician/workload` | `{current, maximum}` |
| POST | `/offers/{id}/accept`, `/offers/{id}/reject` | same as the incident actions |

## Administrator

| Method | Path | Notes |
|---|---|---|
| GET | `/admin/incidents?scope=` | all incidents |
| PUT / PATCH | `/admin/incidents/{id}` | `{title?, description?, impact?, urgency?, affectedUsers?, category?}`; priority + SLA recomputed |
| POST | `/admin/incidents/{id}/assign` | `{technicianId}`; capacity and service link enforced |
| POST | `/admin/incidents/{id}/unassign` | technician removed, auto-reassigned |
| POST | `/admin/incidents/{id}/advance` | `{status}`; at most two steps forward, never VERIFIED/CLOSED |
| GET | `/admin/incidents/{id}/recommendations` | ranked `{technicianId, name, score, skillMatch, loadScore, experienceScore, historyScore, load, maxWorkload, experienceYears, pastIncidentsOnService, eligible, note}` |
| GET | `/admin/sla-violations` | `[{incident, violations[]}]` (undismissed) |
| POST | `/admin/sla-violations/{id}/dismiss` | `{id}` = incident public id or violation id; record kept |
| GET | `/admin/audit?offset=&limit=&incident=&actor=&action=` | newest first, max 500 per page |
| GET | `/admin/audit/incidents/{incidentId}` | one incident's history (or `SYSTEM`) |
| GET / POST | `/admin/users?role=` / `/admin/users` | directory / create employee or technician `{role, name, email, password, department, skills[], maxWorkload, experienceYears}` |
| PUT | `/admin/users/{id}` | update contact, skills, workload, experience |
| DELETE | `/admin/users/{id}` | soft deactivate (technician work reassigned) |
| POST | `/admin/users/{id}/reactivate` | |
| GET / POST | `/admin/services` | `{name, description, category, criticality, keywords, active}` |
| GET / PUT / DELETE | `/admin/services/{id}` | DELETE archives services referenced by incidents |
| POST / DELETE | `/admin/services/{id}/technicians[/{technicianId}]` | link / unlink a technician |
| GET / POST | `/admin/categories` | `{name, description, active}` |
| PUT / DELETE | `/admin/categories/{id}` | DELETE deactivates (never hard-deleted) |
| PUT | `/admin/categories/{id}/keywords` | `{keywords: {word: weight}}` |
| GET / PUT | `/admin/settings` | `priority.strategy`, `assignment.strategy`, `classifier.strategy`, `sla.warning.percent`, `escalation.enabled`, `sla.P1..P4.response/resolution.minutes` |
| GET | `/admin/strategies` | available strategy keys and names |
| GET | `/admin/reports/overview`, `/admin/reports/months`, `/admin/reports/{yyyy-MM}` | `ReportDto {period, total, open, resolved, closed, critical, slaViolations, slaCompliance, averageResponseMinutes, averageResolutionMinutes, averageRating, feedbackCount, escalations, statuses, priorities, categories, services, technicians, trend[]}` |
| GET | `/admin/reports/technicians/{id}` | per-technician report |

## Notifications

| Method | Path | Notes |
|---|---|---|
| GET | `/notifications?sort=newest\|oldest&unreadFirst=true\|false` | `{id, userId, incidentId, type, title, message, createdAt, read}[]` |
| GET | `/notifications/unread-count` | `{count}` |
| POST | `/notifications/mark-read` | |
| DELETE | `/notifications/{id}` | owner only |
| DELETE | `/notifications` | clear all of mine |

Removed in this version: the stub endpoints `/chat/thread` and `/chat/messages` (the conversation lives under `/incidents/{id}/chat`).
