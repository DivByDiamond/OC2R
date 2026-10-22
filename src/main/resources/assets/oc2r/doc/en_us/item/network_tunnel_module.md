# Network Tunnel Module
![Tunnel vision?](item:oc2r:network_tunnel_module)

The network tunnel module allows [robots](robot.md) to send messages to and receive messages from another tunnel device (tunnel modules and [tunnel cards](network_tunnel_card.md)) linked to the module.

To link two tunnel devices, open their configuration interface (use while holding), and insert the other tunnel device to link to. This allows linking any two network tunnel devices.

Linux sees a regular Ethernet interface. See [network setup](../networking.md) for interface discovery, static addresses and DHCP requirements.

Robots *have to be shut down* before installing or removing this component. Installing it while the robot is running will have no effect, removing it may lead to system errors.
