# VXLAN hub
![VXLAN hub](block:oc2r:vxlan_hub)

Bridges an in-game Ethernet network to an external VXLAN tunnel over UDP. Connect it to network devices rather than the device bus. External connectivity requires the server administrator to enable and configure the common config vxlan section. Local forwarding acts as a hub. It is separate from paired tunnel cards and GPS radio networks.

Use the hub with an empty hand to see its VNI and the command for the other end. The tunnel accepts datagrams from the configured remote host on any port, because the Linux VXLAN driver picks the source port itself, and the VNI is random per hub.

[Wiki](https://wiki.loki-code.dev/en/reference/networking).
