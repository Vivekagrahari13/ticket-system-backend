# Ticket System: Backend

REST API for a helpdesk. Users register, log in and raise tickets. Admins see all tickets, change their status and delete them. Built with Spring Boot and PostgreSQL. The web app is in the `ticket-system-frontend` repo.

## Tech stack

- Java 21, Spring Boot 4
- Spring Web, Spring Security, Spring Data JPA, Bean Validation
- PostgreSQL
- JWT with JJWT 0.12, stored in an `HttpOnly` cookie
- Lombok, Maven

## Getting started

1. Create a PostgreSQL database named `ticket_system`.
2. Set these environment variables (IntelliJ: Run, Edit Configurations, Environment variables):

   | Variable | Purpose |
   | --- | --- |
   | `JWT_SECRET` | Base64 secret for signing tokens. Generate one with `openssl rand -base64 64` |
   | `DB_PASSWORD` | Database password |
   | `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Creates an admin user on startup if it doesn't exist |
   | `FRONTEND_ORIGIN` | Allowed browser origin, default `http://localhost:3000` |
   | `COOKIE_SECURE` | `true` in production (HTTPS), default `false` |

3. Run:

   ```bash
   ./mvnw spring-boot:run
   ```

The API runs at `http://localhost:8080`. Tables are created automatically (`ddl-auto: update`).

Never commit real secrets. Keep them in environment variables only.

## API

| Method | Path | Access | Result |
| --- | --- | --- | --- |
| POST | `/auth/register` | Public | 201, the new user (role is always `USER`) |
| POST | `/auth/login` | Public | 200, the user, and the `token` cookie |
| POST | `/auth/logout` | Public | 204, clears the cookie |
| GET | `/auth/me` | Logged in | 200, the current user |
| POST | `/tickets` | Logged in | 201, the new ticket (status `OPEN`) |
| GET | `/tickets?status=&page=&size=` | Logged in | A page of tickets: users see their own, admins see all |
| GET | `/tickets/{id}` | Owner or admin | 200, or 403, or 404 |
| PATCH | `/tickets/{id}/status` | Admin only | 200, or 403 |
| DELETE | `/tickets/{id}` | Admin only | 204, or 403, or 404 |

Errors share one shape:

```json
{ "status": 409, "message": "Email already registered", "timestamp": "..." }
```

Validation errors (400) also include `"fieldErrors": { "email": "..." }`.

## How a request flows

```
Request -> JwtAuthFilter -> Controller -> Service -> Repository -> PostgreSQL
              |                              |
   reads the cookie, verifies the     role and ownership checks
   token, sets the logged-in user
```

- **Auth:** on login the server signs a JWT (id, email, role, 60-minute expiry) and sets it as an `HttpOnly`, `SameSite=Lax` cookie. `JwtAuthFilter` reads it on every request. No cookie or a bad token gives a 401.
- **Authorization:** checks live in the service layer, so they apply however the endpoint is called. A user who calls an admin endpoint gets a 403.
- **Passwords:** hashed with BCrypt, never returned by the API.
- **Login errors:** wrong email and wrong password should return the same message so callers can't tell which emails are registered.
- **CORS:** only the frontend origin is allowed, with credentials.

## Project structure

```
auth/        AuthController, AuthService, request and response DTOs
user/        User entity, Role, UserRepository
ticket/      Ticket entity, status and priority enums, controller, service, repository, DTOs
security/    JwtService, JwtAuthFilter, CustomUserDetails
config/      SecurityConfig (filter chain, CORS), AdminSeeder
common/      GlobalExceptionHandler, ApiError, custom exceptions
entity/      GenericEntity (id, createdAt, updatedAt)
```

## Data model

- `users`: id, email (unique), password (BCrypt hash), role (`USER` or `ADMIN`), timestamps
- `tickets`: id, title, description, priority (`LOW`, `MEDIUM`, `HIGH`), status (`OPEN`, `IN_PROGRESS`, `CLOSED`), owner (user), timestamps

## Next steps

- Flyway migrations instead of `ddl-auto: update`
- Unit and integration tests
- Comments on tickets, an audit log, and ticket assignment
- Rate limiting on login
- Docker and CI
