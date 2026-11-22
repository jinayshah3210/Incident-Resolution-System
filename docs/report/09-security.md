# 9. Security Considerations

- **Authentication:** `POST /api/auth/login` takes an email or ID, the password and the portal. A wrong password, an unknown account and a wrong portal all return the same `401 INVALID_LOGIN` message. Success issues an HS256 JWT (secret from `JWT_SECRET`, at least 32 bytes) that expires after four hours.
- **Passwords:** BCrypt, strength 10; hashes are never returned or logged. Registration requires 8+ characters with upper and lower case letters and a digit, and always creates an EMPLOYEE (any role sent by the client is ignored).
- **Lockout:** five failed attempts lock the account for 15 minutes (`423 ACCOUNT_LOCKED`). Counters are stored in PostgreSQL and audited (`LOGIN_FAILED`, `ACCOUNT_LOCKED`).
- **Authorisation:** every service checks the role (`AccessGuard.requireRole`) and the ownership (`PortalUser.canView`: an employee sees only incidents they reported, a technician only incidents offered or assigned to them, an admin all). This blocks IDOR: guessing another incident id returns 403. Tests cover these cases (T16, T17).
- **Input validation:** Jakarta Validation on request DTOs with field errors, HTML stripped from incident text, enum and range checks in the domain, and JPA only (no SQL concatenation). React escapes output.
- **API hardening:** stateless sessions, CSRF disabled for the token-based API, CORS limited to the configured frontend origins, `X-Frame-Options: DENY` and `X-Content-Type-Options`, and no stack traces in responses (500 returns a correlation id; details stay in the server log).
- **Data integrity:** the audit table is append-only (no update or delete path), incidents use optimistic locking (`version`), and database constraints back the business rules.

**Known limitations:** logout revocation is held in memory (a restart forgets revoked tokens, which still expire after four hours); there are no refresh tokens, MFA or rate limiting beyond the lockout; email is simulated; HTTPS must be provided by the deployment.
