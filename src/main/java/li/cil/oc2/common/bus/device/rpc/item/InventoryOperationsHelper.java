package li.cil.oc2.common.bus.device.rpc.item;

import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.common.capabilities.Capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

final class InventoryOperationsHelper {
    private final Entity entity;
    private final Robot robot;

    InventoryOperationsHelper(final Entity entity, final Robot robot) {
        this.entity = entity;
        this.robot = robot;
    }

    Stream<IItemHandler> getItemStackHandlersInDirection(final Direction direction) {
        return getItemStackHandlersAt(
                entity.blockPosition().relative(direction), direction.getOpposite());
    }

    int takeFromWorld(final int count) {
        final int selectedSlot = robot.getSelectedSlot();
        final ItemStackHandler inventory = robot.getInventory();

        int remaining = count;
        for (final ItemEntity itemEntity : getItemsInRange()) {
            final ItemStack original = itemEntity.getItem().copy();

            final ItemStack stackToInsert = original.copy();
            if (stackToInsert.getCount() > remaining) {
                stackToInsert.setCount(remaining);
            }

            final ItemStack overflow =
                    insertStartingAt(inventory, stackToInsert, selectedSlot, false);
            final int taken = stackToInsert.getCount() - overflow.getCount();

            remaining -= taken;
            original.shrink(taken);
            itemEntity.setItem(original);
        }

        return count - remaining;
    }

    int takeFromInventories(final int count, final List<IItemHandler> handlers) {
        final int selectedSlot = robot.getSelectedSlot();
        final ItemStackHandler inventory = robot.getInventory();

        int remaining = count;
        for (final IItemHandler handler : handlers) {
            for (int fromSlot = 0; fromSlot < handler.getSlots(); fromSlot++) {
                ItemStack extracted = handler.extractItem(fromSlot, remaining, true);
                ItemStack overflow = insertStartingAt(inventory, extracted, selectedSlot, true);

                final int delta = extracted.getCount() - overflow.getCount();
                if (delta == 0) {
                    continue;
                }

                remaining -= delta;

                extracted = handler.extractItem(fromSlot, delta, false);
                overflow = insertStartingAt(inventory, extracted, selectedSlot, false);

                remaining += overflow.getCount();
                overflow = handler.insertItem(fromSlot, overflow, false);

                if (!overflow.isEmpty()) {
                    remaining -= overflow.getCount();
                    entity.spawnAtLocation(overflow);
                }
            }

            if (remaining <= 0) {
                break;
            }
        }

        return count - remaining;
    }

    int takeFromInventory(final int count, final IItemHandler handler, final int slot) {
        final ItemStackHandler inventory = robot.getInventory();
        final int selectedSlot = robot.getSelectedSlot();

        ItemStack extracted = handler.extractItem(slot, count, true);
        ItemStack overflow = insertStartingAt(inventory, extracted, selectedSlot, true);

        int taken = extracted.getCount() - overflow.getCount();

        extracted = handler.extractItem(slot, taken, false);
        overflow = insertStartingAt(inventory, extracted, selectedSlot, false);

        taken -= overflow.getCount();
        overflow = handler.insertItem(slot, overflow, false);

        if (!overflow.isEmpty()) {
            entity.spawnAtLocation(overflow);
        }

        return taken;
    }

    private ItemStack insertStartingAt(
            final IItemHandler handler,
            ItemStack stack,
            final int startSlot,
            final boolean simulate) {
        for (int i = 0; i < handler.getSlots(); i++) {
            final int slot = (startSlot + i) % handler.getSlots();
            stack = handler.insertItem(slot, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    private Stream<IItemHandler> getItemStackHandlersAt(
            final BlockPos blockPos, final Direction side) {
        return Stream.concat(
                getEntityItemHandlersAt(blockPos, side), getBlockItemHandlersAt(blockPos, side));
    }

    private Stream<IItemHandler> getEntityItemHandlersAt(
            final BlockPos blockPos, final Direction side) {
        var position = Vec3.atCenterOf(blockPos);
        final AABB bounds = AABB.unitCubeFromLowerCorner(position.subtract(0.5, 0.5, 0.5));
        return entity.level().getEntities(entity, bounds).stream()
                .map(e -> e.getCapability(Capabilities.ItemHandler.ENTITY))
                .filter(Objects::nonNull);
    }

    private Stream<IItemHandler> getBlockItemHandlersAt(
            final BlockPos blockPos, final Direction side) {
        var level = entity.level();

        final IItemHandler capability =
                level.getCapability(Capabilities.ItemHandler.BLOCK, blockPos, side);
        return Stream.ofNullable(capability);
    }

    private List<ItemEntity> getItemsInRange() {
        return entity.level()
                .getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(1));
    }
}
