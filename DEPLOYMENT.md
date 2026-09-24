# TradeWise — Deployment Guide

> AI-Driven Cross-Border Export Intelligence Platform  
> Full-stack: React 19 · Spring Boot 3.3 · MySQL 8 · Docker

---

## Quick Start (Docker — Recommended)

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- 4 GB RAM available for containers
- Git (to clone the repository)

### 1 — Clone & configure

```bash
git clone https://github.com/Shashidarreddy19/TradeWise.git
cd TradeWise

# Create your environment file from the template
cp .env.example .env
```

Edit `.env` and set at minimum:

```env
MYSQL_ROOT_PASSWORD=YourStrongPassword123!
JWT_SECRET=YourLongRandomSecretAtLeast32CharactersHere
NVIDIA_API_KEY=                # optional — get from https://build.nvidia.com
```

### 2 — Build and start everything

```bash
docker compose up --build -d
```

This starts three containers:
- `tradewise-mysql` — MySQL 8 with both databases pre-created
- `tradewise-backend` — Spring Boot on port 8081
- `tradewise-frontend` — React SPA served by nginx on port 80

### 3 — Open the app

```
http://localhost
```

The backend API is available at:
```
http://localhost:8081/api/health
```

### 4 — Stop

```bash
docker compose down
```

To also delete the database volume:
```bash
docker compose down -v
```

---

## Manual Local Development (No Docker)

### Prerequisites
- Java 21+
- Node.js 18+
- Maven 3.9+
- MySQL 8 running locally

### Step 1 — Configure MySQL

Create both databases:
```sql
CREATE DATABASE InternationalTrade CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE TradeData CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Step 2 — Configure backend `.env`

```bash
cd backend
cp .env.example .env
# Edit .env: set DB_MAIN_PASSWORD, DB_TRADEDATA_PASSWORD, JWT_SECRET
```

### Step 3 — Start backend

```bash
cd backend
mvn spring-boot:run
```

Backend starts on **http://localhost:8081**

### Step 4 — Start frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts on **http://localhost:5173**

---

## Production Deployment Options

### Option A — Single Server with Docker

On any Linux VPS (Ubuntu 22.04 recommended):

```bash
# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# Clone and configure
git clone https://github.com/Shashidarreddy19/TradeWise.git
cd TradeWise
cp .env.example .env
nano .env   # set MYSQL_ROOT_PASSWORD, JWT_SECRET, CORS_ALLOWED_ORIGINS

# Deploy
docker compose up --build -d

# Check status
docker compose ps
docker compose logs backend --tail=50
```

**Ports exposed:**
- Port 80 → React frontend
- Port 8081 → Spring Boot API (can be hidden behind nginx reverse proxy)
- Port 3306 → MySQL (close this in firewall if not needed externally)

### Option B — Railway (Free Tier)

1. Push code to GitHub
2. Go to [railway.app](https://railway.app) → New Project → Deploy from GitHub
3. Add a **MySQL** service plugin
4. Set environment variables in Railway dashboard:
   ```
   DB_MAIN_URL = jdbc:mysql://<railway-mysql-host>:<port>/InternationalTrade?...
   DB_TRADEDATA_URL = jdbc:mysql://<railway-mysql-host>:<port>/TradeData?...
   DB_MAIN_USERNAME = root
   DB_MAIN_PASSWORD = <railway-mysql-password>
   DB_TRADEDATA_USERNAME = root  
   DB_TRADEDATA_PASSWORD = <railway-mysql-password>
   JWT_SECRET = <your-strong-secret>
   NVIDIA_API_KEY = <optional>
   ```
5. The `Dockerfile` in `backend/` is auto-detected

For the frontend, deploy to [Vercel](https://vercel.com) or [Netlify](https://netlify.com):
```bash
cd frontend
npm run build
# Upload dist/ to Vercel/Netlify
# Set env var: VITE_API_BASE_URL = https://your-backend-url.railway.app/api
```

### Option C — Render

- Backend: New Web Service → connect GitHub → Root directory: `backend` → uses `Dockerfile`
- Frontend: New Static Site → Root directory: `frontend` → Build command: `npm run build` → Publish directory: `dist`

Set env vars in Render dashboard (same as Railway above).

---

## Environment Variables Reference

### Root `.env` (Docker Compose)

| Variable | Required | Default | Description |
|---|---|---|---|
| `MYSQL_ROOT_PASSWORD` | **Yes** | `TradeWise@2024Prod` | MySQL root password |
| `JWT_SECRET` | **Yes** | (weak default) | JWT signing key, ≥32 chars |
| `NVIDIA_API_KEY` | No | (empty) | NVIDIA API key for AI features |
| `MYSQL_PORT` | No | `3306` | Host MySQL port |
| `BACKEND_PORT` | No | `8081` | Host backend port |
| `FRONTEND_PORT` | No | `80` | Host frontend port |
| `CORS_ALLOWED_ORIGINS` | No | (empty) | Extra comma-separated origins |
| `TRADEDATA_DDL_AUTO` | No | `update` | Hibernate DDL for TradeData |

### Backend `backend/.env` (Local dev only)

| Variable | Description |
|---|---|
| `DB_MAIN_USERNAME` | MySQL username |
| `DB_MAIN_PASSWORD` | MySQL password |
| `DB_TRADEDATA_USERNAME` | MySQL username for TradeData |
| `DB_TRADEDATA_PASSWORD` | MySQL password for TradeData |
| `JWT_SECRET` | JWT signing key |
| `NVIDIA_API_KEY` | NVIDIA AI API key |

---

## Health Checks

| Endpoint | Expected Response |
|---|---|
| `GET http://localhost:8081/api/health` | `{"status":"UP","database":"UP"}` |
| `GET http://localhost/` | React SPA HTML |
| `GET http://localhost/api/auth/check-email?email=x@x.com` | `{"available":true}` |

---

## Post-Deploy: Populate Regulatory Data

After the first deployment, run this to populate the HS code reference data:

```bash
pip install mysql-connector-python urllib3
python pipeline/fetch_hs_codes.py
```

This inserts 76,329 HS 2022 codes into the `TradeData.hs_master` table (required for HS classification and regulatory lookups).

---

## Architecture Overview

```
Browser
  │
  ├─── GET /  →  nginx (port 80)  →  serves React SPA (dist/)
  │
  └─── /api/* →  nginx proxy  →  Spring Boot (port 8081)
                                      │
                          ┌───────────┴───────────┐
                          │                       │
                   InternationalTrade DB    TradeData DB
                   (users, products,       (HS codes,
                    orders, shipments)      regulations)
```

---

## Troubleshooting

**Backend won't start — database connection refused:**
```bash
docker compose logs mysql --tail=30
# Wait for "mysqld: ready for connections" then:
docker compose restart backend
```

**Frontend shows blank page:**
```bash
docker compose logs frontend --tail=20
# Check nginx config / dist build
```

**CORS errors in browser:**
- Add your frontend URL to `CORS_ALLOWED_ORIGINS` in `.env`
- Rebuild: `docker compose up --build -d`

**Port 80 already in use:**
- Change `FRONTEND_PORT=8080` in `.env`
- Or stop whatever is using port 80: `netstat -ano | findstr :80`

**Check container status:**
```bash
docker compose ps
docker compose logs --tail=50
docker stats
```

---

## Default Test Credentials

After first run, register an account at `http://localhost/` or use the test account:

| Field | Value |
|---|---|
| Email | `apitest_full@tradewise.com` |
| Password | `ApiTest@123` |
| Role | EXPORTER |

---

*TradeWise — B V Raju Institute of Technology, Narsapur*
