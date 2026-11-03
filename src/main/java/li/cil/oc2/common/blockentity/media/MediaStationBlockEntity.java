package li.cil.oc2.common.blockentity.media;

import javax.annotation.Nullable;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.media.MediaStationBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.disk.FloppyVisualSource;
import li.cil.oc2.common.blockentity.misc.flash.FlashMemoryFlasherBlockEntity;
import li.cil.oc2.common.bus.device.DeviceGroup;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.nbt.NBTUtils;
import li.cil.oc2.platform.CapabilityRegistrar;
import li.cil.oc2.platform.CombinedItemHandler;
import li.cil.oc2.platform.ItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Simultaneously exposes a floppy drive and a flash/HDD programmer through one bus port. */
public final class MediaStationBlockEntity extends FlashMemoryFlasherBlockEntity implements FloppyVisualSource {
    private static final String FLOPPY_TAG = "floppy";
    private final MediaStationFloppyStorage floppy = new MediaStationFloppyStorage(this);
    private final ItemHandler inventory = new CombinedItemHandler(floppy, flashInventory());
    private final DeviceGroup devices = new DeviceGroup(this);
    private boolean contentsDropped;

    public MediaStationBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.MEDIA_STATION.get(), pos, state);
        devices.addDevice("floppy", floppy.device);
        devices.addDevice("flash", device);
    }

    public static void registerMediaCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(Capabilities.ItemHandler.BLOCK,
                (level, pos, state, be, side) -> be instanceof MediaStationBlockEntity self ? self.inventory : null,
                Blocks.MEDIA_STATION.get());
        registrar.registerBlock(Capabilities.Device.BLOCK,
                (level, pos, state, be, side) -> be instanceof MediaStationBlockEntity self
                        && side == state.getValue(MediaStationBlock.FACING).getOpposite() ? self.devices : null,
                Blocks.MEDIA_STATION.get());
    }

    public boolean canInsertFloppy(final ItemStack stack) {
        return stack.is(ItemTags.DEVICES_FLOPPY) && floppy.getDiskItemStack().isEmpty();
    }

    public ItemStack insertFloppy(final ItemStack stack) { return floppy.insertItem(0, stack, false); }

    @Override
    public ItemStack getRenderedFloppy() { return floppy.getStackInSlot(0); }

    public void ejectFloppy(@Nullable final Player player) {
        if (level == null || level.isClientSide()) return;
        final ItemStack stack = floppy.extractItem(0, 1, false);
        if (!stack.isEmpty()) {
            final var facing = getBlockState().getValue(MediaStationBlock.FACING);
            li.cil.oc2.common.util.item.ItemStackUtils.spawnAsEntity(
                    level, worldPosition.relative(facing), stack, facing)
                    .ifPresent(entity -> { if (player != null) entity.setNoPickUpDelay(); });
        }
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.put(FLOPPY_TAG, floppy.serializeNBT(registries));
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        floppy.deserializeNBT(registries, NBTUtils.getChildTag(tag, FLOPPY_TAG));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(FLOPPY_TAG, floppy.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        floppy.deserializeNBT(registries, NBTUtils.getChildTag(tag, FLOPPY_TAG));
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        if (isRemove && !contentsDropped) {
            contentsDropped = true;
            // Snapshot both backing handles before asynchronous firmware jobs observe removal.
            final ItemStack[] contents = {getRenderedFloppy().copy(), getFloppy().copy()};
            floppy.setStackInSlot(0, ItemStack.EMPTY);
            flashInventory().setStackInSlot(0, ItemStack.EMPTY);
            for (final ItemStack stack : contents) {
                if (!stack.isEmpty()) Containers.dropItemStack(level,
                        worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
        super.unloadServer(isRemove);
    }
}
