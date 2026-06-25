#!/usr/bin/env bash
set -e

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
  echo ""
  echo "Encerrando serviços..."
  [ -n "$BACKEND_PID" ]  && kill "$BACKEND_PID"  2>/dev/null
  [ -n "$FRONTEND_PID" ] && kill "$FRONTEND_PID" 2>/dev/null
  docker compose -f "$REPO_ROOT/docker-compose.yml" stop
  echo "Pronto."
}
trap cleanup EXIT INT TERM

echo "==> Subindo Docker (db + mailhog)..."
docker compose -f "$REPO_ROOT/docker-compose.yml" up -d db mailhog

echo "==> Aguardando PostgreSQL ficar pronto..."
until docker compose -f "$REPO_ROOT/docker-compose.yml" exec -T db \
      pg_isready -U wachafit -q 2>/dev/null; do
  sleep 1
done
echo "    PostgreSQL pronto."

echo "==> Iniciando backend (porta 8080)..."
SPRING_PROFILES_ACTIVE=dev mvn -f "$REPO_ROOT/backend/pom.xml" spring-boot:run \
  --no-transfer-progress &
BACKEND_PID=$!

echo "==> Iniciando frontend (porta 5173)..."
npm --prefix "$REPO_ROOT/frontend" run dev &
FRONTEND_PID=$!

echo ""
echo "  Backend:  http://localhost:8080"
echo "  Frontend: http://localhost:5173"
echo "  Swagger:  http://localhost:8080/swagger-ui.html"
echo "  MailHog:  http://localhost:8025"
echo ""
echo "  Ctrl+C para encerrar tudo."
echo ""

wait
