# Roadmap: robot

## 28. Robot fixes

Audit result: the robot is a fully implemented working feature (port of OC2), but with thread races, leaks and item bugs. All items stay open (unverified), the first one with a note.

- [ ] **Thread race in `RobotActionProcessor`**: `addAction()` is called from the VM thread (`@Callback(synchronize=false)` in `RobotDevice`), while `tick()` on the server thread reads an `ArrayDeque`
  without a lock (`queue.poll()`, the `action` field is not volatile). Possible queue corruption. Fix: wrap the queue in a lock (like `results`) or use `ConcurrentLinkedDeque`.
  Note: the section 37 audit says it seems to be locked already; **needs manual check**, then close or fix.
- [ ] **`RobotBlockCollider.collideWithWorld()` breaks blocks every tick** without checking whether the robot is moving: a stuck/intersecting robot keeps chewing terrain, including on bounce.
  Fix: break only when there is active movement in that direction.
- [ ] **`RobotEventHandler` leak**: `register()` on the first server tick, `unregister()` only on chunk/world unload. On `discard()`/pickup-as-item the listeners are not removed. Fix: unregister in `Robot.remove()`.
- [ ] **`BlockOperationsModuleDevice.place()` does not consume the item**: `itemStack.copy()` goes into `BlockPlaceContext`; if the block is placed without `consumesAction()` the inventory item is not extracted.
  Fix: consume based on the `place` result.
- [ ] **`exportToItemStack` loses VM/terminal state**: picking the robot up as an item stores only items + energy, the running program/memory is lost (asymmetric with the entity `save()`). Decide: document or serialize VM state into the item.
- [ ] The robot is an `Entity`, not a `LivingEntity`: no HP/damage/gravity (`setNoGravity(true)`: break the block under it and it hovers). Either deliberate design or add gravity + HP (task V2).
- [ ] Movement is only allowed while the VM runs (`addAction` checks `isRunning()`): manual control without an OS is impossible.
- [ ] The item render is static: `RobotWithoutLevelRenderer` does not call the in-hand animation.

### 27 audit rows (robot)

- [x] Robot: `RobotEntity`, `RobotMovementController`, inventory: how "alive" vs the V2 feature: see section 28 above.

### 41.2 robot rows

- [x] **Robot `detect(side)` API (issue #108)**: closed as won't-do (plain feature request, not a bug). There is no such method in `api/capabilities/Robot.java`
  (only getInventory/getSelectedSlot/setSelectedSlot) or `common/entity/Robot.java`. Can be reopened as a feature if wanted. See [upstream.md](upstream.md#412-targeted-fixes-from-the-release).
