package li.cil.oc2.common.bus.device.rpc.item;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.api.util.RobotOperationSide;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InventoryOperationsModuleDevice extends AbstractItemRPCDevice {
    private final Entity entity;
    private final Robot robot;
    private final InventoryOperationsHelper helper;

    ///////////////////////////////////////////////////////////////////

    public InventoryOperationsModuleDevice(final ItemStack identity, final Entity entity, final Robot robot) {
        super(identity, "inventory_operations");
        this.entity = entity;
        this.robot = robot;
        this.helper = new InventoryOperationsHelper(entity, robot);
    }

    ///////////////////////////////////////////////////////////////////

    @Callback
    public void move(@Parameter("fromSlot") final int fromSlot,
                     @Parameter("intoSlot") final int intoSlot,
                     @Parameter("count") final int count) {
        if (count <= 0) {
            return;
        }

        final ItemStackHandler inventory = robot.getInventory();

        ItemStack extracted = inventory.extractItem(fromSlot, count, true);
        ItemStack remaining = inventory.insertItem(intoSlot, extracted, true);

        extracted = inventory.extractItem(fromSlot, extracted.getCount() - remaining.getCount(), false);
        remaining = inventory.insertItem(intoSlot, extracted, false);

        remaining = inventory.insertItem(fromSlot, remaining, false);

        if (!remaining.isEmpty()) {
            entity.spawnAtLocation(remaining);
        }
    }

    @Callback
    public int drop(@Parameter("count") final int count) {
        return drop(count, null);
    }

    @Callback
    public int drop(@Parameter("count") final int count,
                    @Parameter("side") @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final int selectedSlot = robot.getSelectedSlot();

        ItemStack stack = robot.getInventory().extractItem(selectedSlot, count, false);
        if (stack.isEmpty()) {
            return 0;
        }

        final int originalStackSize = stack.getCount();
        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final List<IItemHandler> itemHandlers = helper.getItemStackHandlersInDirection(direction).toList();
        for (final IItemHandler handler : itemHandlers) {
            stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);

            if (stack.isEmpty()) {
                break;
            }
        }

        int dropped = originalStackSize - stack.getCount();
        if (!stack.isEmpty() && !itemHandlers.isEmpty()) {
            stack = robot.getInventory().insertItem(selectedSlot, stack, false);
        }

        if (!stack.isEmpty()) {
            dropped += stack.getCount();
            entity.spawnAtLocation(stack);
        }

        return dropped;
    }

    @Callback
    public int dropInto(@Parameter("intoSlot") final int intoSlot,
                        @Parameter("count") final int count) {
        return dropInto(intoSlot, count, null);
    }

    @Callback
    public int dropInto(@Parameter("intoSlot") final int intoSlot,
                        @Parameter("count") final int count,
                        @Parameter("side") @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final int selectedSlot = robot.getSelectedSlot();

        ItemStack stack = robot.getInventory().extractItem(selectedSlot, count, false);
        if (stack.isEmpty()) {
            return 0;
        }

        final int originalStackSize = stack.getCount();
        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final Optional<IItemHandler> optional = helper.getItemStackHandlersInDirection(direction).findFirst();
        if (optional.isPresent()) {
            stack = optional.get().insertItem(intoSlot, stack, false);
        }

        int dropped = originalStackSize - stack.getCount();
        if (!stack.isEmpty()) {
            stack = robot.getInventory().insertItem(selectedSlot, stack, false);
        }

        if (!stack.isEmpty()) {
            dropped += stack.getCount();
            entity.spawnAtLocation(stack);
        }

        return dropped;
    }

    @Callback
    public int take(@Parameter("count") final int count) {
        return take(count, null);
    }

    @Callback
    public int take(@Parameter("count") final int count,
                    @Parameter("side") @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final List<IItemHandler> handlers = helper.getItemStackHandlersInDirection(direction).collect(Collectors.toList());
        if (handlers.isEmpty()) {
            return helper.takeFromWorld(count);
        } else {
            return helper.takeFromInventories(count, handlers);
        }
    }

    @Callback
    public int takeFrom(@Parameter("fromSlot") final int fromSlot,
                        @Parameter("count") final int count) {
        return takeFrom(fromSlot, count, null);
    }

    @Callback
    public int takeFrom(@Parameter("fromSlot") final int fromSlot,
                        @Parameter("count") final int count,
                        @Parameter("side") @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        return helper.getItemStackHandlersInDirection(direction).findFirst().map(handler ->
            helper.takeFromInventory(count, handler, fromSlot)).orElse(0);
    }
}
