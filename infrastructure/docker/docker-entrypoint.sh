#!/bin/sh
set -e

APP_PORT="${APP_PORT:-8080}"
NGINX_APP_HOST="${NGINX_APP_HOST:-app}"

# Use sed to substitute variables in nginx config template
sed "s|\$APP_PORT|${APP_PORT}|g; s|\$NGINX_APP_HOST|${NGINX_APP_HOST}|g" \
  /etc/nginx/conf.d/default.conf.tpl > /etc/nginx/conf.d/default.conf

# Start nginx
exec nginx -g "daemon off;"

