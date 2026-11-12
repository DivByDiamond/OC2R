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
