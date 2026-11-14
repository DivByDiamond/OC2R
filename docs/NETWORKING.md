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

1. **Internet card enabled** in the common config (`internetCardEnabled = true`). The internet
   card and the internet gateway reach the internet directly from the server; they do **not**
   need the VXLAN tunnel (`vxlan.enable`), which is a separate feature. Older notes that said
   otherwise were wrong, and enabling VXLAN only opens a UDP port for nothing.
2. An **internet card** (PCI) installed into the computer, or an **internet gateway** block on
   the connector network.

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

### Limits and lifetimes

Idle times differ by kind, because a DNS lookup takes a new source port every time while an SSH
session may stay quiet for a long while:

| Option | Default | Applies to |
|--------|---------|------------|
| `defaultSessionLifetimeMs` | 60 s | connections that are being opened or closed |
| `tcpEstablishedLifetimeMs` | 1 h | established TCP connections |
| `datagramSessionLifetimeMs` | 15 s | UDP exchanges (DNS and the like) |
| `defaultSessionsNumberPerCardLimit` | 32 | TCP connections per card |
| `datagramSessionsNumberPerCardLimit` | 64 | UDP exchanges per card; the oldest is dropped when full |

An existing config file keeps the values it already has (the old per-card limit was 10), so raise
it by hand on a server that was set up earlier.

### Diagnostics

| Symptom | Likely cause |
|---------|--------------|
| `ping` fails, network otherwise fine | The native raw-socket ping needs root or `CAP_NET_RAW`. Without it the server runs the system `ping` command, so it must be installed on the server; as a last resort the JVM tries TCP port 7, which most hosts do not answer |
| First DNS lookup always timed out, later ones worked | Fixed: the datagram that opened a UDP exchange was dropped |
| Large DNS answers fail | An answer that does not fit into one frame (about 1470 bytes) is cut and flagged as truncated, so the resolver retries over TCP; other oversized UDP is dropped. Guests need a TCP-capable resolver |
| Idle SSH or long-poll connections die after a minute | Fixed: established connections now live for `tcpEstablishedLifetimeMs` |
| Network stops after a burst of lookups | Fixed: UDP exchanges no longer share the connection limit |
| DHCP client reports an address conflict | Fixed: the card ignores address probes and announcements |
| Connection hangs, no error | TTL exhausted mid-path: answered with ICMP Time Exceeded |

## VXLAN hub

The hub bridges a connector network to a UDP tunnel (`vxlan.enable`, off by default). One tunnel per server is bound to
`bindHost:bindPort` and talks to one peer, `remoteHost:remotePort`. Datagrams are accepted from the peer's address on
any source port, because the Linux VXLAN driver picks the source port per flow; the random per-hub VNI is the second
gate. Right-click a hub with an empty hand to see its VNI. On a Linux peer:

```sh
ip link add vx0 type vxlan id <VNI> remote <server> dstport 4789 local <peer address>
ip addr add 10.0.0.1/24 dev vx0 && ip link set vx0 up
```

Do not expose the port to the open internet: frames are not encrypted. The bundled OnyxOS guest has `ping`,
`tcpserve` and the `rshd` remote shell but no `sshd`.

### A shell in the guest from a PC

The hub side has no DHCP, so the guest is configured by hand. Put a network interface card (all sides enabled) in a
computer with the OnyxOS flash and wire it to the hub like this: a network
connector is placed against the computer, linked by network cable to a second connector, and that one is placed
against the hub (a hub only floods frames; connectors are what poll a computer's card and attach to the block behind them). Right-click the hub to read its VNI and set `vxlan.enable=true` and `remoteHost=<PC address>` in the server config.
In the guest (log in as `root`):

```text
netcfg list
netcfg set 0 192.168.77.2 255.255.255.0 0.0.0.0 0.0.0.0
rshd 2323 mysecret
```

On the PC, with the VNI from the hub:

```sh
ip link add vx0 type vxlan id <VNI> remote <server> dstport 4789 local <PC address>
ip addr add 192.168.77.1/24 dev vx0 && ip link set vx0 up
ping 192.168.77.2
ncat 192.168.77.2 2323        # or nc, socat; answer the password, then type shell lines
```

`rshd` is a plain line shell: one session at a time, no terminal and no encryption (see `docs/ONYXOS.md`), so the password
and everything typed travel in clear text through the tunnel. `scripts/demo/run-vxlan-shell.sh` repeats this setup
without root in network namespaces against the real `TunnelManager`.
