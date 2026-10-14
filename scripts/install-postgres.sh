#!/usr/bin/env bash
# One-off host setup: PostgreSQL 18 from apt, a non-superuser role for the
# current user (peer auth over the Unix socket), and the two databases.
# pg_trgm is a trusted extension; the migration creates it without superuser.
set -euo pipefail
sudo apt-get install -y postgresql-18 postgresql-client-18
sudo -u postgres createuser "$USER" || true
sudo -u postgres createdb -O "$USER" life_on_rails || true
sudo -u postgres createdb -O "$USER" life_on_rails_test || true
psql -h /var/run/postgresql -d life_on_rails -Atc "select 'connected as ' || current_user"
