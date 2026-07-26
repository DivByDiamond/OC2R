
package li.cil.oc2.common.bus.device.rpc.item;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.util.FakePlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

public final class BlockOperationsModuleDevice extends AbstractItemRPCDevice {
    private final Entity entity;
    private final Robot robot;
    private final BlockOperationCooldown cooldown = new BlockOperationCooldown();

    public BlockOperationsModuleDevice(final ItemStack identity, final Entity entity, final Robot robot) {
        super(identity, "block_operations");
        this.entity = entity;
        this.robot = robot;
    }

    @Override
    public CompoundTag serializeNBT(final HolderLookup.Provider provider) {
        return cooldown.serializeNBT(provider);
    }

    @Override
    public void deserializeNBT(final HolderLookup.Provider provider, final CompoundTag tag) {
        cooldown.deserializeNBT(provider, tag, entity.level());
    }

    @Callback
    public boolean excavate() {
        return excavate(null);
    }

    @Callback
    public boolean excavate(@Parameter("side") @Nullable final RobotOperationSide side) {
        if (cooldown.isOnCooldown(entity.level())) {
            return false;
        }
        cooldown.beginCooldown(entity.level());

        final Level level = entity.level();
        if (!(level instanceof final ServerLevel serverLevel)) {
            return false;
        }

        final int selectedSlot = robot.getSelectedSlot();
        final ItemStackHandler inventory = robot.getInventory();

        final List<ItemEntity> oldItems = InventoryHelper.getItemsInRange(entity);

        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        if (!BlockHarvestHelper.tryHarvestBlock(serverLevel, entity.blockPosition().relative(direction), entity, identity)) {
            return false;
        }

        final List<ItemEntity> droppedItems = InventoryHelper.getItemsInRange(entity);
        droppedItems.removeAll(oldItems);

        for (final ItemEntity itemEntity : droppedItems) {
            ItemStack stack = itemEntity.getItem();
            stack = InventoryHelper.insertStartingAt(inventory, stack, selectedSlot, false);
            itemEntity.setItem(stack);
        }

        return true;
    }

    @Callback
    public boolean place() {
        return place(null);
    }

    @Callback
    public boolean place(@Parameter("side") @Nullable final RobotOperationSide side) {
        if (cooldown.isOnCooldown(entity.level())) {
            return false;
        }
        cooldown.beginCooldown(entity.level());

        final Level level = entity.level();
        if (!(level instanceof final ServerLevel serverLevel)) {
            return false;
        }

        final int selectedSlot = robot.getSelectedSlot();
        final ItemStackHandler inventory = robot.getInventory();

        final ItemStack extracted = inventory.extractItem(selectedSlot, 1, true);
        if (extracted.isEmpty() || !(extracted.getItem() instanceof final BlockItem blockItem)) {
            return false;
        }

        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final BlockPos blockPos = entity.blockPosition().relative(direction);
        final Direction oppositeDirection = direction.getOpposite();
        final BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(blockPos).add(Vec3.atCenterOf(oppositeDirection.getNormal()).scale(0.5)),
            oppositeDirection,
            blockPos,
            false);

        final ItemStack itemStack = extracted.copy();
        final ServerPlayer player = FakePlayerUtils.getFakePlayer(serverLevel, entity);
        final BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, itemStack, hit);

        final InteractionResult result = blockItem.place(context);
        if (!result.consumesAction()) {
            return false;
        }

        if (itemStack.isEmpty()) {
            inventory.extractItem(selectedSlot, 1, false);
        }

        return true;
    }

    @Callback(synchronize = false)
    public int durability() {
        return identity.getMaxDamage() - identity.getDamageValue();
    }

    @Callback
    public boolean repair() {
        if (cooldown.isOnCooldown(entity.level())) {
            return false;
        }
        cooldown.beginCooldown(entity.level());

        if (identity.getDamageValue() == 0) {
            return false;
        }

        final int selectedSlot = robot.getSelectedSlot();
        final ItemStackHandler inventory = robot.getInventory();

        final ItemStack extracted = inventory.extractItem(selectedSlot, 1, true);

        final Tier tier = RepairHelper.getRepairItemTier(extracted);
        if (tier == null) {
            return false;
        }

        final int repairValue = tier.getUses();
        if (repairValue == 0) {
            return false;
        }

        if (inventory.extractItem(selectedSlot, 1, false).isEmpty()) {
            return false;
        }

        identity.setDamageValue(identity.getDamageValue() - repairValue);

        return true;
    }
}
