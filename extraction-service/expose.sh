#!/usr/bin/env bash
# expose.sh — print options for reaching the extraction service from an Android
# device. Usage:
#   ./expose.sh            # show LAN URL + suggest tunnels
#   ./expose.sh lan        # only print LAN URL
#   ./expose.sh cloudflared# spawn a cloudflared quick tunnel
#   ./expose.sh ngrok      # spawn an ngrok tunnel (requires authtoken)
set -euo pipefail

PORT="${PORT:-3000}"
MODE="${1:-info}"

lan_ip() {
  case "$(uname -s)" in
    Darwin)
      ipconfig getifaddr en0 2>/dev/null \
        || ipconfig getifaddr en1 2>/dev/null \
        || ipconfig getifaddr en2 2>/dev/null \
        || echo ""
      ;;
    Linux)
      hostname -I 2>/dev/null | awk '{print $1}'
      ;;
    *)
      echo ""
      ;;
  esac
}

print_lan() {
  local ip
  ip="$(lan_ip)"
  if [[ -z "$ip" ]]; then
    echo "could not detect LAN IP — set EXTRACTION_BASE_URL_DEBUG manually" >&2
    return 1
  fi
  echo "LAN URL:           http://${ip}:${PORT}/"
  echo "Emulator URL:      http://10.0.2.2:${PORT}/"
}

print_local_props_hint() {
  cat <<EOF

Put one of these in <repo-root>/local.properties and re-build the app:
  EXTRACTION_BASE_URL_DEBUG=http://${1}:${PORT}/

Then rebuild: ./gradlew :app:assembleDebug
EOF
}

case "$MODE" in
  info|"")
    echo "==> ReelGrab extraction service exposure"
    print_lan
    ip="$(lan_ip)"
    [[ -n "$ip" ]] && print_local_props_hint "$ip"
    echo
    echo "For a public HTTPS URL (real device, no VPN, no LAN):"
    echo "  ./expose.sh cloudflared   # free, no signup"
    echo "  ./expose.sh ngrok         # requires NGROK_AUTHTOKEN"
    ;;

  lan)
    print_lan
    ;;

  cloudflared)
    command -v cloudflared >/dev/null 2>&1 || {
      echo "cloudflared not installed. macOS:  brew install cloudflared" >&2
      exit 1
    }
    echo "==> cloudflared tunnel -> http://localhost:${PORT}"
    echo "    Look for the https://*.trycloudflare.com URL below and set it as"
    echo "    EXTRACTION_BASE_URL_DEBUG in local.properties."
    exec cloudflared tunnel --url "http://localhost:${PORT}"
    ;;

  ngrok)
    command -v ngrok >/dev/null 2>&1 || {
      echo "ngrok not installed. macOS:  brew install ngrok/ngrok/ngrok" >&2
      exit 1
    }
    echo "==> ngrok tunnel -> http://localhost:${PORT}"
    exec ngrok http "${PORT}"
    ;;

  *)
    echo "Usage: $0 [info|lan|cloudflared|ngrok]" >&2
    exit 1
    ;;
esac
