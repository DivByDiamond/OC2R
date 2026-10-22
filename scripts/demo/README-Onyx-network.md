# Onyx guest IPv4 networking

Base kernel: `dc3aa4bdfaa8d86d1b241d0056248685b1feac48` from the owning
OnyxKernel repository. Apply `onyx-network.patch` to an isolated copy; the
external source repository stays unchanged. It composes with
[the console patch](README-Onyx-console.md) and the RPC transport patch.

The stack retains up to eight VirtIO NICs with independent queues and polls
all configured interfaces. IPv4 longest-prefix routing selects a local subnet
before the configured default gateway. ARP entries and TCP connections belong
to their interface. Reconfiguring an interface closes affected sockets and
clears ARP entries. Configuring a nonzero gateway chooses that default interface.
DNS uses its configured DNS address. Boot still attempts DHCP on the first card
and falls back to `10.0.2.15/24`, gateway `10.0.2.2`, DNS `10.0.2.3`; the demo
configures the same segment explicitly through `OC2R_DEMO_NAT_IP`,
`OC2R_DEMO_NAT_GATEWAY` and `OC2R_DEMO_DNS_SERVER` (see the endpoint table
below).

Supported: IPv4, ARP, ICMP echo, DHCP client, DNS A queries, TCP client and
bounded TCP listeners, UDP datagrams with sender address/port/interface.
TCP retransmits unacknowledged segments with bounded retries and handles receive
windows. Limits include eight NICs, eight TCP listeners, four pending clients
per listener, eight queued UDP datagrams per socket, 1472-byte UDP payloads and
536-byte TCP send segments. IPv4 fragments are rejected. This is a small guest
stack: IPv6, TLS, IP forwarding, fragment reassembly, TCP congestion control and
socket compatibility with POSIX are not implemented.

## Build tools

```sh
bash scripts/demo/build-guest-tools.sh /path/to/OnyxCompiller /tmp/oc2r-guest-tools
```

Install the resulting `netcfg`, `udpdemo`, `udpserve`, `tcpdemo`, `tcpserve` and `tcpstream`
as `/bin/*` with the host OnyxFS image builder. The build script uses `--ring1`,
which is required by Onyx spawn privileges; use the compiler/libc patches beside
the RPC tools when rebuilding with the current OnyxCC. Do not modify a mounted image.
The shell accepts `run /bin/netcfg list`; examples below omit that prefix.

## Endpoints and environment variables

No address of the public demo backend is stored in this repository. The demo
reads its endpoints from the environment, or from the git-ignored
`scripts/demo/demo-endpoints.local.sh` (copy
`scripts/demo/demo-endpoints.local.sh.example` and fill it in). The variables
are defined in `scripts/demo/demo-endpoints.sh`; the private lab addresses have
repository defaults, the public backend has none.

| Variable | Meaning | Default |
| --- | --- | --- |
| `OC2R_DEMO_HTTP_HOST` | public demo backend host (HTTP) | unset -- required |
| `OC2R_DEMO_HTTP_PORT` | public demo backend HTTP port | unset -- required |
| `OC2R_DEMO_HTTP_PATH` | HTTP request path | `/oc2r` |
| `OC2R_DEMO_TCP_HOST` | public demo backend host (TCP echo) | unset -- required |
| `OC2R_DEMO_TCP_PORT` | public demo backend TCP echo port | unset -- required |
| `OC2R_DEMO_NAT_IP` | guest address of the QEMU/SLIRP NAT adapter | `10.0.2.15` |
| `OC2R_DEMO_NAT_MASK` | netmask of the NAT segment | `255.255.255.0` |
| `OC2R_DEMO_NAT_GATEWAY` | gateway of the NAT segment | `10.0.2.2` |
| `OC2R_DEMO_DNS_SERVER` | DNS server of the NAT adapter | `1.1.1.1` |
| `OC2R_DEMO_LAN_IP` | LAN address of the first guest | `192.168.50.1` |
| `OC2R_DEMO_LAN_PEER_IP` | LAN address of the second guest | `192.168.50.2` |
| `OC2R_DEMO_LAN_UDP_PORT` | UDP echo port of the LAN pair | `12346` |
| `OC2R_DEMO_LAN_TCP_PORT` | TCP echo port of the LAN pair | `12345` |
| `OC2R_DEMO_LAN_STREAM_PORT` | TCP stream port of the LAN pair | `12348` |
| `OC2R_DEMO_ROUTE_PROBE_IP` | off-subnet probe target | `1.1.1.1` |
| `OC2R_DEMO_ROUTE_PROBE_PORT` | off-subnet probe port | `12347` |

`bash scripts/demo/demo-endpoints.sh show` prints the resolved values and the
guest command lines to paste into the Minecraft terminal. The command examples
below use the placeholders of that table: `<NAT_IP>`, `<NAT_MASK>`,
`<NAT_GATEWAY>`, `<DNS_SERVER>`, `<LAN_IP>`, `<LAN_PEER_IP>`,
`<LAN_UDP_PORT>`, `<LAN_TCP_PORT>`, `<HTTP_HOST>`, `<HTTP_PORT>`,
`<HTTP_PATH>`, `<TCP_HOST>` and `<TCP_PORT>`.

## Guest commands (root)

Indices follow device-tree probe order, not Minecraft item slots. `netcfg list`
shows each MAC, address and the default index; identify actual card order first.
For Internet index 0 and LAN index 1, on PC1:

```text
netcfg list
netcfg set 0 <NAT_IP> <NAT_MASK> <NAT_GATEWAY> <DNS_SERVER>
netcfg set 1 <LAN_IP> <NAT_MASK> 0.0.0.0 0.0.0.0
udpserve <LAN_UDP_PORT>
```

On PC2, configure its Internet card the same way (the separate NAT adapter has
its own address space), configure its LAN address to `<LAN_PEER_IP>`, then:

```text
udpdemo call <LAN_IP> <LAN_UDP_PORT> OC2R-LAN-UDP
```

`UDP ROUNDTRIP: ONYX-UDP-ECHO` proves request and response. For TCP, start
`tcpserve <LAN_TCP_PORT>` on PC1, then on PC2:

```text
tcpdemo <LAN_IP> <LAN_TCP_PORT> OC2R-LAN-TCP
```

`TCP RECEIVED: ONYX-TCP-ECHO` proves the TCP exchange. Servers accept one
request; tools have 60-second guest deadlines. Reverse the roles to test the
other direction. `udpdemo send IP PORT MESSAGE` and `udpdemo recv PORT` remain
available for one-way markers.

Internet and LAN remain configured together. With a permitted external server
(the public demo backend, configured in the local endpoint file):

```text
ohttp <HTTP_HOST>:<HTTP_PORT><HTTP_PATH> /tmp/demo-http.txt
cat /tmp/demo-http.txt
tcpdemo <TCP_HOST> <TCP_PORT> OC2R-GUEST-PROBE
```

`ohttp` accepts numeric IPv4 or hostnames, optional `http://`, optional port and
path, and a mandatory output filename. It does not support HTTPS or redirects.
The Internet adapter does not provide DNS at the QEMU fallback `10.0.2.3`;
configure a reachable DNS server for hostname lookups. Minecraft server Internet
access and destination policy must permit the endpoint.

## Guest ABI

The extensions require root UID or root/kernel ring. Negative values are kernel
errno values; callers must validate errors. Existing TCP client calls remain.

| Number | Operation | Arguments |
| --- | --- | --- |
| 91 | interface config | 32-byte config pointer, operation (0 snapshot active; 1 apply; 2 snapshot index) |
| 92 | UDP bind | local port; returns socket |
| 93 | UDP send | destination IPv4 pointer, port, payload pointer, length |
| 94 | UDP receive | socket, buffer, capacity; zero also means no queued packet |
| 95 | UDP close | socket |
| 100 | TCP listen | port; returns listener |
| 101 | TCP accept | listener; returns connection or `-ENOENT` while empty |
| 102 | TCP unlisten | listener |
| 103 | UDP receive from | socket, buffer, capacity, 8-byte peer pointer; `-ENOENT` while empty |
| 104 | UDP send from | socket, peer pointer, payload pointer, length |

Config layout: little-endian u32 index, IPv4, gateway, mask, DNS (four bytes
each), six MAC bytes, enabled byte, default-index byte, little-endian u32 count.
The peer layout is IPv4, big-endian u16 port, interface byte, reserved byte.
UDP receives consume one whole datagram even if truncated; syscall 103 preserves
zero-length messages. See guest sources for exact wrappers.

## Verification

The merged source passes 191 kernel tests and an S-mode build. The six guest
tools compile with OnyxCC. Runtime verification must inspect received markers
and HTTP bodies; a successful send alone does not prove delivery. The standalone
Sedna pair fixture and Minecraft arena provide separate integration checks.


`run-network-pair.sh` takes the Sedna runtime classpath, firmware, kernel binary,
root image and output directory. It boots two independent RV64 boards with two
real VirtIO NICs each. NIC1 queues connect the guests directly; NIC0 has a small
ARP-only fixture gateway. The test checks literal UDP and TCP request/reply
markers on LAN, then verifies an off-subnet UDP marker emerges on NIC0. That
last check proves routing, not connectivity to an external Internet server.
Actual HTTP/TCP Internet verification belongs to the Minecraft arena.


The independent review additionally covers atomic PID ownership for TCP
connections, accepted clients, listeners and UDP sockets. A different process
cannot operate on guessed IDs. Process exit releases owned listeners and UDP
ports before taking process-list/runqueue locks; explicitly closed TCP sockets
retain bounded kernel-owned FIN/data retries. Numeric socket IDs are not
inherited by forked processes. Reads reopen the advertised receive window;
duplicate/persist segments receive ACKs, and CLOSE_WAIT allows responses after
a peer half-closes its write side.

Verified standalone checkpoint: UDP printed `UDP ROUNDTRIP: ONYX-UDP-ECHO`,
TCP printed `TCP RECEIVED: ONYX-TCP-ECHO`, and `tcpstream` delayed its reads then
verified 8192 patterned bytes, exceeding the 2048-byte receive buffer and
exercising receive ring rollover. The final 191-test kernel, including process-exit cleanup, finished
`NETWORK_PAIR_PASS routed=1 bridged=69 internetFrames=1,3`.
