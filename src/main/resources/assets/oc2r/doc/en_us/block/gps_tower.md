# GPS tower
![GPS tower](block:oc2r:gps_tower)

The tower reports its base position and dimension. Relative mode adds explicitly paired radio networks whose stations report offsets from a selected reference.

Place it on a solid foundation with five blocks of vertical clearance. Connect its base to the computer's device bus. RPC device name: `gps_tower`.

## Default mode

```lua
local gps = assert(require("devices"):find("gps_tower"))
local p = gps:getPosition()
print(p[1], p[2], p[3])
print(gps:getDimension())
```

By default the result is the base's world X, Y, Z. A damaged tower raises an error. `isAssembled()` checks the structure.

## Relative mode

Enable `gameplay.relative_coordinates` in the common OC2R config. `gameplay.gps_tower_range` controls the maximum horizontal hop distance, defaulting to 45 blocks. Coverage is circular in X/Z with no height restriction; dimensions remain separate.

Call `setAsReference()` on the first tower and read `getNetworkId()`. At additional towers, pass that ID to `joinNetwork(id)`. A network may have only one reference; its position is `(0, 0, 0)`.

`hasSignal()` checks for a route to the reference. Without a route, `getPosition()` raises an error rather than returning world XYZ. Other networks cannot relay the signal. Unloaded stations continue using the persisted topology. Breaking a station removes it.

To move the origin, first call `clearReference()` on the old reference, then select another member. Update saved waypoints after changing the origin.

The tower does not locate players or add a GPS receiver to a [robot](../item/robot.md). Robot positions remain local.

[Full setup and method reference](https://wiki.loki-code.dev/en/reference/gps-networks).
