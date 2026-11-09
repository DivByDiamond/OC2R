# Wireless access point and environment sensor

## Wireless access point

`oc2r:wireless_access_point` uses the supplied flat modem model. Place a normal network connector
against its underside, then join it to another connector with the blue Ethernet cable. Ethernet
is available only on the bottom face; the device bus may access the block from an adjacent cable interface.
The indicator animates while it has an authenticated tower attachment.

Read `getRadioEndpointId()` through the device bus. Issue an approved invitation for that endpoint
from the network administrator's tower RPC, then call `joinRadioNetwork(invite, secret)` on the access point.
`getRadioAddress()` and `getRadioStatus()` expose its address and connection state, without the credential.
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
is against the wall; connect a device-bus cable/interface to any exposed adjacent side. No network card is required.
It appears as `environment_sensor` on `/dev/hvc0`.

- `getEnvironment()` returns a read-only snapshot of the local biome ID, biome temperature, sky light,
  block light, local rain, global thunder, sky visibility, day time and sample game tick.
- `getSampleIntervalTicks()` returns 20. Repeated calls inside that interval reuse the cached sample.
- `getUnits()` identifies light values as 0–15, time in game ticks, and temperature as Minecraft's biome
  temperature scalar. This scalar is not degrees Celsius and does not include height-adjusted precipitation rules.
- The device-bus event `environment_changed` reports a changed sampled biome, light, weather, sky visibility
  or temperature. Its payload contains `sampledAt`; call `getEnvironment()` for the new snapshot.
  Ordinary passage of time does not generate an event every second.

Sampling reads only the sensor's own loaded block. Sampling and events stop when it unloads, and event subscriptions
are cleared. The indicator lights after the first server sample. No neighboring chunks are loaded for measurements.
