# How I Deployed CBEC-AI (TradeBridge)

**Project:** Context-Aware AI Platform for Cross-Border Export Intelligence & Regulatory Compliance
**Author:** Shashidhar Reddy
**Stack:** Spring Boot 3.3 / Java 21 · React 19 + Vite · MySQL 8 · NVIDIA NIM (Hybrid RAG) · XGBoost · Docker

This document records exactly how the project is deployed — both the
**local Docker deployment** (single command) and the **cloud deployment** (Render + TiDB Cloud).

---

## 1. Architecture at a Glance

The system runs as **three services** behind a single web entry point:

```
                         ┌──────────────────────────────┐
   Browser  ──── :80 ───▶│  Frontend (nginx + React SPA)│
                         │  serves /  and proxies /api/ │
                         └───────────────┬──────────────┘
                                         │  /api/*  →  :8081
                                         ▼
                         ┌──────────────────────────────┐
                         │  Backend (Spring Boot / Java) │
                         │  REST API + JWT + Hybrid RAG  │
                         └───────────────┬──────────────┘
                                         │  JDBC
                                         ▼
                         ┌──────────────────────────────┐
                         │  MySQL 8 (two schemas)        │
                         │  • InternationalTrade (auth,  │
                         │    products, orders, shipments│
                         │  • TradeData (HS codes,       │
                         │    regulatory data)           │
                         └──────────────────────────────┘

   External: NVIDIA NIM API (LLM + embeddings + reranker) for AI features
```

**Why nginx is the entry point:** the browser only ever talks to port 80. nginx
serves the built React bundle and reverse-proxies every `/api/*` call to the backend
on `:8081`. This means there are no cross-origin (CORS) problems in the Docker setup,
and client-side routes fall back to `index.html` so deep links don't 404.

---

## 2. Deployment Files (what each one does)

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Orchestrates the 3 services (mysql, backend, frontend) with health-gated startup order |
| `backend/Dockerfile` | Multi-stage: Maven/JDK 21 builds the jar → slim `temurin:21-jre-alpine` runs it |
| `frontend/Dockerfile` | Multi-stage: Node 22 builds the Vite bundle → `nginx:1.27-alpine` serves it |
| `frontend/nginx.conf` | Serves the SPA and proxies `/api/*` → `backend:8081` |
| `deploy/mysql/init.sql` | Creates both databases (`InternationalTrade`, `TradeData`) on first boot |
| `.env.example` → `.env` | All secrets and ports (never commit the real `.env`) |
| `start.sh` | One-command helper: start / stop / restart / logs / status / clean |
| `render.yaml` | Cloud deployment blueprint for Render (backend Docker + frontend static) |

---

## 3. Prerequisites

- **Docker Desktop** (or Docker Engine + Compose v2) — the only requirement for local deploy
- **Python 3.10+** — only needed once, to seed the 76,000+ HS code records
- **An NVIDIA API key** (optional) from <https://build.nvidia.com> — AI features degrade
  gracefully to a knowledge-based fallback if it is left blank

---

## 4. Local Deployment (Docker — the primary method)

### Step 1 — Configure environment

```bash
cp .env.example .env
```

Then edit `.env` and set at minimum:

```env
MYSQL_ROOT_PASSWORD=<a strong password>
JWT_SECRET=<32+ char random string — generate with: openssl rand -hex 32>
NVIDIA_API_KEY=<your NVIDIA key, or leave blank>
TRADEDATA_DDL_AUTO=update        # "update" on first run, "none" afterwards
```

### Step 2 — Build and start everything

```bash
docker compose up --build -d
```

or use the helper script:

```bash
chmod +x start.sh
./start.sh
```

Docker builds all three images and starts them **in the correct order**, because the
compose file uses health checks:

1. **MySQL** starts and must pass `mysqladmin ping` before anything else runs.
2. **Backend** waits for MySQL to be healthy, then boots and must pass `/api/health`.
3. **Frontend** waits for the backend to be healthy, then starts nginx.

This health-gated ordering is what prevents the classic "backend crashes because the
database isn't ready yet" startup loop.

### Step 3 — Seed the HS code dataset (once)

Hibernate auto-creates the tables, but the **76,000+ ITC-HS 2022 classification records**
are loaded by a one-time pipeline script:

```bash
python pipeline/fetch_hs_codes.py
```

After it completes, set `TRADEDATA_DDL_AUTO=none` in `.env` and restart so Hibernate
stops altering that schema on every boot:

```bash
./start.sh restart
```

### Step 4 — Verify

```bash
# Backend health
curl http://localhost:8081/api/health
# → {"service":"trade-backend","status":"UP", ...}

# AI pipeline status (chat + embeddings + reranker)
curl http://localhost:8081/api/v1/ai/status

# Container status
docker compose ps        # or ./start.sh status
```

Open the app:

- **Frontend:** <http://localhost>
- **Backend API:** <http://localhost:8081/api/health>

### Everyday commands

```bash
./start.sh start      # build + start
./start.sh stop       # stop (keeps the database volume)
./start.sh restart    # rebuild + restart
./start.sh logs       # tail all logs
./start.sh status     # container status
./start.sh clean      # DESTRUCTIVE — removes containers AND the DB volume
```

---

## 5. Cloud Deployment (Render + TiDB Cloud)

The cloud setup splits the database out to a managed service and hosts the two app
tiers on Render, described by `render.yaml`.

### Database — TiDB Cloud (MySQL-compatible)

- Two schemas: `InternationalTrade` and `TradeData`
- Connected over TLS (`useSSL=true&enabledTLSProtocols=TLSv1.2,TLSv1.3`)
- JDBC tuned for a serverless DB: prepared-statement caching, batched statements,
  and connect/socket timeouts

### Backend — Render Docker Web Service

- `runtime: docker`, built from `backend/Dockerfile`
- Health check path: `/api/health`
- All DB / JWT / NVIDIA / CORS values injected as environment variables

### Frontend — Render Static Site

- `buildCommand: npm run build`, published from `frontend/dist`
- SPA rewrite rule: `/* → /index.html`
- `VITE_API_BASE_URL` points at the deployed backend URL

> **Important — Vite bakes the API URL at build time.** `VITE_API_BASE_URL` is compiled
> into the JavaScript bundle during `npm run build`. If the backend URL changes, the
> frontend must be **rebuilt**, not just restarted.

### Deploy to Render

1. Push the repo to GitHub.
2. In Render, create a **Blueprint** from the repo (it reads `render.yaml`).
3. Provide the environment variable values (see the security note below).
4. Render builds and deploys both services.

---

## 6. Security — Configuration & Secrets

Handle all credentials as environment variables, never in committed files.

- **`.env` is git-ignored** — only `.env.example` (with placeholders) is committed.
- **Secrets** (database password, `JWT_SECRET`, `NVIDIA_API_KEY`) must be set in the
  hosting provider's secret manager (Render dashboard / environment group), not written
  into `render.yaml` in the repository.
- **`JWT_SECRET`** must be at least 32 characters (HS256). Generate with `openssl rand -hex 32`.
- **CORS:** in production, set `CORS_ALLOWED_ORIGINS` to the exact deployed frontend
  domain. Do not use `*` together with credentialed requests.
- **Rotate any credential** that has ever been committed or shared.

---

## 7. Notable Design Choices That Affect Deployment

- **Two databases on purpose:** transactional data (`InternationalTrade`) is isolated
  from the large read-heavy reference data (`TradeData`) via separate Hikari pools.
- **ML model needs no deploy step:** the export-ranking predictions
  (`ml_export_rankings_v4.csv`) are packaged as a classpath resource inside the jar, so
  there are no native ML dependencies in the running JVM. Confirm on boot with
  `docker compose logs backend | grep MlExportRanking`.
- **AI is optional and fails safe:** with no `NVIDIA_API_KEY`, AI endpoints return a
  knowledge-based fallback and the app stays fully usable.

---

## 8. Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Backend restarts / can't connect to DB | MySQL not ready yet | Health checks handle ordering; if it persists check `docker compose logs mysql` |
| `Incorrect datetime value: '0000-00-00'` on startup | Zero-date rows in `hs_master` | JDBC URL includes `zeroDateTimeBehavior=CONVERT_TO_NULL`; re-seed the HS data if needed |
| AI features say "unavailable" | `NVIDIA_API_KEY` missing/expired | Set the key in `.env` (local) or the provider's secrets (cloud) and restart |
| Frontend loads but API calls fail | Wrong `VITE_API_BASE_URL` | Rebuild the frontend with the correct backend URL (baked at build time) |
| Login returns 401 | Wrong credentials or locked account | Use valid credentials; check `failed_login_attempts` / `lockout_until` |
| HS search returns nothing | Dataset not seeded | Run `python pipeline/fetch_hs_codes.py` |

---

## 9. One-Page Cheat Sheet

```bash
# ── LOCAL (Docker) ──────────────────────────────────────────────
cp .env.example .env            # then edit secrets
docker compose up --build -d    # or: ./start.sh
python pipeline/fetch_hs_codes.py   # once, to seed HS data
curl http://localhost:8081/api/health
# App: http://localhost   |   API: http://localhost:8081

# ── MANAGE ──────────────────────────────────────────────────────
./start.sh logs | stop | restart | status | clean

# ── CLOUD (Render) ──────────────────────────────────────────────
# Push to GitHub → Render Blueprint from render.yaml
# Set secrets in Render dashboard (NOT in the repo)
```
