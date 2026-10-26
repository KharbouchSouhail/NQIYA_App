# NQIYA 🇲🇦 — Spot a mess. Clean it together. Level up.

A mobile-first platform that turns cleaning public spaces into a ranked, social quest.

| | |
|---|---|
| **Frontend** | Angular 21 (standalone components, signals, zoneless, lazy routes), Leaflet + OpenStreetMap/CARTO, `qrcode`, `jsqr`, `canvas-confetti` |
| **Backend**  | Java 21, Spring Boot 4 (Web MVC, Data JPA, Security + OAuth2 resource server/JWT, Validation, Mail, Actuator), Flyway, PostgreSQL, springdoc OpenAPI |
| **AI**       | Groq vision model via the OpenAI-compatible API (JSON mode, fallback model chain), or Google Gemini, or an offline `stub` for development |
| **Infra**    | Docker multi-stage images, nginx serving the SPA and proxying `/api` (single origin), docker-compose |

```
nqiya-app/
├── backend/                     Spring Boot REST API
│   ├── src/main/java/ma/nqiya/
│   │   ├── auth/                passwordless login (6-digit email code → JWT)
│   │   ├── spot/                report, map, join, QR check-in, finish + payout
│   │   ├── ai/                  photo gate: Groq, Gemini, or stub
│   │   ├── reward/              XP / coins ledger, ranks, level-up gifts
│   │   ├── shop/                partner vouchers, wallet, redeem
│   │   ├── leaderboard/         podium + my position
│   │   ├── admin/               moderation + audit log
│   │   ├── user/ photo/ security/ config/ common/
│   ├── src/main/resources/db/migration   Flyway SQL
│   └── src/test/                unit tests + end-to-end test on real PostgreSQL
├── frontend/                    Angular mobile-first PWA
│   └── src/app/
│       ├── core/                models, services, interceptor, guards
│       ├── shared/components/   icon, logo, bottom nav, toasts, rank badge, celebration, QR…
│       └── features/            map, auth, report, spot, leaderboard, shop, profile, admin
└── docker-compose.yml
```

## Run it locally (no install besides Java 21+ and Node 22+)

```bash
# 1. API on :8080 with an embedded PostgreSQL (data kept in backend/target/dev-pg)
cd backend
mvn spring-boot:test-run -Dspring-boot.run.arguments="--app.admin-emails=you@example.com"

# same, with the real Groq photo check (bash; in PowerShell: $env:AI_PROVIDER="groq"; $env:GROQ_API_KEY="gsk_...")
AI_PROVIDER=groq GROQ_API_KEY=gsk_... mvn spring-boot:test-run

# 2. App on :4200 (proxies /api to :8080)
cd frontend
npm install
npm start
```

Open http://localhost:4200 on your phone-sized browser window. In dev mode the login code is shown in the app and logged by the API, so you don't need SMTP.
API docs: http://localhost:8080/swagger-ui.html

To use your own PostgreSQL instead: `mvn spring-boot:run` with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Run with Docker

```bash
cp .env.example .env      # set JWT_SECRET (and GEMINI_API_KEY / SMTP_* for real use)
docker compose up --build
```

App: http://localhost:8081. The backend isn't published, only reachable through nginx's `/api` proxy.

## Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | local `nqiya` db | PostgreSQL |
| `JWT_SECRET` | dev value | HS256 signing key, **32+ chars, required in prod** |
| `AI_PROVIDER` | `stub` | `groq` or `gemini` to enable the real photo gate |
| `GROQ_API_KEY`, `GROQ_MODELS` | – / `qwen/qwen3.8-27b` | Groq key and ordered list of vision models |
| `GEMINI_API_KEY`, `GEMINI_MODELS` | – / `gemini-2.5-flash,…` | Gemini key and ordered fallback chain |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM` | – | login code emails (logged when no host is set; in docker-compose use `SMTP_*` from `.env`) |
| `AUTH_DEV_MODE` | `true` (`false` in `prod` profile) | show the login code in the API response |
| `ADMIN_EMAILS` | – | comma-separated emails promoted to admin on login |
| `CORS_ORIGINS` | `http://localhost:4200` | only needed if the SPA isn't served from the same origin |

## How a cleanup works (API)

1. `POST /api/spots/analyze` (multipart `photo`): the AI (Groq or Gemini) checks the BEFORE photo is a real mess and drafts title/description, dirt level and waste type. Rejected photos can't be published.
2. `POST /api/spots`: publish with the accepted `photoId`, a fresh GPS fix (accuracy ≤ 100 m), and either **PUBLIC** + meeting time or **PRIVATE** (solo, optionally anonymous).
3. `POST /api/spots/{id}/join`: volunteers join. Each user has a personal ticket code (`GET /api/me` → `ticketCode`, shown as a QR code).
4. `POST /api/spots/{id}/check-in` `{ticketCode}`: the organizer scans tickets. The first check-in flips the spot to **CLEANING**. Solo cleanups use `POST /api/spots/{id}/start`.
5. `POST /api/spots/{id}/finish` (multipart `photo`): the AI compares BEFORE/AFTER (same place? clean?). On pass, **every checked-in participant** gets 100 XP + 100 coins and the organizer gets +50/+50, all in one transaction with rank multipliers and level-up gifts. The verdict is always computed server-side.

Ranks: Bronze 0 · Silver 100 (+100 coins) · Gold 400 (+250) · Platinum 1200 (×2 coins, +500) · Diamond 3000 (×2 XP, ×2 coins, +1000).

## Engineering notes

- **Layering**: controller → service (transactions, business rules) → Spring Data repository, with DTOs as Java records, Bean Validation, and RFC 9457 problem details carrying a stable `code`.
- **Security**: stateless JWT (Spring Security resource server), role-based admin routes, hashed one-time codes with attempt and rate limits, optimistic locking (`@Version`) on users and spots against double payouts.
- **AI calls happen outside DB transactions**. Model fallback chain for free-tier quotas.
- **Schema** is owned by Flyway; Hibernate only validates it.
- **Frontend**: signals + `rxResource`, OnPush everywhere, functional interceptor/guards, lazy-loaded feature routes, client-side photo compression, toasts on every action, safe-area padding, 44 px tap targets, `prefers-reduced-motion` respected.

## Tests

```bash
cd backend && mvn test     # 24 tests incl. full report→join→check-in→finish→payout→shop flow on real PostgreSQL
cd frontend && npx ng build
```
