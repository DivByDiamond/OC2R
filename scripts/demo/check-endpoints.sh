#!/usr/bin/env bash
# Gate for the demo sources: no public IPv4 address and no public demo backend
# port may be hardcoded in scripts/demo.
#
# The policy (allowed ranges, explicitly allowed literals, forbidden demo
# backend ports) is read from one place: scripts/demo/endpoint-policy.txt.
# The public backend address itself is not in the repository -- it lives in the
# git-ignored scripts/demo/demo-endpoints.local.sh or in the environment, and
# the sources refer to it through the OC2R_DEMO_* variables.
#
# Usage: bash scripts/demo/check-endpoints.sh
set -euo pipefail

demo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
policy="$demo_dir/endpoint-policy.txt"

if [ ! -f "$policy" ]; then
    echo "check-endpoints: missing policy file $policy" >&2
    exit 2
fi

# The policy file and the local (git-ignored) configuration are not sources:
# the first is the policy, the second holds the real addresses by design.
mapfile -t scanned < <(
    find "$demo_dir" -type f \
        \( -name '*.c' -o -name '*.h' -o -name '*.java' -o -name '*.sh' \
           -o -name '*.example' -o -name '*.md' -o -name '*.txt' -o -name '*.py' \
           -o -name '*.patch' \) \
        ! -path '*/build/*' \
        ! -name 'endpoint-policy.txt' \
        ! -name 'demo-endpoints.local.sh' \
        | sort
)

if [ "${#scanned[@]}" -eq 0 ]; then
    echo "check-endpoints: no demo sources found under $demo_dir" >&2
    exit 2
fi

# The local configuration holds the real addresses by design, so it is not
# scanned -- but it must never be committed. A tracked copy would leak the
# backend into the repository, so the gate refuses it.
local_config="$demo_dir/demo-endpoints.local.sh"
if [ -f "$local_config" ] && command -v git >/dev/null 2>&1 \
    && git -C "$demo_dir" rev-parse --is-inside-work-tree >/dev/null 2>&1 \
    && git -C "$demo_dir" ls-files --error-unmatch -- demo-endpoints.local.sh >/dev/null 2>&1; then
    echo "check-endpoints: scripts/demo/demo-endpoints.local.sh is tracked by git" >&2
    echo "check-endpoints: the local endpoint file must stay untracked" >&2
    exit 1
fi

# The classification runs in awk: it reads the policy and decides for every
# literal whether it is inside an allowed range or an explicitly allowed
# literal. Everything else is a public address and fails the gate.
classifier=$(mktemp "${TMPDIR:-/tmp}/oc2r-endpoint-classifier.XXXXXX.awk")
literals=$(mktemp "${TMPDIR:-/tmp}/oc2r-endpoint-literals.XXXXXX")
trap 'rm -f "$classifier" "$literals"' EXIT
cat > "$classifier" <<'AWK'
function pow2(n,   i, r) { r = 1; for (i = 0; i < n; i++) r = r * 2; return r }
function ip2bits(ip,   a, n, i, v, j, out, b) {
    n = split(ip, a, ".")
    if (n != 4) return ""
    out = ""
    for (i = 1; i <= 4; i++) {
        if (a[i] !~ /^[0-9]+$/) return ""
        v = a[i] + 0;
        if (v > 255) return ""
        b = "";
        for (j = 7; j >= 0; j--) b = b ((int(v / pow2(j)) % 2) ? "1" : "0");
        out = out b;
    }
    return out;
}
BEGIN {
    while ((getline line < policy) > 0) {
        sub(/#.*/, "", line);
        gsub(/^[ \t\r]+|[ \t\r]+$/, "", line);
        if (line == "") continue;
        split(line, part, /[ \t]+/);
        kind = part[1];
        value = part[2];
        if (kind == "allow-range") {
            split(value, cidr, "/");
            bits = ip2bits(cidr[1]);
            if (bits == "" || cidr[2] !~ /^[0-9]+$/ || (cidr[2] + 0) > 32) {
                printf "policy error: bad allow-range %s\n", value;
                policy_error = 1;
                continue;
            }
            range_count++;
            range_prefix[range_count] = bits;
            range_bits[range_count] = cidr[2] + 0;
        } else if (kind == "allow-literal") {
            if (ip2bits(value) == "") {
                printf "policy error: bad allow-literal %s\n", value;
                policy_error = 1;
                continue;
            }
            allowed_literal[value] = 1;
        } else if (kind == "forbid-port") {
            forbidden_port[value] = 1;
            port_count++;
        } else {
            printf "policy error: unknown directive %s\n", kind;
            policy_error = 1;
        }
    }
    close(policy);
}
{
    # Input lines are "file:line:literal".
    literal = $3;
    if (literal in allowed_literal) next;
    bits = ip2bits(literal);
    if (bits == "") next;
    for (i = 1; i <= range_count; i++) {
        if (substr(bits, 1, range_bits[i]) == substr(range_prefix[i], 1, range_bits[i])) next;
    }
    printf "%s:%s: public IPv4 literal %s is not allowed in the demo sources\n", $1, $2, literal;
    violations++;
}
END {
    if (policy_error) exit 3;
    if (violations) exit 1;
}
AWK

failures=0

echo "check-endpoints: scanning ${#scanned[@]} demo source file(s) against $(basename "$policy")"
# The extraction and the classification run separately: with `pipefail` a
# pipeline would report the exit code of grep (1 = "no literal found"), which
# is not a gate failure.
# Dotted quad notation (203.0.113.7) ...
grep -n -o -E '([0-9]{1,3}\.){3}[0-9]{1,3}' "${scanned[@]}" > "$literals" || true
# ... and the byte array notation of the kernel sources ([203,0,113,7]), which
# is normalized into a dotted quad before the classification.
grep -n -o -E '\[[0-9]{1,3}[[:space:]]*,[[:space:]]*[0-9]{1,3}[[:space:]]*,[[:space:]]*[0-9]{1,3}[[:space:]]*,[[:space:]]*[0-9]{1,3}[[:space:]]*\]' \
    "${scanned[@]}" \
    | sed -E 's/\[[[:space:]]*([0-9]{1,3})[[:space:]]*,[[:space:]]*([0-9]{1,3})[[:space:]]*,[[:space:]]*([0-9]{1,3})[[:space:]]*,[[:space:]]*([0-9]{1,3})[[:space:]]*\]/\1.\2.\3.\4/' \
    >> "$literals" || true
classify_status=0
awk -F: -v policy="$policy" -f "$classifier" "$literals" || classify_status=$?
case "$classify_status" in
    0) ;;
    1) failures=1 ;;
    3)
        echo "check-endpoints: the endpoint policy is broken" >&2
        exit 2
        ;;
    *)
        echo "check-endpoints: the endpoint classifier failed (exit $classify_status)" >&2
        exit 2
        ;;
esac

# The ports of the public demo backend must not be hardcoded either.
port_pattern=""
while read -r port; do
    [ -n "$port" ] || continue
    if [ -n "$port_pattern" ]; then
        port_pattern="$port_pattern|"
    fi
    port_pattern="$port_pattern(^|[^0-9])${port}([^0-9]|$)"
done < <(sed -e 's/#.*//' -e 's/[[:space:]]\+/ /g' "$policy" \
    | awk '$1 == "forbid-port" { print $2 }')

if [ -n "$port_pattern" ]; then
    if grep -n -E "$port_pattern" "${scanned[@]}"; then
        echo "check-endpoints: a public demo backend port is hardcoded in the demo sources" >&2
        failures=1
    fi
fi

if [ "$failures" -ne 0 ]; then
    echo "check-endpoints: FAILED -- use the OC2R_DEMO_* variables instead of hardcoded endpoints" >&2
    exit 1
fi

echo "check-endpoints: OK -- no public address and no demo backend port in the demo sources"
