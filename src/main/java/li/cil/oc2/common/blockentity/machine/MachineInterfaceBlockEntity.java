package li.cil.oc2.common.blockentity.machine;

import java.util.Collection;
import java.util.List;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.block.machine.MachineInterfaceBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.machine.MachineInventory;
import li.cil.oc2.common.util.nbt.NBTUtils;
import li.cil.oc2.platform.CapabilityRegistrar;
import li.cil.oc2.platform.ItemHandler;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A computer-controlled buffer attached to the machine in front of its connector face. */
public final class MachineInterfaceBlockEntity extends ModBlockEntity
        implements NamedDevice, MenuProvider, MachineInventoryPort, MachineSettingsPort, MachineResourcePort {
    public final MachineInventory inventory = new MachineInventory(this::setChanged);
    public final li.cil.oc2.common.machine.MachineResources resources =
            new li.cil.oc2.common.machine.MachineResources(this::setChanged);
    private boolean dropped;

    public MachineInterfaceBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.MACHINE_INTERFACE.get(), pos, state);
    }

    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(Capabilities.ItemHandler.BLOCK,
                (level, pos, state, be, side) -> be instanceof MachineInterfaceBlockEntity self
                        ? self.inventory : null,
                li.cil.oc2.common.block.common.Blocks.MACHINE_INTERFACE.get());
    }

    @Override
    public MachineInterfaceBlockEntity machine() { return this; }

    @Override
    public Collection<String> getDeviceTypeNames() { return List.of("machine_interface"); }

    @Override
    public Component getDisplayName() { return Component.translatable("block.oc2r.machine_interface"); }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory playerInventory, final Player player) {
        return new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x2, id, playerInventory, this, 2);
    }

    @Callback
    public int getTargetSlotCount() { return targetItems().getSlots(); }

    @Callback
    public ItemStack getTargetItem(@Parameter("slot") final int slot) {
        return targetItems().getStackInSlot(slot).copy();
    }

    @Callback
    public ItemStack getBufferItem(@Parameter("slot") final int slot) {
        checkAttached();
        return inventory.getStackInSlot(slot).copy();
    }

    @Callback
    public int pushItems(@Parameter("bufferSlot") final int from, @Parameter("targetSlot") final int to,
                         @Parameter("amount") final int amount) {
        return inventory.transfer(inventory, from, targetItems(), to, amount);
    }

    @Callback
    public int pullItems(@Parameter("targetSlot") final int from, @Parameter("bufferSlot") final int to,
                         @Parameter("amount") final int amount) {
        return inventory.transfer(targetItems(), from, inventory, to, amount);
    }

    @Callback
    public int recoverItems() {
        checkAttached();
        return inventory.recover();
    }

    public Direction facing() { return getBlockState().getValue(MachineInterfaceBlock.FACING); }

    public BlockState targetState() {
        checkAttached();
        final BlockPos target = worldPosition.relative(facing());
        if (!level.hasChunkAt(target)) throw new IllegalStateException("Target chunk is not loaded");
        return level.getBlockState(target);
    }

    public BlockEntity targetEntity() {
        targetState();
        return level.getBlockEntity(worldPosition.relative(facing()));
    }

    private ItemHandler targetItems() {
        targetState();
        final ItemHandler handler = Platform.capabilities().getBlockCapability(Capabilities.ItemHandler.BLOCK,
                level, worldPosition.relative(facing()), facing().getOpposite());
        if (handler == null) throw new IllegalStateException("Target exposes no item handler");
        return handler;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals") // Reject a replaced BE, even at the same position.
    public void checkAttached() {
        if (level == null || level.isClientSide() || !isValid() || !level.hasChunkAt(worldPosition)
                || level.getBlockEntity(worldPosition) != this) {
            throw new IllegalStateException("Machine interface is no longer attached");
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        li.cil.oc2.common.machine.MachineResourcePersistence.save(resources, tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        li.cil.oc2.common.machine.MachineResourcePersistence.load(resources, tag, registries);
        inventory.deserializeNBT(registries, NBTUtils.getChildTag(tag, "inventory"));
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        if (isRemove && !dropped) {
            dropped = true;
            inventory.close(stack -> Containers.dropItemStack(level,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack));
        }
        super.unloadServer(isRemove);
    }
}
