package li.cil.oc2.common.blockentity.keyboard;

import java.util.UUID;
import javax.annotation.Nullable;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.keyboard.link.KeyboardInputSession;
import li.cil.oc2.common.blockentity.keyboard.link.KeyboardTargets;
import li.cil.oc2.common.bus.device.vm.block.KeyboardDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.CapabilityRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class KeyboardBlockEntity extends ModBlockEntity implements TickableBlockEntity {
    private final KeyboardDevice<BlockEntity> keyboardDevice = new KeyboardDevice<>(this);
    private UUID inputId = UUID.randomUUID();
    @Nullable private BlockPos targetPos;
    @Nullable private UUID targetId;
    public final KeyboardInputSession capture = new KeyboardInputSession(this);

    public KeyboardBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.KEYBOARD.get(), pos, state);
        setNeedsLevelUnloadEvent();
    }

    public void handleInput(final int keycode, final boolean isDown) {
        keyboardDevice.sendKeyEvent(keycode, isDown);
    }

    /** Stable identity prevents stale packets controlling a replacement keyboard. */
    public UUID getInputId() { return inputId; }

    /** Linked endpoint position, or this keyboard for legacy DOWN bus mode. */
    public BlockPos getTargetPos() { return targetPos == null ? getBlockPos() : targetPos; }

    /** Associates this keyboard with an identity-checked native input endpoint. */
    public void link(final BlockEntity target) {
        capture.release();
        if (target instanceof li.cil.oc2.common.blockentity.computer.ComputerBlockEntity computer) {
            computer.busElement.enableNativeInput();
        } else if (target instanceof li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity projector) {
            projector.enableNativeInput();
        }
        targetPos = target.getBlockPos().immutable();
        targetId = KeyboardTargets.identity(target);
        setChanged();
        li.cil.oc2.platform.Platform.capabilities().invalidateBlock(level, getBlockPos());
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    /** Resolves only an authorized endpoint belonging to the linked guest. */
    @Nullable
    public KeyboardDevice<?> inputTarget(final ServerPlayer player, final BlockPos focus) {
        if (targetPos == null) return keyboardDevice;
        final BlockEntity anchor = KeyboardTargets.resolve(level, targetPos);
        if (anchor == null || !targetId.equals(KeyboardTargets.identity(anchor))) return null;
        final KeyboardDevice<?> anchorDevice = KeyboardTargets.device(anchor);
        if (focus.equals(targetPos)) return anchorDevice;
        final BlockEntity endpoint = KeyboardTargets.resolve(level, focus);
        final KeyboardDevice<?> target = KeyboardTargets.device(endpoint);
        return target != null && anchorDevice.sharesMachineWith(target)
                && KeyboardTargets.isLookedAt(player, endpoint) ? target : anchorDevice;
    }

    @Override
    public void serverTick() {
        capture.tick();
        li.cil.oc2.common.blockentity.keyboard.link.KeyboardPairing.tick(this);
        if (targetPos != null && !level.hasChunkAt(targetPos)) capture.release();
        if (targetPos != null && level.hasChunkAt(targetPos)) {
            final BlockEntity endpoint = KeyboardTargets.resolve(level, targetPos);
            if (endpoint == null || !targetId.equals(KeyboardTargets.identity(endpoint))) {
                capture.release();
                targetPos = null;
                targetId = null;
                setChanged();
                li.cil.oc2.platform.Platform.capabilities().invalidateBlock(level, getBlockPos());
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    protected void unloadServer(final boolean removed) { capture.release(); }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("input_id", inputId.toString());
        if (targetPos != null) {
            tag.putLong("input_target", targetPos.asLong());
            tag.putString("input_target_id", targetId.toString());
        }
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Strings keep legacy and current NBT codecs identical.
        //? if >=26.1 {
        /*final String ownId = tag.getStringOr("input_id", "");
        final String linkedId = tag.getStringOr("input_target_id", "");
        final long position = tag.getLongOr("input_target", 0);
        *///?} else {
        final String ownId = tag.getString("input_id");
        final String linkedId = tag.getString("input_target_id");
        final long position = tag.getLong("input_target");
        //?}
        try {
            if (!ownId.isEmpty()) inputId = UUID.fromString(ownId);
            targetId = linkedId.isEmpty() ? null : UUID.fromString(linkedId);
            targetPos = targetId == null ? null : BlockPos.of(position);
        } catch (final IllegalArgumentException ignored) {
            targetId = null;
            targetPos = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(
                Capabilities.Device.BLOCK,
                (level, pos, state, be, side) -> {
                    if (side == Direction.DOWN && be instanceof final KeyboardBlockEntity self
                            && self.targetPos == null) {
                        return self.keyboardDevice;
                    }
                    return null;
                },
                Blocks.KEYBOARD.get());
    }
}