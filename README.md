# GymPilot Backend

Spring Boot 4 / Java 21 API for GymPilot Vision. PostgreSQL is managed with Flyway and authentication uses short-lived JWT access tokens plus rotated, opaque refresh tokens.

## Local setup

1. Install Java 21 and Docker Desktop.
2. Start PostgreSQL:

   ```powershell
   docker compose up -d
   ```

3. Create your ignored local environment file, replace the placeholder secrets,
   and start the API:

   ```powershell
   Copy-Item .env.example .env
   # Edit .env and set DB_PASSWORD, JWT_SECRET, and any service credentials.
   .\mvnw.cmd spring-boot:run
   ```

Spring Boot imports `.env` from the project directory for local development.
Operating-system and deployment environment variables take precedence. The
default local database URL is `jdbc:postgresql://localhost:5432/gympilot` with
username `gympilot`; set its password with `DB_PASSWORD`. Flyway applies the
schema, media, and exercise-catalogue migrations automatically.

If `java -version` reports Java 17 but Android Studio is installed, its bundled Java 21 runtime can be used in PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Health check: `GET http://localhost:8080/actuator/health`

### Cloudinary image storage

Create a Cloudinary account and set these server-side values in `.env` before
starting the API:

```properties
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
```

Never put the API secret in the React Native application. Authenticated clients upload `multipart/form-data` using the `file` field:

- `POST /api/profile/picture` uploads/replaces the current user's profile picture.
- `DELETE /api/profile/picture` removes it.
- `POST /api/media/images?purpose=GENERAL` uploads any other application image. Supported purposes are `WORKOUT`, `EXERCISE`, `NUTRITION`, `COACH`, and `GENERAL`.
- `DELETE /api/media/images/{mediaId}` removes an image owned by the current user.

JPEG, PNG, WebP, and GIF are accepted up to 5 MB. The API verifies both the declared MIME type and file signature. PostgreSQL stores the returned HTTPS URL and Cloudinary identifiers in `media_assets`; binary image data is never stored in PostgreSQL.

## Authentication endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `POST /api/auth/verify-email`
- `POST /api/auth/resend-verification`
- `POST /api/auth/change-password` (Bearer token required)

Account endpoints are under `/api/users/me`; fitness profile endpoints are under `/api/profile` and `/api/profile/me`. All other routes require a valid access token unless explicitly allowed in `SecurityConfig`.

## Exercise and workout endpoints

- `GET /api/exercises` returns the seeded exercise catalogue.
- `GET /api/exercises/{exerciseId}` returns one exercise.
- `POST /api/workouts/plans/generate` creates a plan when the current week has none; otherwise it returns the active plan.
- `POST /api/workouts/plans/regenerate` archives the current plan and creates its replacement.
- `GET /api/workouts/plans/current` returns the current weekly plan.
- `GET /api/workouts/today` returns today's workout from that plan.
- `POST /api/workouts/sessions` prepares a planned workout session.
- `POST /api/workouts/sessions/{sessionId}/start` starts the prepared session.
- `PATCH /api/workouts/sessions/{sessionId}/complete` completes it and stores exercise results.
- `PATCH /api/workouts/sessions/{sessionId}/skip` records a skipped session.
- `PATCH /api/workouts/sessions/{sessionId}/reschedule` moves a scheduled session.
- `GET /api/workouts/sessions/history` returns completed and skipped workouts.

Plan generation uses the user's goal, experience level, available equipment, selected workout days, and preferred session duration. A completed fitness profile is required before generating a plan.

### Optional Ollama plan explanation

The rule-based generator remains responsible for exercise selection and prescription. Ollama is used only to explain the completed plan. Install Ollama, pull the configured model, then set:

```properties
PLAN_EXPLANATION_AI_ENABLED=true
SPRING_AI_CHAT_MODEL=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2:3b
```

If Ollama is disabled, unavailable, or returns empty content, the API stores a deterministic explanation instead.

### Brevo transactional email

Register and verify a sender in Brevo, then add these server-side values to `.env`:

```properties
EMAIL_DELIVERY_ENABLED=true
BREVO_API_KEY=your-brevo-api-key
BREVO_SENDER_EMAIL=verified-sender@example.com
BREVO_SENDER_NAME=GymPilot
```

Registration/resend-verification and password recovery use Brevo's transactional email API. Keep `EMAIL_DELIVERY_ENABLED=false` until the sender and API key are ready; no email secret belongs in the React Native project.

For local UI work only, `EXPOSE_DEVELOPMENT_TOKENS=true` includes email-verification/reset tokens in the response. Keep it `false` outside local development. Production startup rejects the built-in development JWT secret.

## API response format

Every controller returns the same envelope:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {},
  "timestamp": "2026-10-26T13:00:00Z",
  "path": "/api/auth/login"
}
```

Errors use `success: false` and an `errors` array containing stable `code`, optional `field`, and human-readable `message` values.

## Tests

```powershell
.\mvnw.cmd test
```

Tests use an in-memory H2 database and do not require PostgreSQL or Ollama.
