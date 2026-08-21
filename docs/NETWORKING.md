# Networking Stack

OC2R implements a full TCP/IP network stack for virtual computers, plus VXLAN tunneling for external connectivity.

## Architecture

```
 Application
     ↓
 Session Layer (TCP/UDP sockets)
     ↓
 Transport Layer (TCP state machine / UDP)
     ↓
 Network Layer (IPv4)
     ↓
 Link Layer (ARP)
     ↓
 VXLAN Tunnel ←→ Native oc2rnet lib ←→ Real network
```

## Protocol Stack

### Link Layer — ARP

- `ArpProtocol` — address resolution protocol
- Maintains mapping between IPv4 addresses and MAC addresses
- Handles ARP requests and replies

### Network Layer — IPv4

- `DefaultNetworkLayer` — packet send/receive with routing
- `Ipv4Space` — IPv4 address space management (subnets, ranges, CIDR)
- `MacAddress` — 6-byte MAC address handling

### Transport Layer — TCP

Full TCP state machine with 7 states:

- `AcceptState` — listening for connections
- `ConnectState` — SYN-SENT / SYN-RECEIVED
- `EstablishedState` — data transfer
- `FinishState` — FIN-WAIT / CLOSE-WAIT
- `ExpiredState` — timed-out connection
- `RejectState` — RST sent/received

Key classes:

- `TcpHeader` — TCP segment parsing/writing
- `TcpState` — per-connection state machine
- `TcpStates` — static state instances
- `TcpUtils` — sequence number handling

### Transport Layer — UDP

- Datagram-oriented, no state machine
- `DatagramSession` — send/receive datagrams

### Session Layer

- `DefaultSessionLayer` — socket management
- `SocketManager` — ephemeral port allocation
- `SessionManager` — session lifecycle

Session types:

| Type | Class | Protocol |
|------|-------|----------|
| Stream | `StreamSessionImpl` | TCP |
| Datagram | `DatagramSessionImpl` | UDP |
| Echo | `EchoSessionImpl` | ICMP echo |

### ICMP

- `IcmpHandler` — ICMP packet processing
- `ICMPReply` — echo reply generation
- `EchoHandler` — ping response handling

## Internet Connectivity

- `InternetManagerImpl` — manages internet thread and connections
- `InternetAdapter` — bridges VM network to real internet
- `DefaultInternetProvider` — default connectivity provider
- `InternetConnection` — per-connection state

## Setting Up Internet In The Guest OS

### Requirements

1. **VXLAN enabled** in the common config (`vxlan.enable` / hub block set up) — the internet
   card silently does nothing without it (a WARN is logged at startup since 0.1.1).
2. **Internet card enabled** in the common config (`internetCardEnabled = true`).
3. An **internet card** (PCI) installed into the computer.

### Addressing

There is **no DHCP**. The card behaves as a point-to-point link:

- The guest assigns itself any IP address allowed by the config (see below).
- The card answers ARP requests for the gateway side, so any gateway IP works.
- DNS: the card does not run a resolver. Point the guest's `/etc/resolv.conf` at the
  configured `defaultNameServer` (default `1.1.1.1`) or any reachable resolver.

Example (Minux/guest shell):

```sh
ip addr add 10.0.2.15/24 dev eth0    # any allowed address works
ip route add default via 10.0.2.2    # the card answers ARP for this
echo "nameserver 1.1.1.1" > /etc/resolv.conf
ping 1.1.1.1
```

### Access Control

- `deniedHosts` (CIDR list) — destinations VMs may not reach. By default all private
  ranges are denied; packets to them are dropped **silently** (no ICMP error, by design).
- `allowedHosts` — inverse mode: if non-empty, only these may be reached.
- Only one of the two lists may be non-empty.

### Diagnostics

| Symptom | Likely cause |
|---------|--------------|
| `ping` always fails, network otherwise fine | On dedicated servers without `CAP_NET_ROOT`/root, the JVM-level ICMP fallback reports false negatives; a WARN is logged once |
| Large DNS responses time out | Now answered with ICMP frag-needed (MTU 1500); older builds dropped fragments silently |
| Connection hangs, no error | TTL exhausted mid-path → now answered with ICMP Time Exceeded |
| Port unreachable replies come from `0.0.0.0` | Fixed in 0.1.1 — they now carry the unreachable host's address |

## VXLAN Tunneling

`VxlanBlockEntity` and `TunnelManager` provide outer-network connectivity via VXLAN:

- UDP-based tunneling
- Uses native `oc2rnet` library for ICMP+UDP (platform-specific: `.so`, `.dylib`, `.dll`)
- Falls back to JVM implementation if native lib unavailable
- Background thread for packet reception
- Multiple tunnel interfaces by VNI (VXLAN Network Identifier)

### Native Library

Prebuilt binaries in `src/main/resources/natives/`:

| Platform | File |
|----------|------|
| Linux x86_64 | `liboc2rnet-linux-x86_64.so` |
| Linux ARM64 | `liboc2rnet-linux-arm64.so` |
| macOS x86_64 | `liboc2rnet-x86_64.dylib` |
| macOS ARM64 | `liboc2rnet-arm64.dylib` |
| Windows x86_64 | `oc2rnet-x86_64.dll` |
| Windows ARM64 | `oc2rnet-arm64.dll` |
| Android ARM64 | `liboc2rnet-android-arm64.so` |
| Android x86_64 | `liboc2rnet-android-x86_64.so` |

## Related

- [Architecture](ARCHITECTURE.md) — threading model
- [Device Types](DEVICES.md) — network interface card device
- [Source Structure](SRC_STRUCTURE.md) — code layout
