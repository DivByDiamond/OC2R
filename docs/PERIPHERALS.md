# Wireless access point and environment sensor

## Wireless access point

`oc2r:wireless_access_point` uses the supplied flat modem model. Connect an ordinary device-bus cable/interface
below it to expose radio messages, telemetry and control RPC to a running computer. An adjacent side interface
also works. Standard Ethernet uses a network connector against the underside and the blue network cable;
its network-interface port is available only on the bottom face.
The off texture is shown while disconnected. An authenticated tower attachment enables the three original on frames
in their supplied order, at six ticks per frame, without interpolation.

Read `getRadioEndpointId()` through the device bus. Issue an approved invitation for that endpoint
from the network administrator's tower RPC, then call `joinRadioNetwork(invite, secret)` on the access point.
`getRadioAddress()` and `getRadioStatus()` expose its own address and connection state, without the credential.
A running computer connected to the access point's device bus can register services, send and receive ORA messages,
ping, discover devices, publish/subscribe telemetry and send/answer commands through the tower network. Permissions
belong to the access point's enrolled identity, not the tower. The `gps_tower` radio discovery alias keeps `ochat`
compatible; it does not add positioning hardware. Guest services disconnect and pending work is cancelled when the
computer stops or its bus detaches, while wireless client coverage and the Ethernet bridge remain available.
`getRadioStatus().guestOnline` distinguishes the guest service from infrastructure connectivity.
Named services survive save/load but an offline guest cannot receive commands successfully.
Enrollment, revocation, persistence, frame limits and duplicate suppression use the existing radio implementation.
An unenrolled, revoked or duplicate loaded identity cannot bridge traffic or provide access-point coverage.

The first version is a short-range access point, like the robot dock: clients inside the configured
`robot_dock_range` (10 blocks by default) can reach a tower of the access point's enrolled network.
A loaded tower must be within tower range of the point (45 blocks by default). All ranges are circular
in X/Z, with unrestricted height. It does not relay between towers or extend range through chains of access points.
Unloading or breaking it removes its coverage without force-loading chunks. Ordinary Ethernet and guest TCP/IP
continue to work through the authenticated bridge.

## Environment sensor

`oc2r:environment_sensor` uses the supplied wall-panel geometry and rotates horizontally. Its rear
is against the support: mount it directly on the outward face of a device-bus cable and add an interface
on that cable face. Only the sensor's rear accepts RPC; front, side, top, bottom and unsided queries do not.
A solid wall can also support a disconnected decorative sensor. Existing paid side interfaces remain
physical cable parts after upgrading, but no longer expose the sensor; move them to its rear. No network card is required.
It appears as `environment_sensor` on `/dev/hvc0`.

- `getEnvironment()` returns a read-only snapshot of the local biome ID, biome temperature, sky light,
  block light, local rain, global thunder, sky visibility, day time and sample game tick.
- `getSampleIntervalTicks()` returns 20. Repeated calls inside that interval reuse the cached sample.
- `getUnits()` identifies light values as 0–15, time in game ticks, and temperature as Minecraft's biome
  temperature scalar. This scalar is not degrees Celsius and does not include height-adjusted precipitation rules.
- The device-bus event `environment_changed` reports a changed sampled biome, light, weather, sky visibility
  or temperature. Its payload contains `sampledAt`; call `getEnvironment()` for the new snapshot.
  Ordinary passage of time does not generate an event every second.

Sampling reads only the sensor's own loaded block. Automatic sampling and the on indicator require a mounted device connection to a running VM.
A nearby stopped computer or an unattached bus does not light the sensor. Its two supplied textures represent
disconnected/off and mounted/on; no extra frames are invented. Direct diagnostic reads remain bounded to the
same sampling interval. Sampling and events stop when it unloads, and event subscriptions are cleared when
their controller disconnects. No neighboring chunks are loaded for measurements.

## Acceptance

The updated Fabric 26.1.2 client run passed with both devices in a live scene. All 16 orientation/on-off
model states matched their expected baked sprites and bounds. The access point's original three-frame
pulse advanced between two captures six ticks apart, while the mounted sensor showed its on indicator.

Integrated verification at source revision `92319182` passed 134/136/136 NeoForge GameTests and
163/163 Fabric 1.21.1 GameTests, with zero failures. Core and mod unit tests, all six platform compile
targets and the feature static-analysis checks also passed; see the
[integrated acceptance matrix](roadmap/tablet-radio.md#14-integrated-acceptance) for counts.


The rear-socket follow-up at `aa5b3763` passed 137/139/139 NeoForge GameTests and 166/166 Fabric
1.21.1 GameTests. A fresh real Fabric 26.1.2 client scene confirmed the sensor mounted through its rear
interface, with the computer facing away from the cable so its device-bus side remained available.
See [tablet graphics acceptance](roadmap/tablet-radio.md#15-rear-sensor-socket-and-tablet-graphics).
