#!/usr/bin/env bash
# Reach the Onyx remote shell from an ordinary Linux peer through a kernel VXLAN device.
#
# Without root: `unshare -rn` gives this script a private network namespace that plays the Minecraft
# server and a child namespace that plays the PC. The server side runs one real RV64 OnyxOS guest
# (Sedna) whose VirtIO NIC is the production TunnelManager's tunnel interface; the PC side has a
# kernel `vxlan` device and talks to the guest with ping and ncat. The hub block entity itself is not
# part of this run; RemoteShellTests covers it in game.
#
# usage: unshare -rn scripts/demo/run-vxlan-shell.sh <mod classes dir> <dependency classpath> \
#            <fw_jump.bin> <onyx-kernel.bin> <onyxfs.img> <output dir>
# <mod classes dir> is the compiled main source set (versions/1.21.1/build/classes/java/main); the
# dependency classpath needs the Sedna jar and log4j-api, jetbrains annotations and spotbugs-annotations.
set -euo pipefail
if [ "$#" -ne 6 ]; then
    sed -n '2,/^set -euo/p' "$0" | sed '$d' >&2
    exit 2
fi
classes=$(realpath "$1"); deps=$2; firmware=$(realpath "$3"); kernel=$(realpath "$4"); image=$(realpath "$5")
out=$(realpath -m "$6")
here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
vni=${VNI:-4242}
mkdir -p "$out"
javac -cp "$classes:$deps" -d "$out/classes" "$here/qa/stub/li/cil/oc2/common/config/Config.java" "$here/qa/OnyxVxlanGuest.java"
class_path="$out/classes:$classes:$deps"

ip link set lo up
unshare -n sleep 300 &
pc_pid=$!
trap 'kill $pc_pid 2>/dev/null || true' EXIT
sleep 0.5
ip link add veth0 type veth peer name veth1 netns "$pc_pid"
ip addr add 10.0.0.1/24 dev veth0
ip link set veth0 up
pc() { nsenter -t "$pc_pid" -n "$@"; }
pc ip link set lo up
pc ip addr add 10.0.0.2/24 dev veth1
pc ip link set veth1 up
# Exactly the command of docs/NETWORKING.md, with the VNI the hub reports.
pc ip link add vx0 type vxlan id "$vni" dstport 4789 remote 10.0.0.1 local 10.0.0.2
pc ip addr add 192.168.77.1/24 dev vx0
pc ip link set vx0 up
# A veth underlay advertises checksum offload, so the kernel hands the inner TCP checksum to the
# "hardware" half computed; the guest stack, like any real receiver of a raw frame, rejects that.
# Offload off on the PC side makes the kernel finish the checksums itself (ethtool -K <dev> tx off).
pc python3 "$here/qa/tx_offload_off.py" veth1 vx0

rm -f "$out/stop"
java -Xmx1G -cp "$class_path" OnyxVxlanGuest "$firmware" "$kernel" "$image" 10.0.0.1 10.0.0.2 "$vni" 192.168.77.2 \
    "$out/stop" "$out/guest-console.log" > "$out/guest.log" 2>&1 &
guest_pid=$!
for _ in $(seq 1 300); do grep -q GUEST_READY "$out/guest.log" 2>/dev/null && break; sleep 1; done
grep GUEST_READY "$out/guest.log"

client=$(command -v ncat || command -v nc)
if command -v tcpdump >/dev/null 2>&1; then
    pc tcpdump -l -n -i vx0 > "$out/vx0-capture.txt" 2>&1 &
    capture_pid=$!
    sleep 0.5
fi
echo "--- ping through the VXLAN device"
pc ping -c 3 -W 3 192.168.77.2 | tee "$out/ping.log" | tail -3
echo "--- remote shell with $(basename "$client")"
printf 'pw-from-pc\necho hello from the guest\ncd /etc\npwd\nls\nexit\n' \
    | pc timeout 60 "$client" -w 20 192.168.77.2 2323 | tee "$out/session.log"
kill "${capture_pid:-0}" 2>/dev/null || true
touch "$out/stop"
wait "$guest_pid"
cat "$out/guest.log"
grep -q "hello from the guest" "$out/session.log" && grep -q 'shadow' "$out/session.log" && echo "VXLAN_SHELL_PASS"
