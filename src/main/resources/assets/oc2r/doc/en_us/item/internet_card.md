# Internet Card
![Host-backed networking](item:oc2r:internet_card)

This native device provides an Ethernet connection through the real Minecraft server's Internet adapter. Internet access is disabled by default and requires administrator configuration. Host restrictions and session limits apply.

Unlike a cable LAN, this is a point-to-point link without a DHCP server. Configure the guest's interface with a static address and suitable route; the adapter answers ARP. *setup-network.lua* is not included. Actual allowed destinations depend on the server's *deniedHosts* configuration. An ordinary NIC and local cables do not grant Internet access on their own.

Shut down before installing or removing this card. The [Internet gateway](../block/internet_gateway.md) can expose a host-backed connection to a cable network instead.
