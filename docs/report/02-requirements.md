# 2. Requirements Specification

## Functional requirements

Users register or sign in to a role portal. Employees report incidents, follow status and provide closure feedback. Technicians accept work, investigate, request information, resolve and participate in closure approval. Administrators monitor incidents, audit events, SLA status, service configuration, strategies and analytics.

The pipeline validates and normalizes a report, checks near duplicates, classifies by weighted keywords, identifies a service, derives impact and urgency, computes priority, determines response and resolution deadlines, assigns a technician, then records events and notifications.

## Non-functional requirements

Use layered APIs, responsive role views, explicit domain encapsulation, persistent incident records, structured failures, secure ownership checks, testable OOP behavior, and reproducible setup. Production authorization and credential storage require the security work tracked in the assumptions register.

## Priority rule

Inputs are four 1-4 scales: impact and urgency (derived from the description), the business criticality of the affected service, and the number of affected users (chosen by the employee or derived from the text). Default (multiplicative) score = max(impact, affected users) × urgency × criticality (range 1-64): P1 >= 36, P2 18-35, P3 8-17, P4 <= 7. Payment outage: 4 × 4 × 4 = 64, P1. The alternative weighted strategy scores (0.35·impact + 0.25·urgency + 0.25·criticality + 0.15·affected users) / 4 × 100: P1 >= 85, P2 >= 65, P3 >= 40. The administrator switches strategy at runtime; see section 8.4.

## Default SLA and lifecycle

| Priority | Response | Resolution |
|---|---:|---:|
| P1 | 5 min | 30 min |
| P2 | 15 min | 2 hours |
| P3 | 1 hour | 8 hours |
| P4 | 4 hours | 24 hours |

Lifecycle: NEW → CLASSIFIED → ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED. Each status is a State object that rejects skipped or terminal transitions with InvalidStateTransitionException (HTTP 409). SLA warnings fire at 80 % of a target (configurable); breaches are recorded once, and unacknowledged P1/P2 incidents escalate technician → team lead → admin → IT manager.
