# Networking

Connect [network connectors](block/network_connector.md) with [network cables](item/network_cable.md), and install a [network interface card](item/network_interface_card.md) in each computer. Connectors attach to the computer's non-front faces. The card's configuration enables or disables its individual sides.

Connectors relay frames between cable links. Use a [hub](block/network_hub.md) for branches. Avoid loops: forwarded frames consume a finite hop budget, but cycles can still create duplicates before that budget expires.

## Linux setup

Shut down before installing a network card, then boot Linux. Discover the interface in the shell:

```sh
ifconfig -a
```

For a two-computer LAN, run this on the first computer, replacing `eth0` if needed:

```sh
ifconfig eth0 192.168.50.1 netmask 255.255.255.0 up
```

On the second computer:

```sh
ifconfig eth0 192.168.50.2 netmask 255.255.255.0 up
```

Test from the first computer:

```sh
ping 192.168.50.2
```

These are temporary settings; persistent setup uses the guest's Linux network configuration.

The old *setup-network.lua* wizard is not included in the current bundled scripts. There is no automatic DHCP server in a cable or hub. A DHCP client such as *udhcpc -i eth0* requires an actual DHCP server on that network. Do not run multiple DHCP servers or overlap the dynamic pool with static addresses.

The guest includes *ssh* and *scp* clients. Remote login also needs a running SSH server on the destination. LuaSocket is available for custom guest networking. An [Internet card](item/internet_card.md) uses a separate host-backed point-to-point connection; it is not configured like a DHCP cable LAN.
