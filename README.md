# ResolveIT

**ResolveIT** is an Intelligent IT Service Management and Incident Resolution System: Spring Boot 3.3 / Java 17 backend,
React 18 + Vite frontend, PostgreSQL database. It manages the full incident lifecycle
(report → classify → prioritise → assign → investigate → resolve → verify → close) with SLA monitoring, escalation,
notifications, an append-only audit trail, search, reports and several intelligent features.

## Prerequisites

- JDK 17+ (tested on JDK 25), Node.js 20+ and npm
- **PostgreSQL 15+ (required, it is the only runtime database)**, either installed locally or through Docker

## 1. Database

**Option A: local PostgreSQL.** Run once as the `postgres` superuser:

```sql
CREATE ROLE resolveit WITH LOGIN PASSWORD 'resolveit-local';
CREATE DATABASE resolveit OWNER resolveit;
```

**Option B: Docker.** `docker compose up -d`

Connection settings come from environment variables, with defaults matching the commands above:
`DB_HOST=localhost`, `DB_PORT=5432`, `DB_NAME=resolveit`, `DB_USER=resolveit`, `DB_PASSWORD=resolveit-local`.
If your machine already defines a global `DB_PASSWORD`, the role's password must match it (or override it when starting).

On start-up **Flyway** applies `V1__schema.sql` (all tables, constraints, indexes) and `V2__seed_reference.sql`
(categories with classifier keywords, IT services, SLA policy, strategy settings). Hibernate runs with
`ddl-auto=validate`, so every entity is checked against the schema. With `DEMO_MODE=true` (default) the demo accounts
and technician-service links are inserted if missing.

## 2. Backend

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

API at http://localhost:8080/api (health: `/api/health`). Presentation profile with a 60× SLA clock:
`./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo`.

## 3. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

## Demo accounts

All demo accounts use the password `Password@123` and can sign in with their ID or email.

| Role | ID | Email |
|---|---|---|
| Administrator | A001 | admin@resolveit.test |
| Administrators | A002, A003 | ananya.menon@ / kabir.sethi@resolveit.test |
| IT manager (top of escalation chain) | A004 | it.manager@resolveit.test |
| Employee | E101, E102 | e101@ / e102@resolveit.test |
| Employees | E301–E310 | firstname.lastname@resolveit.test |
| Technician | T201, T202 | t201@ / t202@resolveit.test |
| Team lead (technician) | T300 | team.lead@resolveit.test |
| Technicians | T301–T310 | firstname.lastname@resolveit.test |

## Tests

```powershell
cd backend
./mvnw.cmd test
```

171 automated tests in 10 classes: domain (State pattern transitions, user hierarchy), strategies (priority,
assignment, classifier, similarity), patterns (observer isolation, email adapter, JWT), and Spring integration tests
(pipeline, lifecycle, SLA and escalation, HTTP security and errors, admin management). Integration tests use H2 in
PostgreSQL mode with the same Flyway migrations and schema validation as production.

## Project structure

```
backend/src/main/java/com/resolveit/
  config/        clock, strategy registries, security, CORS, scheduling
  security/      JWT service, @CurrentUser resolver
  domain/        user (PortalUser, Employee, Technician, Administrator, Role), incident (Incident aggregate,
                 IncidentStatus, Priority, IncidentNote, ChatMessage, Feedback, state/*), catalog, sla,
                 notification, audit, settings
  strategy/      priority/*, assignment/*, classify/*, StrategyRegistry
  notify/        NotificationChannel, InAppChannel, EmailChannel (Adapter), MailGateway
  event/         IncidentEvent, IncidentEventPublisher, IncidentObserver
  observer/      AuditObserver, NotificationObserver, SlaObserver
  escalation/    EscalationHandler chain (technician, team lead, admin, IT manager)
  factory/       IncidentFactory, NotificationFactory, UserFactory, IncidentStateFactory
  service/       Auth, User, Catalog, Settings, IncidentWorkflow, IncidentQuery, Assignment, Sla, Notification,
                 Audit, Report, Chat, Similarity, IncidentCorrelation
  repository/    Spring Data repositories + IncidentSpecifications
  web/           controller/*, dto/*, mapper/*
  exception/     ItsmException hierarchy + GlobalExceptionHandler
  scheduler/     SlaMonitorJob, AssignmentRetryJob
  seed/          DemoDataSeeder
frontend/src/    api, context, hooks, utils, components, layouts, pages/{auth,employee,shared,admin}
docs/            report sections, UML, API contract
```

See `docs/report/08-oop-design.md` for how each OOP concept and design pattern maps to classes.
