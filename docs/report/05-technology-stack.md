# 5. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 (source/target; built and tested on JDK 25) |
| Backend framework | Spring Boot (Web, Data JPA, Validation, Security, Scheduling) | 3.3.5 |
| ORM | Hibernate ORM | 6.5 (via Spring Boot) |
| Database | PostgreSQL | 15+ (verified on 18.7) |
| Schema migrations | Flyway | 10 (via Spring Boot) |
| Authentication | HS256 JWT (custom signer), BCrypt (strength 10) | Spring Security Crypto |
| Testing | JUnit 5, Spring Boot Test, MockMvc, H2 (tests only, PostgreSQL mode) | via Spring Boot 3.3.5 |
| Build | Maven (wrapper) | 3.9 |
| Frontend | React, React Router, Vite | 18.3, 6.28, 5.4 |
| Frontend data and UI | Axios, TanStack Query, Recharts, lucide-react, react-hot-toast, Tailwind (base) | see `frontend/package.json` |
| Diagrams | PlantUML | sources in `docs/uml` |
| Optional infrastructure | Docker Compose (PostgreSQL 15) | `docker-compose.yml` |
