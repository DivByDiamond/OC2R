#!/usr/bin/env bash
# Demo endpoint configuration for the OC2R Onyx guest demo.
#
# The public demo backend is NOT part of this repository. Its address comes
# from the environment or from the git-ignored
# scripts/demo/demo-endpoints.local.sh (copy demo-endpoints.local.sh.example
# and fill it in). Only the private lab addresses have repository defaults:
# they are the QEMU/SLIRP NAT segment and the LAN pair of the demo arena, not
# reachable from the public Internet.
#
# Usage:
#   source scripts/demo/demo-endpoints.sh      # export the variables
#   bash scripts/demo/demo-endpoints.sh show   # print the resolved values
#                                             # and the guest command lines
#
# The gate scripts/demo/check-endpoints.sh keeps public addresses out of the
# demo sources; the policy lives in scripts/demo/endpoint-policy.txt.

_demo_endpoints_dir=$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)
_demo_endpoints_local="$_demo_endpoints_dir/demo-endpoints.local.sh"
if [ -f "$_demo_endpoints_local" ]; then
    # shellcheck source=scripts/demo/demo-endpoints.local.sh
    . "$_demo_endpoints_local"
fi

# --- public demo backend: no repository default ---------------------------
# Empty means "not configured"; the demo helpers then refuse to build a
# command line instead of pointing at a stale address.
: "${OC2R_DEMO_HTTP_HOST:=}"
: "${OC2R_DEMO_HTTP_PORT:=}"
: "${OC2R_DEMO_HTTP_PATH:=/oc2r}"
: "${OC2R_DEMO_TCP_HOST:=}"
: "${OC2R_DEMO_TCP_PORT:=}"

# --- lab addresses (private ranges, overridable) -------------------------
: "${OC2R_DEMO_NAT_IP:=10.0.2.15}"
: "${OC2R_DEMO_NAT_MASK:=255.255.255.0}"
: "${OC2R_DEMO_NAT_GATEWAY:=10.0.2.2}"
: "${OC2R_DEMO_DNS_SERVER:=1.1.1.1}"
: "${OC2R_DEMO_LAN_IP:=192.168.50.1}"
: "${OC2R_DEMO_LAN_PEER_IP:=192.168.50.2}"
: "${OC2R_DEMO_LAN_UDP_PORT:=12346}"
: "${OC2R_DEMO_LAN_TCP_PORT:=12345}"
: "${OC2R_DEMO_LAN_STREAM_PORT:=12348}"
: "${OC2R_DEMO_ROUTE_PROBE_IP:=1.1.1.1}"
: "${OC2R_DEMO_ROUTE_PROBE_PORT:=12347}"

export OC2R_DEMO_HTTP_HOST OC2R_DEMO_HTTP_PORT OC2R_DEMO_HTTP_PATH
export OC2R_DEMO_TCP_HOST OC2R_DEMO_TCP_PORT
export OC2R_DEMO_NAT_IP OC2R_DEMO_NAT_MASK OC2R_DEMO_NAT_GATEWAY
export OC2R_DEMO_DNS_SERVER
export OC2R_DEMO_LAN_IP OC2R_DEMO_LAN_PEER_IP
export OC2R_DEMO_LAN_UDP_PORT OC2R_DEMO_LAN_TCP_PORT OC2R_DEMO_LAN_STREAM_PORT
export OC2R_DEMO_ROUTE_PROBE_IP OC2R_DEMO_ROUTE_PROBE_PORT

_demo_endpoints_value() {
    if [ -z "${1:-}" ]; then
        printf '%s\n' "(unset)"
    else
        printf '%s\n' "$1"
    fi
}

_demo_endpoints_show() {
    printf 'public demo backend (from scripts/demo/demo-endpoints.local.sh or the environment)\n'
    printf '  OC2R_DEMO_HTTP_HOST  %s\n' "$(_demo_endpoints_value "$OC2R_DEMO_HTTP_HOST")"
    printf '  OC2R_DEMO_HTTP_PORT  %s\n' "$(_demo_endpoints_value "$OC2R_DEMO_HTTP_PORT")"
    printf '  OC2R_DEMO_HTTP_PATH  %s\n' "$(_demo_endpoints_value "$OC2R_DEMO_HTTP_PATH")"
    printf '  OC2R_DEMO_TCP_HOST   %s\n' "$(_demo_endpoints_value "$OC2R_DEMO_TCP_HOST")"
    printf '  OC2R_DEMO_TCP_PORT   %s\n' "$(_demo_endpoints_value "$OC2R_DEMO_TCP_PORT")"
    printf 'lab addresses\n'
    printf '  OC2R_DEMO_NAT_IP           %s\n' "$OC2R_DEMO_NAT_IP"
    printf '  OC2R_DEMO_NAT_MASK         %s\n' "$OC2R_DEMO_NAT_MASK"
    printf '  OC2R_DEMO_NAT_GATEWAY      %s\n' "$OC2R_DEMO_NAT_GATEWAY"
    printf '  OC2R_DEMO_DNS_SERVER       %s\n' "$OC2R_DEMO_DNS_SERVER"
    printf '  OC2R_DEMO_LAN_IP           %s\n' "$OC2R_DEMO_LAN_IP"
    printf '  OC2R_DEMO_LAN_PEER_IP      %s\n' "$OC2R_DEMO_LAN_PEER_IP"
    printf '  OC2R_DEMO_LAN_UDP_PORT     %s\n' "$OC2R_DEMO_LAN_UDP_PORT"
    printf '  OC2R_DEMO_LAN_TCP_PORT     %s\n' "$OC2R_DEMO_LAN_TCP_PORT"
    printf '  OC2R_DEMO_LAN_STREAM_PORT  %s\n' "$OC2R_DEMO_LAN_STREAM_PORT"
    printf '  OC2R_DEMO_ROUTE_PROBE_IP   %s\n' "$OC2R_DEMO_ROUTE_PROBE_IP"
    printf '  OC2R_DEMO_ROUTE_PROBE_PORT %s\n' "$OC2R_DEMO_ROUTE_PROBE_PORT"
    printf 'guest command lines\n'
    printf '  run /bin/netcfg set 0 %s %s %s %s\n' \
        "$OC2R_DEMO_NAT_IP" "$OC2R_DEMO_NAT_MASK" "$OC2R_DEMO_NAT_GATEWAY" "$OC2R_DEMO_DNS_SERVER"
    printf '  run /bin/netcfg set 1 %s %s 0.0.0.0 0.0.0.0\n' \
        "$OC2R_DEMO_LAN_IP" "$OC2R_DEMO_NAT_MASK"
    printf '  run /bin/udpserve %s\n' "$OC2R_DEMO_LAN_UDP_PORT"
    printf '  run /bin/udpdemo call %s %s OC2R-LAN-UDP\n' \
        "$OC2R_DEMO_LAN_IP" "$OC2R_DEMO_LAN_UDP_PORT"
    printf '  run /bin/tcpserve %s\n' "$OC2R_DEMO_LAN_TCP_PORT"
    printf '  run /bin/tcpdemo %s %s OC2R-LAN-TCP\n' \
        "$OC2R_DEMO_LAN_IP" "$OC2R_DEMO_LAN_TCP_PORT"
    if [ -n "$OC2R_DEMO_HTTP_HOST" ] && [ -n "$OC2R_DEMO_HTTP_PORT" ]; then
        printf '  run /bin/ohttp %s:%s%s /tmp/demo-http.txt\n' \
            "$OC2R_DEMO_HTTP_HOST" "$OC2R_DEMO_HTTP_PORT" "$OC2R_DEMO_HTTP_PATH"
    else
        printf '  run /bin/ohttp <HTTP_HOST>:<HTTP_PORT><HTTP_PATH> /tmp/demo-http.txt   # not configured\n'
    fi
    if [ -n "$OC2R_DEMO_TCP_HOST" ] && [ -n "$OC2R_DEMO_TCP_PORT" ]; then
        printf '  run /bin/tcpdemo %s %s OC2R-GUEST-PROBE\n' \
            "$OC2R_DEMO_TCP_HOST" "$OC2R_DEMO_TCP_PORT"
    else
        printf '  run /bin/tcpdemo <TCP_HOST> <TCP_PORT> OC2R-GUEST-PROBE             # not configured\n'
    fi
}

if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    case "${1:-show}" in
        show) _demo_endpoints_show ;;
        *)
            echo "Usage: $0 [show]" >&2
            exit 2
            ;;
    esac
fi
