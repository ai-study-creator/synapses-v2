#!/usr/bin/env bash
set -euo pipefail

APP_PORT="${APP_PORT:-8000}"
LISTEN_PORT="${LISTEN_PORT:-80}"
SERVER_NAME="${SERVER_NAME:-}"
SITE_NAME="${SITE_NAME:-synapses-v2}"
OPEN_FIREWALL=0
DRY_RUN=0

usage() {
  cat <<'USAGE'
Configure Nginx as a public reverse proxy for Synapses V2.

Usage:
  sudo ./scripts/configure-nginx-public-ip.sh [options]

Options:
  --server-name VALUE   Public IP address or DNS name. Defaults to auto-detect,
                        then "_" if detection fails.
  --app-port PORT       Local orchestrator port. Default: 8000.
  --listen-port PORT    Public Nginx listen port. Default: 80.
  --site-name NAME      Nginx site/config name. Default: synapses-v2.
  --open-firewall       If ufw is active, allow the Nginx listen port.
  --dry-run             Print the generated config without writing files.
  -h, --help            Show this help.

Environment variables can also be used:
  SERVER_NAME=203.0.113.10 APP_PORT=8000 LISTEN_PORT=80 SITE_NAME=synapses-v2
USAGE
}

detect_public_ip() {
  if command -v curl >/dev/null 2>&1; then
    curl -fsS --max-time 3 https://api.ipify.org 2>/dev/null || true
  fi
}

require_numeric_port() {
  local name="$1"
  local value="$2"

  if [[ ! "$value" =~ ^[0-9]+$ ]] || (( value < 1 || value > 65535 )); then
    echo "Invalid $name: $value" >&2
    exit 1
  fi
}

require_safe_value() {
  local name="$1"
  local value="$2"

  if [[ "$value" == *$'\n'* || "$value" == *";"* || "$value" == *"{"* || "$value" == *"}"* ]]; then
    echo "Invalid $name: $value" >&2
    exit 1
  fi
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --server-name)
      SERVER_NAME="${2:-}"
      shift 2
      ;;
    --app-port)
      APP_PORT="${2:-}"
      shift 2
      ;;
    --listen-port)
      LISTEN_PORT="${2:-}"
      shift 2
      ;;
    --site-name)
      SITE_NAME="${2:-}"
      shift 2
      ;;
    --open-firewall)
      OPEN_FIREWALL=1
      shift
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

require_numeric_port "APP_PORT" "$APP_PORT"
require_numeric_port "LISTEN_PORT" "$LISTEN_PORT"

if [[ ! "$SITE_NAME" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "Invalid SITE_NAME: $SITE_NAME" >&2
  exit 1
fi

if [[ -z "$SERVER_NAME" ]]; then
  SERVER_NAME="$(detect_public_ip)"
fi

if [[ -z "$SERVER_NAME" ]]; then
  SERVER_NAME="_"
fi

require_safe_value "SERVER_NAME" "$SERVER_NAME"

read -r -d '' NGINX_CONFIG <<EOF || true
server {
    listen ${LISTEN_PORT};
    listen [::]:${LISTEN_PORT};

    server_name ${SERVER_NAME};

    client_max_body_size 1m;

    location / {
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Connection "";

        proxy_pass http://127.0.0.1:${APP_PORT};
    }
}
EOF

if (( DRY_RUN )); then
  printf '%s\n' "$NGINX_CONFIG"
  exit 0
fi

if (( EUID != 0 )); then
  echo "This script writes to /etc/nginx and reloads Nginx. Re-run it with sudo." >&2
  exit 1
fi

if ! command -v nginx >/dev/null 2>&1; then
  echo "Nginx is not installed. Install it first, for example: sudo apt update && sudo apt install nginx" >&2
  exit 1
fi

if [[ -d /etc/nginx/sites-available && -d /etc/nginx/sites-enabled ]]; then
  CONFIG_PATH="/etc/nginx/sites-available/${SITE_NAME}"
  ENABLED_PATH="/etc/nginx/sites-enabled/${SITE_NAME}"
else
  CONFIG_PATH="/etc/nginx/conf.d/${SITE_NAME}.conf"
  ENABLED_PATH=""
fi

if [[ -f "$CONFIG_PATH" ]]; then
  BACKUP_PATH="${CONFIG_PATH}.bak.$(date +%Y%m%d%H%M%S)"
  cp "$CONFIG_PATH" "$BACKUP_PATH"
  echo "Backed up existing config to $BACKUP_PATH"
fi

printf '%s\n' "$NGINX_CONFIG" > "$CONFIG_PATH"

if [[ -n "$ENABLED_PATH" && ! -e "$ENABLED_PATH" ]]; then
  ln -s "$CONFIG_PATH" "$ENABLED_PATH"
fi

nginx -t

if command -v systemctl >/dev/null 2>&1; then
  systemctl reload nginx
else
  service nginx reload 2>/dev/null || nginx -s reload
fi

if (( OPEN_FIREWALL )) && command -v ufw >/dev/null 2>&1; then
  if ufw status | grep -q '^Status: active'; then
    ufw allow "$LISTEN_PORT"/tcp
  fi
fi

echo "Nginx is configured for http://${SERVER_NAME}:${LISTEN_PORT}/ -> http://127.0.0.1:${APP_PORT}/"
echo "Slack Events path: /slack/events"
echo "Slack URL verification requires HTTPS with a valid SSL certificate; add TLS before using this endpoint in Slack."
