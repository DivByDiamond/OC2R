# GPS networks and relative coordinates

This development change adds explicitly paired radio networks to the existing GPS tower. It does not add player tracking, a tablet receiver or a robot GPS module.

## Configuration

Edit `config/oc2r-common.toml` on the server or in the single-player instance. Keep these keys inside the existing `[gameplay]` section:

```toml
[gameplay]
relative_coordinates = true
gps_tower_range = 45
```

`relative_coordinates` defaults to `false`, preserving the tower's existing world XYZ API. When enabled, `getPosition()` returns an offset from that tower's network reference. Robot positions remain relative to their placement/calibration origin in both modes.

`gps_tower_range` is the maximum horizontal distance between neighboring stations, measured between their base blocks in X/Z; height is ignored. Its default is 45 blocks; accepted values are 1–1024. A chain can span a greater distance. Obstacles do not attenuate the signal.

Restart after changing the pack's configuration so every program starts with the intended coordinate mode. Scripts should not retain a coordinate-frame assumption across a mode change.

## Create a network

Connect one assembled tower to the computer's device bus. With the relative mode enabled, run in guest Lua:

```lua
local gps = assert(require("devices"):find("gps_tower"))
gps:setAsReference()
print(gps:getNetworkId())
```

Keep the printed network ID. At each additional tower, connect a computer and explicitly join that network:

```lua
local gps = assert(require("devices"):find("gps_tower"))
gps:joinNetwork("paste-the-network-id-here")
print(gps:hasSignal())
if gps:hasSignal() then
    local p = gps:getPosition()
    print(p[1], p[2], p[3])
end
```

For setup, connect one tower at a time, or choose a specific device with `devices:get(deviceId)` when several towers are on the same bus. `find()` does not choose a station by physical proximity.

Nearby towers from different networks do not connect. A network ID alone does not bypass radio range. The reference is `(0, 0, 0)`; other towers report east/X, up/Y and south/Z offsets. `getDimension()` identifies the dimension. Networks do not cross dimensions.

## Reference selection

Only an explicit `setAsReference()` selects an origin. If that network already has one, the call fails. To move the origin deliberately, call `clearReference()` on the old reference, then `setAsReference()` on a member station. This changes the coordinate frame; update stored waypoints.

Breaking the reference makes its network unavailable until a player explicitly selects a replacement. There is no lowest-ID election. Creating a new tower creates fresh station and network IDs. A former reference must clear its role before joining another network.

## Unloaded chunks

The server persists station IDs, membership, reference roles and internal positions separately for each dimension. Unloaded stations continue to relay using this saved topology. Navigation does not force their chunks to load.

Breaking any tower part removes that station. If it was the only route to the reference, `hasSignal()` becomes `false`, and `getPosition()` raises a coordinate-free error. Loaded stations are checked against the actual world; stale entries are removed. External editing of an unloaded chunk is detected when that chunk next loads.

The configuration controls the guest device API. It does not remove Minecraft's own coordinates from chunk files, entity saves or server internals. Server operators with access to save files can still inspect world positions. No claim of fulfilling the broader "no absolute NBT anywhere" brief is made.

## RPC methods

| Method | Result |
|---|---|
| `isAssembled()` | Whether the five-part structure and foundation are intact |
| `getPosition()` | Three-number array: world XYZ in default mode, network offset in relative mode |
| `getDimension()` | Dimension identifier |
| `getId()` | Stable station UUID |
| `getNetworkId()` | Network UUID to share with stations that should join |
| `getRange()` | Configured maximum hop distance |
| `hasSignal()` | Reachable reference in relative mode; intact structure in default mode |
| `isReference()` | Whether this intact tower is the selected reference |
| `setAsReference()` | Select this network's reference; relative mode required |
| `clearReference()` | Clear this tower's reference role; relative mode required |
| `joinNetwork(networkId)` | Join a network that has a reference; relative mode required |

Common failures: `GPS reference is not configured`, `GPS signal is unavailable`, `GPS network already has a reference`, `GPS tower is incomplete`. They contain no station positions. No failure path falls back to world XYZ when relative mode is enabled.
