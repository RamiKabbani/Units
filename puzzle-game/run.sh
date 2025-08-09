#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
PORT=${PORT:-5173}
python3 -m http.server "$PORT" --bind 127.0.0.1 | cat