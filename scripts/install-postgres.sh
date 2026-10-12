#!/usr/bin/env bash
# Host runbook helper: install PostgreSQL 18 + pgvector via apt (operator runs
# with sudo; NOT invoked by any code PR). Then:
#   sudo -u postgres createuser "$USER"
#   sudo -u postgres createdb -O "$USER" life_on_rails
#   sudo -u postgres psql -d life_on_rails -c 'CREATE EXTENSION IF NOT EXISTS vector;'
set -euo pipefail
sudo apt-get update
sudo apt-get install -y postgresql-18 postgresql-client-18 postgresql-18-pgvector
echo "Postgres 18 installed. Next (as postgres superuser):"
echo "  sudo -u postgres createuser \"\$USER\""
echo "  sudo -u postgres createdb -O \"\$USER\" life_on_rails"
echo "  sudo -u postgres psql -d life_on_rails -c 'CREATE EXTENSION IF NOT EXISTS vector;'"
