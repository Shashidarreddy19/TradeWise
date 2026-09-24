#!/bin/bash
# ============================================================================
#  TradeWise — One-command start script
#  Usage:
#    chmod +x start.sh
#    ./start.sh           # start all services
#    ./start.sh stop      # stop all services
#    ./start.sh restart   # rebuild and restart
#    ./start.sh logs      # tail logs
#    ./start.sh status    # show container status
# ============================================================================

set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'

log()  { echo -e "${GREEN}[TradeWise]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARNING]${NC}  $1"; }
err()  { echo -e "${RED}[ERROR]${NC}    $1"; exit 1; }

# ── Check prerequisites ───────────────────────────────────────────────────────
check_docker() {
  if ! command -v docker &>/dev/null; then
    err "Docker not found. Install from https://docs.docker.com/get-docker/"
  fi
  if ! docker info &>/dev/null; then
    err "Docker daemon not running. Start Docker Desktop or run: sudo systemctl start docker"
  fi
}

# ── Create .env from .env.example if missing ──────────────────────────────────
ensure_env() {
  if [ ! -f ".env" ]; then
    warn ".env file not found. Copying from .env.example..."
    cp .env.example .env
    warn "Edit .env with your values before running in production!"
  fi
}

# ── Commands ──────────────────────────────────────────────────────────────────
case "${1:-start}" in
  start)
    check_docker
    ensure_env
    log "Building and starting all services..."
    docker compose up --build -d
    log "Waiting for backend health check..."
    sleep 5
    for i in {1..20}; do
      if curl -fsS http://localhost:8081/api/health &>/dev/null; then
        log "✓ Backend is UP"
        break
      fi
      echo -n "."
      sleep 4
    done
    echo ""
    log "✓ TradeWise is running!"
    echo -e "${BLUE}  Frontend:${NC} http://localhost"
    echo -e "${BLUE}  Backend: ${NC} http://localhost:8081/api/health"
    docker compose ps
    ;;

  stop)
    log "Stopping all services..."
    docker compose down
    log "All services stopped."
    ;;

  restart)
    log "Rebuilding and restarting..."
    docker compose down
    docker compose up --build -d
    ;;

  logs)
    docker compose logs -f --tail=100
    ;;

  status)
    docker compose ps
    ;;

  clean)
    warn "This will remove all containers AND the database volume!"
    read -rp "Are you sure? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
      docker compose down -v
      log "Cleaned up."
    else
      log "Aborted."
    fi
    ;;

  *)
    echo "Usage: ./start.sh [start|stop|restart|logs|status|clean]"
    exit 1
    ;;
esac
