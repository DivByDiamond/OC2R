package li.cil.oc2.common.blockentity.projector;

import java.util.UUID;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.projector.misc.FrameConsumer;
import li.cil.oc2.common.blockentity.projector.misc.ProjectorContraptionHelper;
import li.cil.oc2.common.blockentity.projector.misc.ProjectorFrameSender;
import li.cil.oc2.common.blockentity.projector.misc.ProjectorRenderBounds;
import li.cil.oc2.common.bus.device.vm.block.misc.ProjectorDevice;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.projector.ProjectorRequestFramebufferMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class ProjectorBlockEntity extends ModBlockEntity implements TickableBlockEntity {
    public static final int MAX_RENDER_DISTANCE = 16;
    public static final int MAX_GOOD_RENDER_DISTANCE = 12;
    public static final int MAX_WIDTH = MAX_GOOD_RENDER_DISTANCE + 1;
    public static final int MAX_HEIGHT =
            (MAX_GOOD_RENDER_DISTANCE * ProjectorDevice.HEIGHT / ProjectorDevice.WIDTH) + 1;

    private static final String ENERGY_TAG_NAME = "energy";
    private static final String DEVICE_ID_TAG_NAME = "device_id";

    public final ProjectorDevice projectorDevice =
            new ProjectorDevice(this, this::handleMountedChanged);
    public final ProjectorFrameSender frameSender = new ProjectorFrameSender(this);
    public final FixedEnergyStorage energy = new FixedEnergyStorage(Config.projectorEnergyStorage);
    private final ProjectorState projectorState = new ProjectorState();
    private final ProjectorRenderBounds renderBounds = new ProjectorRenderBounds();

    public UUID deviceId = UUID.randomUUID();

    public ProjectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.PROJECTOR.get(), pos, state);
        renderBounds.update(state, pos);
    }

    public boolean isProjecting() {
        return projectorState.isProjecting(level, getBlockPos(), getBlockState());
    }

    public boolean hasEnergy() {
        return projectorState.hasEnergy;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public ProjectorBlockEntity getPrimaryForContraptionRendering() {
        return ProjectorContraptionHelper.getPrimaryForContraptionRendering(this);
    }

    public void setFrameConsumer(@Nullable final FrameConsumer consumer) {
        frameSender.setFrameConsumer(consumer);
    }

    public void handleWatchedBy(final ServerPlayer player) {
        frameSender.handleWatchedBy(player);
    }

    public void onRendering() {
        frameSender.onRendering();
    }

    @Override
    public void clientTick() {
        ProjectorContraptionHelper.registerInClientRegistry(this);
    }

    @Override
    protected void loadClient() {
        ProjectorContraptionHelper.registerInClientRegistry(this);
    }

    @Override
    public void serverTick() {
        if (!projectorState.isMounted) return;
        final boolean isPowered;
        if (Config.projectorsUseEnergy()) {
            isPowered =
                    energy.extractEnergy(Config.projectorEnergyPerTick, true)
                            >= Config.projectorEnergyPerTick;
            if (isPowered) energy.extractEnergy(Config.projectorEnergyPerTick, false);
        } else {
            isPowered = true;
        }
        projectorState.update(
                level,
                getBlockPos(),
                getBlockState(),
                projectorState.isMounted,
                isPowered,
                isValid(),
                this);
        if (!projectorState.hasEnergy || !projectorDevice.hasChanges()) return;
        frameSender.sendFrame(projectorDevice);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("projecting", projectorState.isMounted);
        tag.putBoolean("has_energy", projectorState.hasEnergy);
        tag.putUUID(DEVICE_ID_TAG_NAME, deviceId);
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        projectorState.applyClient(tag.getBoolean("projecting"), tag.getBoolean("has_energy"));
        if (tag.hasUUID(DEVICE_ID_TAG_NAME)) deviceId = tag.getUUID(DEVICE_ID_TAG_NAME);
        ProjectorContraptionHelper.registerInClientRegistry(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        ProjectorContraptionHelper.unregisterFromClientRegistry(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ProjectorContraptionHelper.unregisterFromClientRegistry(this);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(ENERGY_TAG_NAME, energy.serializeNBT(registries));
        tag.putUUID(DEVICE_ID_TAG_NAME, deviceId);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.deserializeNBT(registries, tag.getCompound(ENERGY_TAG_NAME));
        if (tag.hasUUID(DEVICE_ID_TAG_NAME)) deviceId = tag.getUUID(DEVICE_ID_TAG_NAME);
    }

    public AABB getRenderBoundingBox() {
        renderBounds.update(getBlockState(), getBlockPos());
        return renderBounds.get();
    }

    public void applyProjectorStateClient(final boolean isProjecting, final boolean hasEnergy) {
        if (level == null || !level.isClientSide()) return;
        projectorState.applyClient(isProjecting, hasEnergy);
    }

    public void applyClientFrame(final int width, final int height, final byte[] data) {
        if (level == null || !level.isClientSide()) return;
        frameSender.applyClientFrame(width, height, data);
    }

    public void applyChunk(
            final int width,
            final int height,
            final int chunkIndex,
            final int chunkCount,
            final byte[] data) {
        if (level == null || !level.isClientSide()) return;
        frameSender.applyChunk(width, height, chunkIndex, chunkCount, data);
    }

    private void handleMountedChanged(final boolean value) {
        projectorState.update(
                level,
                getBlockPos(),
                getBlockState(),
                value,
                projectorState.hasEnergy,
                isValid(),
                this);
    }
}