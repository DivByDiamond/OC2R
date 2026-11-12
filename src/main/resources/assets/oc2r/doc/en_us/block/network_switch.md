# Network Switch
![Learns where hosts are](block:oc2r:network_switch)

The network switch connects up to six [network connectors](network_connector.md), one on each face. It remembers on which face it last saw the sender of a packet and forwards a packet addressed to that sender only there. A packet for an address it has not seen yet is sent to every other face. This is the difference to the [network hub](network_hub.md), which repeats every packet everywhere. Every forwarding step costs one hop of the packet's time to live.

Addresses are forgotten after two minutes without traffic. A port is never used to send a packet back to the face it came from unless hairpin is enabled for it.

The switch is also a device on the device bus when a [bus cable](bus_cable.md) touches it. It has the type name `switch` and these methods:

- `getHostTable()` returns the known hosts, each with its MAC address, the age of the entry in ticks and the face.
- `getLinkState()` returns, for each face, whether a connector or another network device is attached.
- `getPortConfig()` returns the settings of each face.
- `setPortConfig(settings)` replaces them. A setting has `untagged` (the VLAN of packets without a tag, 0 for none), `tagged` (the VLANs that pass tagged), `hairpin` and `trunkAll` (let every tagged VLAN pass).

Faces are numbered down, up, north, south, west, east.
