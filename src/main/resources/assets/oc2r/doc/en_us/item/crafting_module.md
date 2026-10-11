# Crafting Module
![Crafty](item:oc2r:crafting_module)

The crafting module provides to [robots](robot.md) the ability to craft items from a crafting grid.

## API
Device name: *crafting*

This is a high level API device. It can be controlled using Lua in the default Linux distribution. For example:  
*local d = require("devices")*  
*local c = d:find("crafting")*  
*c:craft()*

The top part of the robot's inventory, slots *0* to *8*, serves as the crafting grid. Crafted items go into the bottom row, starting at the selected slot.

### Methods
*craft():number* tries to craft once from the crafting grid. Fails if the grid matches no recipe or the result does not fit into the bottom row.
- Returns the number of items crafted, or *0* if crafting failed.
