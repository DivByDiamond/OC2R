package li.cil.oc2.common.blockentity.misc.flash;

import java.time.Duration;
import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.flash.FlashMemoryFlasherContainer;
import li.cil.oc2.common.bus.device.vm.block.flash.FlashMemoryFlasherDevice;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.item.ItemStackUtils;
import li.cil.oc2.common.util.item.LocationSupplierUtils;
import li.cil.oc2.common.util.sound.SoundEvents;
import li.cil.oc2.common.util.sound.ThrottledSoundEmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}

public abstract class FlashMemoryFlasherBlockEntity extends ModBlockEntity
        implements FlashMemoryFlasherContainer {
    private final FlashMemoryItemStackHandler itemHandler = new FlashMemoryItemStackHandler(this);
    protected final FlashMemoryFlasherDevice<FlashMemoryFlasherBlockEntity> device =
            new FlashMemoryFlasherDevice<>(this);
    private volatile boolean writing;
    private final ThrottledSoundEmitter insertSoundEmitter;
    private final ThrottledSoundEmitter ejectSoundEmitter;

    protected FlashMemoryFlasherBlockEntity(final net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                            final BlockPos pos, final BlockState state) {
        super(type, pos, state);

        this.insertSoundEmitter =
                new ThrottledSoundEmitter(
                                LocationSupplierUtils.of(this), SoundEvents.FLOPPY_INSERT.get())
                        .withMinInterval(Duration.ofMillis(100));
        this.ejectSoundEmitter =
                new ThrottledSoundEmitter(
                                LocationSupplierUtils.of(this), SoundEvents.FLOPPY_EJECT.get())
                        .withMinInterval(Duration.ofMillis(100));
    }

    public boolean canInsert(final ItemStack stack) {
        return !writing && !stack.isEmpty() && (stack.is(ItemTags.DEVICES_FLASH_MEMORY)
                || stack.getItem() instanceof li.cil.oc2.common.item.storage.HardDriveItem);
    }

    public ItemStack insert(final ItemStack stack, @Nullable final Player player) {
        if (!canInsert(stack)) {
            return stack;
        }

        eject(player);

        insertSoundEmitter.play();
        return itemHandler.insertItem(0, stack, false);
    }

    @Override
    public boolean isWriting() {
        return writing;
    }

    public synchronized boolean beginWriting() {
        if (writing || device.isMounted()) return false;
        writing = true;
        return true;
    }

    public synchronized void endWriting() {
        writing = false;
    }

    public void replaceWrittenItem(final ItemStack stack) {
        itemHandler.setStackInSlot(0, stack);
    }

    public boolean canEject() {
        return !writing && !itemHandler.extractItem(0, 1, true).isEmpty();
    }

    public void eject(@Nullable final Player player) {
        if (level == null || writing) {
            return;
        }

        final ItemStack stack = itemHandler.extractItem(0, 1, false);
        if (!stack.isEmpty()) {
            final Direction facing = getBlockState().getValue(
                    net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
            ejectSoundEmitter.play();
            ItemStackUtils.spawnAsEntity(level, getBlockPos().relative(facing), stack, facing)
                    .ifPresent(
                            entity -> {
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
    public void setFlashMemory(final ItemStack stack) {
        itemHandler.setStackInSlot(0, stack);
    }

    /** Exposes the existing flash slot to subclasses composing a multi-slot device. */
    protected final li.cil.oc2.platform.ItemHandler flashInventory() {
        return itemHandler;
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.put(Constants.ITEMS_TAG_NAME, itemHandler.serializeNBT(registries));
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        //? if >=26.1 {
        /*itemHandler.deserializeNBT(registries, tag.getCompoundOrEmpty(Constants.ITEMS_TAG_NAME));
        *///?} else {
        itemHandler.deserializeNBT(registries, tag.getCompound(Constants.ITEMS_TAG_NAME));
        //?}
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put(Constants.ITEMS_TAG_NAME, itemHandler.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        //? if >=26.1 {
        /*itemHandler.deserializeNBT(registries, tag.getCompoundOrEmpty(Constants.ITEMS_TAG_NAME));
        *///?} else {
        itemHandler.deserializeNBT(registries, tag.getCompound(Constants.ITEMS_TAG_NAME));
        //?}
    }

    @Override
    public ItemStack getDiskItemStack() {
        return itemHandler.getStackInSlotRaw(0);
    }

    @Override
    public void handleDataAccess() {}
}