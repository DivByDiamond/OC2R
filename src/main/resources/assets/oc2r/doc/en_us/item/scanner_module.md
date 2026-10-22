# Scanner Module
![I see you](item:oc2r:scanner_module)

The scanner module provides to [robots](robot.md) the ability to inspect the block, fluid and entities on one of its sides, to measure the hardness of the blocks around it, and to check whether it can see the sky.

## API
Device name: *scanner*

This is a high level API device. It can be controlled using Lua in the default Linux distribution. For example:  
*local d = require("devices")*  
*local s = d:find("scanner")*  
*s:scan()*

### Sides
The side parameter of *inspect()* represents a face of the robot. Valid values are: *front*, *up* and *down*.

### Scanning
*scan()* measures the hardness of every block in a cube of seven by seven by seven blocks with the robot at its center. Positions use the axes of the robot's *getPosition()*: *x* grows towards east, *y* upward and *z* towards south.

The RPC result is a Lua table of 343 unsigned numbers (0 to 255), one per block, not a string. Blocks are ordered by *y*, then *z*, then *x*, each from *-3* to *3*, so the result is a stack of horizontal layers from bottom to top, and *x* changes fastest. The block at offset *x*, *y*, *z* from the robot is at index `(y + 3) * 7 * 7 + (z + 3) * 7 + (x + 3)`, counting from zero. Since Lua counts from one, this is equivalent to *hardness[index + 1]*.

Each byte encodes air as *0*, any fluid as *254* and unbreakable or unscannable blocks as *255*. Regular blocks are encoded as hardness mapped into the *1* to *253* range.

After a scan, the scanner needs five seconds to recharge. Scanning while it recharges fails.

### Fluids
Fluids are always reported by their source fluid, such as *minecraft:water*.

### Entities
Entities are reported by identifier, such as *minecraft:sheep*, closest first. At most twenty are listed.

### Methods
*inspect(side):table* identifies the block, fluid and entities in the space on the specified side of the robot.
- *side* is the side to look at: *front*, *up* or *down*.
- Returns a table with the *block* and *fluid*, if present, and a list of *entities*.

*scan():table* measures the hardness of the blocks around the robot and lists nearby entities.
- Returns a table with the block *hardness* as a numeric array, see above, and the list of *entities* in the scanned cube.

*canSeeSky():boolean* checks whether the robot's space gets full sky light.
- Returns whether the sky is visible.

*getScanLayer(layer:number):table* reads one layer of the last scan. Fails if there was no scan yet.
- *layer* is the layer, 0 to 6 from bottom to top.
- Returns a table of 49 numbers, the hardness of each block in the layer.
