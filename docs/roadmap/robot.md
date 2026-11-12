# Roadmap: robot

## 28. Robot fixes

Audit result: the robot is a fully implemented working feature (port of OC2). The five bug items below were re-checked against the code on 2026-11-12; three were already fixed, two were fixed then.

- [x] **Thread race in `RobotActionProcessor`** (checked 2026-11-12): already fixed. Every access to the queue, the current action, the results and
  `lastActionId` goes through one `ReentrantLock` (`addAction()` from the VM thread, `tick()` on the server thread, serialization); no field is read outside it.
- [x] **`RobotBlockCollider.collideWithWorld()` chews terrain** (2026-11-12): the earlier guard only asked whether any action was queued, so a robot that
  turned, had queued work or overlapped a block behind it still broke blocks. It now clears only blocks that touch the half of its box facing a move in
  progress; after a bounce the old obstacle is behind it. Covered by `RobotBlockColliderTest`.
- [x] **`RobotEventHandler` leak** (checked 2026-11-12): already fixed. `Robot.remove()` unregisters the listeners and disposes the VM for every destroying
  removal (`discard()`, pickup as an item, kill). Not covered: a dimension change does not call `remove()`, the old entity's listeners stay until its chunk unloads.
- [x] **`BlockOperationsModuleDevice.place()` does not consume the item** (checked 2026-11-12): already fixed. The slot is probed with a simulated extract and
  one item is extracted after `place` reports `consumesAction()`.
- [x] **`exportToItemStack` loses VM/terminal state** (2026-11-12): memory, the private root disk, the terminal and device state are far too large for an item,
  so picking a robot up writes them to `<world>/data/oc2r_robots/<uuid>.dat` and the item carries the id (`robot_state_id` component). Placing the item
  restores the machine once and deletes the file; a copied item finds nothing and starts fresh, exactly as before. The movement queue is not carried over.
  Covered by `RobotTests.pickedUpRobotKeepsItsMachineStateOnce`. Files of robot items that are destroyed are not collected.
- [ ] The robot is an `Entity`, not a `LivingEntity`: no HP/damage/gravity (`setNoGravity(true)`: break the block under it and it hovers). Either deliberate design or add gravity + HP (task V2).
- [ ] Movement is only allowed while the VM runs (`addAction` checks `isRunning()`): manual control without an OS is impossible.
- [ ] The item render is static: `RobotWithoutLevelRenderer` does not call the in-hand animation.

### 27 audit rows (robot)

- [x] Robot: `RobotEntity`, `RobotMovementController`, inventory: how "alive" vs the V2 feature: see section 28 above.

### 41.2 robot rows

- [x] **Robot `detect(side)` API (issue #108)**: closed as won't-do (plain feature request, not a bug). There is no such method in `api/capabilities/Robot.java`
  (only getInventory/getSelectedSlot/setSelectedSlot) or `common/entity/Robot.java`. Can be reopened as a feature if wanted. See [upstream.md](upstream.md#412-targeted-fixes-from-the-release).

## 53. Robot as a full worker

Goal: the robot can do the usual mining, building and hauling jobs on its own, with a clear cost model, and be programmed in Lua or by an OS program with a driver.

How it should work:
- **Fuel/energy**: every move and action costs energy from the robot battery (the existing energy model); a refuel action takes fuel items or charger power. Running out stops movement, not the VM.
- **Tool slots / upgrades**: slots for a tool (pickaxe, axe, shovel, hoe; dig speed and what it can break follow the tool), a wireless card ([section 51](network-overhaul.md#51-wireless-networking-and-positioning)), a speaker, an inventory expander and the existing block/inventory/crafting/scanner modules. Upgrades are items with a small data-driven definition so addons can add more.
- **Actions**: move/turn/up/down, dig, place, detect, inspect, compare, drop/suck for inventories and chests, transfer between slots, craft, equip and unequip, attack. Each returns a result and a reason string; digging uses the real block-break rules (protection mods, drops, tool damage).
- **Native face** ([section 50](devices-storage.md#50-driver-layer-every-device-reachable-without-lua)): a command queue register with completion interrupts, so an OS driver exposes the robot as `/dev/robotN`.
- **Programs** ([section 56](devices-storage.md#56-software-shipped-with-the-image)): tunnel, quarry, go-to, build wall/line, refuel loop, with GPS support when [section 51](network-overhaul.md#51-wireless-networking-and-positioning) lands.

Pros: this is the most visible automation feature in the mod; programs written once work on any robot; clear energy cost stops infinite-mining exploits.

Tasks:
- [x] First close the open items of [section 28](#28-robot-fixes) (thread race in `RobotActionProcessor`, collider chewing terrain, event handler leak, `place()` item use, export losing VM state): closed 2026-11-12.
- [ ] Decide gravity and HP ([section 28](#28-robot-fixes) V2) since a robot that hovers when the floor is broken changes how programs behave.
- [ ] Tool slot and dig using the tool; protection/claim checks through a fake player.
- [ ] Energy and fuel model, charger integration, a `getFuel`/`getFuelLimit` pair on both faces.
- [ ] Upgrade item definition (datapack-driven) and API for addons.
- [ ] Missing actions: compare, attack, equip/unequip, transfer; tests per action.
- [ ] Guest programs listed in [section 56](devices-storage.md#56-software-shipped-with-the-image).
