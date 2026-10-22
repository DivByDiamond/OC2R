# OC2R demo scripts

Guest-side tooling, host-side QA harnesses and Onyx kernel patches for the
OC2R OnyxOS demonstration. Nothing in this directory is part of the Minecraft
mod: the sources are compiled for the Onyx guest (C), for the host (C, Java,
shell) or applied to a private OnyxKernel checkout (patches).

| Path | Content |
| --- | --- |
| `guest/` | the guest tools (`netcfg`, `udpdemo`, `udpserve`, `tcpdemo`, `tcpserve`, `tcpstream`, `oc2ctl`) and the native RPC library |
| `qa/` | host harnesses that boot real RV64 guests in Sedna (`NetworkPair.java`) |
| `tests/` | host fixtures and the RPC regression test (`test_rpc.py`) |
| `*.patch` | Onyx kernel, compiler, libc and OnyxApps patches |
| `build-*.sh`, `rebuild-onyx-userspace.sh`, `run-network-pair.sh` | the manual build and run steps |
| `README-Onyx-*.md` | the per-topic documentation (network, console, build) |

The guest tools take every address and port as an argument: `netcfg set NIC IP
MASK GATEWAY DNS`, `udpdemo call IP PORT MESSAGE`, `tcpdemo IP PORT MESSAGE`.
The tools themselves contain no address at all; the addresses live in the
environment (see below) and in the host harnesses.

## Checks

The demo sources are checked by `scripts/demo/run-checks.sh`, which is the same
entry point locally and in CI (the `demo-scripts` job of
`.github/workflows/ci-work.yml`). None of the checks starts Minecraft, touches
the network or needs the Onyx toolchain.

```sh
bash scripts/demo/run-checks.sh all        # every check
bash scripts/demo/run-checks.sh shell      # bash -n over the demo shell scripts
bash scripts/demo/run-checks.sh c          # syntax-only compile of the C sources
bash scripts/demo/run-checks.sh python     # the guest RPC test
bash scripts/demo/run-checks.sh java       # compile the demo Java harnesses
bash scripts/demo/run-checks.sh endpoints  # the hardcoded-endpoint gate
```

What each check does:

- **shell** -- `bash -n` over every `*.sh` in this directory.
- **c** -- `cc -fsyntax-only` over the guest tools and the host fixtures. The
  guest tools target OnyxCC and include the Onyx headers `<syscalls.h>` and
  `<net.h>`; CI has no Onyx toolchain, so the syntax-only stubs in
  `tests/onyx-stubs/` stand in for them. The stubs are not an implementation:
  the real build still uses OnyxCC and the real headers.
- **python** -- `tests/test_rpc.py` compiles the native RPC library for the
  host and drives it over a pseudo terminal (fragmented frames, oversized
  replies, malformed JSON, an unsolicited event). It needs a C compiler, but
  neither Minecraft nor a network.
- **java** -- `javac` over `qa/NetworkPair.java` and `tests/RpcTransportProbe.java`
  against the Sedna runtime (`scripts/download-libs.sh` puts it into `libs/`).
  `RpcTransportProbe.java` additionally needs Gson; when Gson is neither in the
  Gradle cache nor downloadable, that single harness is skipped with a warning
  and the rest of the check still runs.
- **endpoints** -- the gate below.

### The endpoint gate

`scripts/demo/check-endpoints.sh` fails when a public IPv4 address or a port
of the public demo backend is hardcoded in the demo sources. The policy lives in
one place, `scripts/demo/endpoint-policy.txt`: the allowed private and
documentation ranges, the literals that are allowed on purpose (each with a
reason) and the forbidden demo backend ports. The public backend address itself
is not in the repository, so the range rules catch it without naming it.

Both notations of an address are checked: the dotted quad (`203.0.113.7`) and
the byte array of the kernel sources (`[203,0,113,7]`). The Onyx kernel patch
carries the test fixtures of the routing function; they use the RFC 5737
documentation ranges, so no public address survives in the repository.

## Endpoints

The public demo backend is a temporary service; its address is not stored in
the repository. It comes from the environment or from the git-ignored
`scripts/demo/demo-endpoints.local.sh`:

```sh
cp scripts/demo/demo-endpoints.local.sh.example scripts/demo/demo-endpoints.local.sh
# fill in the host and the ports of the backend
bash scripts/demo/demo-endpoints.sh show
```

`scripts/demo/demo-endpoints.sh` defines the variables and loads the local
file; `show` prints the resolved values and the guest command lines to paste
into the Minecraft terminal.

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

An unset host or port means "not configured": the helpers print a placeholder
instead of a stale address. The private lab addresses have repository defaults
because they are not reachable from the public Internet; override them in the
local file when the arena uses another segment.

The topic documentation uses the placeholders of that table: see
[README-Onyx-network.md](README-Onyx-network.md) for the guest commands of the
network demo and [README-Onyx-build.md](README-Onyx-build.md) for the kernel,
userspace and image rebuild.

### Known gap: the LAN pair harness still hardcodes its addresses

`qa/NetworkPair.java` still contains the NAT and LAN addresses of the two
guests it boots. It has to read them from the `OC2R_DEMO_*` variables with the
same defaults as `demo-endpoints.sh` (`OC2R_DEMO_NAT_IP`,
`OC2R_DEMO_NAT_MASK`, `OC2R_DEMO_NAT_GATEWAY`, `OC2R_DEMO_DNS_SERVER`,
`OC2R_DEMO_LAN_IP`, `OC2R_DEMO_LAN_PEER_IP` and the `OC2R_DEMO_LAN_*_PORT`
values) instead of the literals. The gate tolerates the literals for now
because they are private addresses, but a public address in that file would
fail the build.
