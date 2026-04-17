#!/usr/bin/env bash
set -e

GREEN="\033[0;32m"
CYAN="\033[0;36m"
BOLD="\033[1m"
RED="\033[1;31m"
RESET="\033[0m"

here="$(cd "$(dirname "$0")" && pwd)"
certs_dir="$here/certs"
required_certs=(root-cert.pem server.crt server.key cert.pem key.pem key.pk8 pg_hba.conf)

missing=0
for f in "${required_certs[@]}"; do
  if [ ! -f "$certs_dir/$f" ]; then
    missing=1
    break
  fi
done

if [ "$missing" -eq 1 ]; then
  echo -e "${CYAN}Certs missing — generating…${RESET}"
  "$here/generate-certs.sh"
else
  echo -e "${GREEN}✔ Certs already present${RESET}"
fi

running_services="$(
  docker compose ps --format json 2>/dev/null \
  | jq -r 'select(.State=="running") | .Service' \
  | sort -u
)"

echo $running_services

if echo "$running_services" | grep -qx "postgres"; then
  echo -e "${GREEN}✔ Postgres service is already running in Docker Compose${RESET}"
  exit 0
fi

echo -e "${CYAN}Starting Postgres service in Docker Compose…${RESET}"

echo "what"

# Suppress the ugly compose chatter unless it fails
output=$(docker compose up -d --remove-orphans 2>&1)
if [ $? -eq 0 ]; then
  echo -e "${GREEN}${BOLD}✔ Compose started!${RESET}"
else
  echo -e "${RED}${BOLD}✖ Docker compose failed to start${RESET}"
  echo "$output"
  exit 1
fi
