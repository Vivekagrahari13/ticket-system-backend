# Ticket System: Backend

REST API for a helpdesk. Users register, log in, raise tickets, edit them, and withdraw them. Admins see all tickets, start work and close them with a resolution note. Tickets are never deleted. Built with Spring Boot and PostgreSQL. The web app is in the `ticket-system-frontend` repo.

## Tech stack

- Java 21, Spring Boot 4
- Spring Web, Spring Security, Spring Data JPA, Bean Validation
- PostgreSQL with Flyway migrations
- JWT with JJWT 0.12, stored in an `HttpOnly` cookie
- springdoc-openapi (Swagger UI), Lombok, Maven
- JUnit 5, Mockito, AssertJ

## Getting started

1. Create a PostgreSQL database named `ticket_system`.
2. Set these environment variables (IntelliJ: Run, Edit Configurations, Environment variables):

   | Variable | Purpose |
   | --- | --- |
   | `JWT_SECRET` | Base64 secret for signing tokens. Generate one with `openssl rand -base64 64` |
   | `DB_PASSWORD` | Database password |
   | `DB_URL`, `DB_USER` | Optional. Default to a local PostgreSQL and the user `postgres` |
   | `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Creates an admin user on startup if it doesn't exist |
   | `FRONTEND_ORIGIN` | Allowed browser origin, default `http://localhost:3000` |
   | `COOKIE_SECURE` | `true` in production (HTTPS), default `false` |
   | `SWAGGER_ENABLED` | `false` to turn the API docs off, default `true` |

3. Run:

   ```bash
   ./mvnw spring-boot:run
   ```

The API runs at `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui.html`. Flyway creates and updates the schema on startup, and Hibernate only validates it (`ddl-auto: validate`).

Never commit real secrets. Keep them in environment variables only. `.env.example` lists the variable names.

## Ticket lifecycle

A ticket only moves forward and ends as `CLOSED` or `WITHDRAWN`. Both are final and read-only.

| From | To | Who | Condition |
| --- | --- | --- | --- |
| `OPEN` | `IN_PROGRESS` | Admin | None |
| `OPEN` | `CLOSED` | Admin | Resolution note required |
| `IN_PROGRESS` | `CLOSED` | Admin | Resolution note required |
| `OPEN` | `WITHDRAWN` | Owner | Only while `OPEN` |

Every other move, including any move backwards or to the same status, is refused with 409. The rule lives in one place, `TicketStatus.canMoveTo`. Title, description and priority can be edited only while the ticket is `OPEN` or `IN_PROGRESS`. There is no delete.

## API

| Method | Path | Access | Result |
| --- | --- | --- | --- |
| POST | `/auth/register` | Public | 201, the new user (role is always `USER`) |
| POST | `/auth/login` | Public | 200, the user, and the `token` cookie |
| POST | `/auth/logout` | Public | 204, clears the cookie |
| GET | `/auth/me` | Logged in | 200, the current user |
| POST | `/tickets` | Logged in | 201, the new ticket (status `OPEN`) |
| GET | `/tickets?status=&page=&size=` | Logged in | A page of **your own** tickets, for admins too |
| GET | `/admin/tickets?status=&page=&size=` | Admin only | A page of every user's tickets. 403 for a user |
| GET | `/tickets/{id}` | Owner or admin | 200, or 403, or 404 |
| PUT | `/tickets/{id}` | Owner or admin | 200, or 400, 403, 404, 409 (ticket is final) |
| POST | `/tickets/{id}/withdraw` | Owner only | 200, or 403, 404, 409 (not `OPEN`) |
| PATCH | `/tickets/{id}/status` | Admin only | Body `{ status, resolutionNote }`. 200, or 400, 403, 404, 409 |

`PATCH /tickets/{id}/status` accepts only `IN_PROGRESS` and `CLOSED`. A request for any other status returns 400, and closing without a `resolutionNote` returns 400.

Errors share one shape:

```json
{ "status": 409, "message": "Cannot move a ticket from CLOSED to IN_PROGRESS", "timestamp": "..." }
```

Validation errors (400) also include `"fieldErrors": { "title": "..." }`.

Status codes: 400 bad or invalid request, 401 not logged in, 403 not allowed, 404 not found, 409 conflicts with the ticket's current status.

## Logging

- **Request id:** every request gets an id (`X-Request-Id`). A safe id sent by the caller is reused, otherwise one is generated. It appears on every log line as `[req=...]`, in the response header, and as `requestId` in error bodies, so a user can quote a reference that matches the server logs.
- **User id:** after authentication the user's id appears as `[user=...]`.
- **Access log:** one `INFO` line per request: method, path, status and duration. Swagger and preflight requests are skipped or kept at `DEBUG`.
- **Business events** at `INFO`: ticket created, edited, status changed, withdrawn, user registered, user logged in. Each logs ids, never titles, notes or other content.
- **Levels:** `ERROR` for unexpected failures (with the stack trace), `WARN` for security-relevant rejections (failed login, 401, 403), `INFO` for expected business errors (400, 404, 409), `DEBUG` for noise.
- **Never logged:** passwords, tokens, cookies, request bodies. An email is masked (`j***@company.com`) when it must appear.
- **Settings:** `LOG_LEVEL` sets the level for the app's own code (default `INFO`). Set the `org.hibernate.SQL` level to `DEBUG` to see SQL.
- **Production:** run with `SPRING_PROFILES_ACTIVE=prod` for one JSON object per line (`application-prod.yml`), so a log system can search by `requestId` and `userId`.

## How a request flows

```
Request -> JwtAuthFilter -> Controller -> Service -> Repository -> PostgreSQL
              |                              |
   reads the cookie, verifies the     role, ownership and status rules
   token, sets the logged-in user
```

- **Auth:** on login the server signs a JWT (id, email, role, 60-minute expiry) and sets it as an `HttpOnly`, `SameSite=Lax` cookie. `JwtAuthFilter` reads it on every request. No cookie or a bad token gives a 401.
- **Authorization:** checks live in the service layer, so they apply however the endpoint is called. The order is always 403 (not allowed), 400 (bad request), 404 (not found), then 409 (wrong status).
- **Passwords:** hashed with BCrypt, never returned by the API.
- **CORS:** only the frontend origin is allowed, with credentials.

## Project structure

```
auth/        AuthController, AuthService, request and response DTOs
user/        User entity, Role, UserRepository
ticket/      Ticket entity, TicketStatus and Priority, controller, service, repository, DTOs
security/    JwtService, JwtAuthFilter, CustomUserDetails
config/      SecurityConfig (filter chain, CORS), OpenApiConfig, AdminSeeder
common/      GlobalExceptionHandler, ApiError, custom exceptions
entity/      GenericEntity (id, createdAt, updatedAt)
src/main/resources/db/migration/   Flyway migrations
src/test/    unit tests
```

## Data model

- `users`: id, email (unique), password (BCrypt hash), role (`USER` or `ADMIN`), timestamps
- `tickets`: id, title, description, priority (`LOW`, `MEDIUM`, `HIGH`), status (`OPEN`, `IN_PROGRESS`, `CLOSED`, `WITHDRAWN`), owner (user), `resolution_note`, `closed_at`, `withdrawn_at`, timestamps

Migrations: `V1` creates the tables, `V2` adds ticket indexes, `V3` adds the resolution note and the end dates. Never edit a migration that has already run. Add a new file instead.

## Tests

```bash
./mvnw test
```

- `TicketStatusTest`: every status transition (a 4 by 4 table), final and editable statuses
- `TicketServiceTest`: create, list mine and list all, view, edit, change status, and withdraw, including the forbidden, bad request, not found and conflict cases
- `RequestLoggingFilterTest` and `LogMaskingTest`: request id handling, and that personal data is masked

Unit tests use mocks and need no database. The Spring context test is disabled until it is replaced by a Testcontainers test.

## Next steps

- Optimistic locking with `@Version`, and a status history table
- Testcontainers integration tests and MockMvc controller tests
- Problem Details error format, `ETag` and `If-Match`, idempotent create
- Comments on tickets, ticket assignment, notifications through an outbox
- Rate limiting on login, Docker and CI
- CSRF hardening: CSRF protection is off, and the `SameSite=Lax` cookie is the only defence. Add an `Origin` check or a CSRF token
