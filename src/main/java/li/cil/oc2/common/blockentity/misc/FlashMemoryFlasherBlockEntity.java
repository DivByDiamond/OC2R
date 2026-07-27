package li.cil.oc2.common.blockentity.misc;

import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.FlashMemoryFlasherBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.FlashMemoryFlasherContainer;
import li.cil.oc2.common.bus.device.vm.block.FlashMemoryFlasherDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.LocationSupplierUtils;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.util.ThrottledSoundEmitter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.time.Duration;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = API.MOD_ID)
public final class FlashMemoryFlasherBlockEntity extends ModBlockEntity
        implements FlashMemoryFlasherContainer {
    private final FlashMemoryItemStackHandler itemHandler = new FlashMemoryItemStackHandler(this);
    final FlashMemoryFlasherDevice<FlashMemoryFlasherBlockEntity> device =
            new FlashMemoryFlasherDevice<>(this);
    private final ThrottledSoundEmitter insertSoundEmitter;
    private final ThrottledSoundEmitter ejectSoundEmitter;

    public FlashMemoryFlasherBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.FLASH_MEMORY_FLASHER.get(), pos, state);

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
        return !stack.isEmpty() && stack.is(ItemTags.DEVICES_FLASH_MEMORY);
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
            final Direction facing = getBlockState().getValue(FlashMemoryFlasherBlock.FACING);
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

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final FlashMemoryFlasherBlockEntity self) {
                        return self.itemHandler;
                    }
                    return null;
                },
                Blocks.FLASH_MEMORY_FLASHER.get());
        event.registerBlock(
                Capabilities.Device.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final FlashMemoryFlasherBlockEntity self) {
                        if (side
                                == self.getBlockState()
                                        .getValue(FlashMemoryFlasherBlock.FACING)
                                        .getOpposite()) {
                            return self.device;
                        }
                    }
                    return null;
                },
                Blocks.FLASH_MEMORY_FLASHER.get());
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
        itemHandler.deserializeNBT(registries, tag.getCompound(Constants.ITEMS_TAG_NAME));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put(Constants.ITEMS_TAG_NAME, itemHandler.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        itemHandler.deserializeNBT(registries, tag.getCompound(Constants.ITEMS_TAG_NAME));
    }

    @Override
    public ItemStack getDiskItemStack() {
        return itemHandler.getStackInSlotRaw(0);
    }

    @Override
    public void handleDataAccess() {}
}
