# Ticket System: architecture

Last verified: 2026-11-17

A small helpdesk. Users raise tickets, admins work them and close them. Two repos:

- backend: `ticket-system-backend` (Spring Boot 4, Java 21, PostgreSQL)
- frontend: `ticket-system-frontend` (Next.js 16, TypeScript, Tailwind)

```
Browser (Next.js, :3000)  --- HTTP + cookie --->  Spring Boot API (:8080)  --- JDBC --->  PostgreSQL
```

There is no session on the server. The login cookie holds a signed JWT, and the browser sends it on every call.

## Backend layers

```
Controller  ->  Service  ->  Repository  ->  PostgreSQL
```

- Controllers only read the request and call a service. They return DTO records, never entities.
- Services hold the rules: who may do what, which status moves are allowed. They are `@Transactional` when they write.
- Repositories are Spring Data interfaces.
- `GlobalExceptionHandler` turns exceptions into one JSON error shape.
- Code is grouped by feature: `auth`, `user`, `ticket`, plus `security`, `config`, `common`.

## Authentication

1. `POST /auth/login` looks the user up by email and compares the password with BCrypt.
2. On success `JwtService` signs a token (user id, email, role, 60 minutes) and `AuthService` puts it in a cookie named `token`: `HttpOnly`, `SameSite=Lax`, `Path=/`. `Secure` is on when `COOKIE_SECURE=true`.
3. On every later request `JwtAuthFilter` reads the cookie, checks the signature and expiry, and builds the logged-in user from the token's claims. There is no database lookup on each request.
4. No cookie or a bad token means no user, and the response is 401.
5. `GET /auth/me` returns the user from the token. The frontend calls it on page load, because JavaScript cannot read an `HttpOnly` cookie.
6. `POST /auth/logout` sends the cookie back with `Max-Age=0`.

Trade-off: because the role is read from the token, a role change only takes effect when the token expires.

An admin account is created at startup by `AdminSeeder` from `ADMIN_EMAIL` and `ADMIN_PASSWORD`. Registration always creates a `USER`.

## One request, in order

1. `RequestLoggingFilter` runs first. It gives the request an id (or reuses a safe `X-Request-Id`), puts it in the log context, and later writes one access log line.
2. The Spring Security chain runs: CORS, then `JwtAuthFilter`. After this the user id is added to the log context.
3. The controller validates the body (`@Valid`) and calls the service.
4. The service checks permissions and rules, then saves.
5. If anything throws, `GlobalExceptionHandler` builds the response. Expected errors are logged at INFO or WARN, unexpected ones at ERROR with the stack trace.

Error body: `{ status, message, fieldErrors?, timestamp, requestId }`. The frontend shows the `requestId` as a reference for server errors, so it can be matched to the log.

Status codes: 400 bad input, 401 not logged in, 403 not allowed, 404 not found, 409 the ticket's current status does not allow it.

## Ticket lifecycle

Statuses: `OPEN`, `IN_PROGRESS`, `CLOSED`, `WITHDRAWN`. Status only moves forward.

```
OPEN --admin--> IN_PROGRESS --admin + note--> CLOSED
 |------------------ admin + note ---------> CLOSED
 '------ owner, only while OPEN -----------> WITHDRAWN
```

`CLOSED` and `WITHDRAWN` are final. A final ticket cannot be edited or moved. Tickets are never deleted: there is no delete endpoint. A user who no longer needs a ticket withdraws it.

The rules sit in one place, the `TicketStatus` enum (`canMoveTo`, `isFinal`, `isEditable`, `canBeSetByAdmin`). The `switch` in `canMoveTo` has no default branch, so adding a status makes the code stop compiling until its rules are written.

Closing needs a resolution note. It is saved with `closed_at`. Withdrawing sets `withdrawn_at`.

## Permissions

| Action | Owner | Admin | Someone else |
| --- | --- | --- | --- |
| View a ticket | yes | yes | 403 |
| List own tickets (`GET /tickets`) | yes | yes (own only) | n/a |
| List all tickets (`GET /admin/tickets`) | 403 | yes | 403 |
| Edit (`PUT /tickets/{id}`) | yes, while not final | yes, while not final | 403 |
| Start work or close (`PATCH /tickets/{id}/status`) | 403 | yes | 403 |
| Withdraw (`POST /tickets/{id}/withdraw`) | yes, while `OPEN` | 403 unless it is their own | 403 |

Checks happen in the service, not only in the controller, so they hold whichever way the endpoint is called.

Order of checks, so a stranger learns nothing about a ticket's state:

- owner actions (edit, withdraw): 404, then 403, then 409
- admin status change: 403, then 400, then 404, then 409

## Data model

- `users`: id, email (unique), password (BCrypt hash), role, `created_at`, `updated_at`
- `tickets`: id, title, description, priority, status, `owner_id`, `resolution_note`, `closed_at`, `withdrawn_at`, `created_at`, `updated_at`

Statuses and priorities are stored as text, so adding a status does not need a schema change.

Schema changes go through Flyway (`src/main/resources/db/migration`): V1 creates the tables, V2 adds indexes, V3 adds the resolution note and end dates. Hibernate is set to `validate`, so it only checks that the entities match the tables. A migration that has run is never edited. A new file is added instead.

Lists are paged: default 10 per page, at most 50, newest first.

## Frontend

- `app/` holds the routes. Every `page.tsx` is a Server Component that renders a client component from `components/`.
- `context/AuthContext.tsx` calls `/auth/me` once on load and exposes `user`, `loading`, `setUser` and `logout`.
- `hooks/useRequireAuth.ts` redirects logged-out visitors to `/login`, and non-admins away from the admin page. It is only for a cleaner experience. The backend does the real check.
- `hooks/useTickets.ts` loads a page of tickets, from `/tickets` or from `/admin/tickets`. `hooks/useTicket.ts` loads one ticket.
- `services/api.ts` is the only place that calls the backend. It sends the cookie and turns error responses into one error type.
- `types/ticket.ts` has the shared types, labels and two helpers, `nextStatuses` and `isFinalStatus`, which copy the backend rules so the UI offers only valid choices.

Routes: `/`, `/login`, `/register`, `/tickets`, `/tickets/new`, `/tickets/[id]/edit`, `/admin/tickets`.

## Decisions

- JWT in an `HttpOnly` cookie instead of `localStorage`, so a script injected into a page cannot read the token.
- "My tickets" and "All tickets" are two endpoints. One endpoint that returned different data by role confused the UI.
- No deletion. Records stay. This also made soft delete unnecessary.
- Status rules in an enum with a unit test for every combination, instead of scattered `if` checks.
- Flyway instead of letting Hibernate change the schema, so every environment gets the same changes in the same order.

## Known gaps

- CSRF protection is off. The `SameSite=Lax` cookie is the only defence. An `Origin` check or a CSRF token should be added.
- No optimistic locking: two admins changing one ticket at once can overwrite each other.
- No status history table, so past changes are not recorded.
- Integration tests are paused. The Spring context test is disabled, so wiring mistakes are only found by running the app.
- No login rate limiting, no caching, one instance only.
- No Docker setup, CI pipeline or deployment yet.
