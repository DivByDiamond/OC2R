/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity.disk;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;

import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.DiskDriveBlock;
import li.cil.oc2.common.bus.device.vm.block.DiskDriveContainer;
import li.cil.oc2.common.bus.device.vm.block.DiskDriveDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.TypedItemStackHandler;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.DiskDriveFloppyMessage;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.LocationSupplierUtils;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.util.ThrottledSoundEmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;

import static li.cil.oc2.common.item.AbstractBlockDeviceItem.DATA_TAG_NAME;

@EventBusSubscriber(modid = API.MOD_ID)
public final class DiskDriveBlockEntity extends ModBlockEntity implements DiskDriveContainer {
    private final DiskDriveItemStackHandler itemHandler = new DiskDriveItemStackHandler();
    private final DiskDriveDevice<DiskDriveBlockEntity> device = new DiskDriveDevice<>(this);
    private final ThrottledSoundEmitter accessSoundEmitter;
    private final ThrottledSoundEmitter insertSoundEmitter;
    private final ThrottledSoundEmitter ejectSoundEmitter;

    ///////////////////////////////////////////////////////////////////

    public DiskDriveBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.DISK_DRIVE.get(), pos, state);

        this.accessSoundEmitter = new ThrottledSoundEmitter(LocationSupplierUtils.of(this),
            SoundEvents.FLOPPY_ACCESS.get()).withMinInterval(Duration.ofSeconds(1));
        this.insertSoundEmitter = new ThrottledSoundEmitter(LocationSupplierUtils.of(this),
            SoundEvents.FLOPPY_INSERT.get()).withMinInterval(Duration.ofMillis(100));
        this.ejectSoundEmitter = new ThrottledSoundEmitter(LocationSupplierUtils.of(this),
            SoundEvents.FLOPPY_EJECT.get()).withMinInterval(Duration.ofMillis(100));
    }

    ///////////////////////////////////////////////////////////////////

    public boolean canInsert(final ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.DEVICES_FLOPPY);
    }

    public ItemStack insert(final ItemStack stack, @Nullable final Player player) {
        if (!canInsert(stack)) {
            return stack;
        }

        eject(player);

        insertSoundEmitter.play();
        return itemHandler.insertItem(0, stack, false);
    }

    public boolean canEject() {
        return !itemHandler.extractItem(0, 1, true).isEmpty();
    }

    public void eject(@Nullable final Player player) {
        if (level == null) {
            return;
        }

        final ItemStack stack = itemHandler.extractItem(0, 1, false);
        if (!stack.isEmpty()) {
            final Direction facing = getBlockState().getValue(DiskDriveBlock.FACING);
            ejectSoundEmitter.play();
            ItemStackUtils.spawnAsEntity(level, getBlockPos().relative(facing), stack, facing).ifPresent(entity -> {
                if (player != null) {
                    entity.setNoPickUpDelay();
                    entity.setThrower(player);
                }
            });
        }
    }

    public ItemStack getFloppy() {
        return itemHandler.getStackInSlot(0);
    }

    @OnlyIn(Dist.CLIENT)
    public void setFloppyClient(final ItemStack stack) {
        itemHandler.setStackInSlot(0, stack);
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
            Capabilities.ItemHandler.BLOCK,
            (level, pos, state, be, side) -> {
                if (be instanceof final DiskDriveBlockEntity self) {
                    return self.itemHandler;
                }
                return null;
            },
            Blocks.DISK_DRIVE.get()
        );
        event.registerBlock(
            Capabilities.Device.BLOCK,
            (level, pos, state, be, side) -> {
                if (be instanceof final DiskDriveBlockEntity self) {
                    if (side == self.getBlockState().getValue(DiskDriveBlock.FACING).getOpposite()) {
                        return self.device;
                    }
                }
                return null;
            },
            Blocks.DISK_DRIVE.get()
        );
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.put(Constants.ITEMS_TAG_NAME, itemHandler.serializeNBT(registries));
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound(Constants.ITEMS_TAG_NAME));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put(Constants.ITEMS_TAG_NAME, itemHandler.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        itemHandler.deserializeNBT(registries, tag.getCompound(Constants.ITEMS_TAG_NAME));
    }

    @Override
    public ItemStack getDiskItemStack() {
        return itemHandler.getStackInSlotRaw(0);
    }

    @Override
    public void handleDataAccess() {
        accessSoundEmitter.play();
    }

    ///////////////////////////////////////////////////////////////////

    private final class DiskDriveItemStackHandler extends TypedItemStackHandler {
        public DiskDriveItemStackHandler() {
            super(1, ItemTags.DEVICES_FLOPPY);
        }

        public ItemStack getStackInSlotRaw(final int slot) {
            return super.getStackInSlot(slot);
        }

        @Override
        @Nonnull
        public ItemStack getStackInSlot(final int slot) {
            final ItemStack stack = getStackInSlotRaw(slot);
            exportDeviceDataToItemStack(stack);
            return stack;
        }

        @Override
        @Nonnull
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            if (slot == 0 && !simulate && amount > 0) {
                exportDeviceDataToItemStack(getStackInSlotRaw(0));
            }

            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return 1;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            exportDeviceDataToItemStack(getStackInSlotRaw(0));
            return super.serializeNBT(provider);
        }

        @Override
        protected void onContentsChanged(final int slot) {
            super.onContentsChanged(slot);

            if (level == null || level.isClientSide()) {
                return;
            }

            final ItemStack stack = getStackInSlotRaw(slot);
            if (stack.isEmpty()) {
                device.removeBlockDevice();
            } else {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, (nbt) -> {
                    final CompoundTag tag = ItemStackUtils.getOrCreateModDataTag(nbt).getCompound(DATA_TAG_NAME);
                    device.updateBlockDevice(tag);
                });
            }

            Network.sendToClientsTrackingBlockEntity(new DiskDriveFloppyMessage(DiskDriveBlockEntity.this), DiskDriveBlockEntity.this);

            setChanged();
        }

        private void exportDeviceDataToItemStack(final ItemStack stack) {
            if (stack.isEmpty()) {
                return;
            }

            if (level == null || level.isClientSide()) {
                return;
            }

            final CompoundTag tag = new CompoundTag();
            device.exportToItemStack(tag);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, (nbt) -> {
                ItemStackUtils.getOrCreateModDataTag(nbt).put(DATA_TAG_NAME, tag);
            });
        }
    }
}
