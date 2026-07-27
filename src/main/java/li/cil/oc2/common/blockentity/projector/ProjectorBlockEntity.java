package li.cil.oc2.common.blockentity.projector;

import java.nio.ByteBuffer;
import java.util.UUID;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.ProjectorDevice;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.loadbalancer.ProjectorLoadBalancer;
import li.cil.oc2.common.network.message.projector.ProjectorRequestFramebufferMessage;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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

    private final ProjectorDevice projectorDevice =
            new ProjectorDevice(this, this::handleMountedChanged);
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.projectorEnergyStorage);
    private final Picture picture =
            Picture.create(ProjectorDevice.WIDTH, ProjectorDevice.HEIGHT, ColorSpace.YUV420J);
    private final ProjectorVideoEncoder videoEncoder = new ProjectorVideoEncoder();
    private final ProjectorVideoDecoder videoDecoder = new ProjectorVideoDecoder();
    private final ProjectorState projectorState = new ProjectorState();
    private final ProjectorRenderBounds renderBounds = new ProjectorRenderBounds();

    UUID deviceId = UUID.randomUUID();
    private long lastKeepAliveSentAt;

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

    public void setRequiresKeyframe() {
        videoEncoder.setRequiresKeyframe();
    }

    public void setFrameConsumer(@Nullable final FrameConsumer consumer) {
        videoDecoder.setFrameConsumer(picture, consumer);
    }

    public void onRendering() {
        final long now = System.currentTimeMillis();
        if (now - lastKeepAliveSentAt > 1000) {
            lastKeepAliveSentAt = now;
            NetworkMessages.sendToServer(new ProjectorRequestFramebufferMessage(this));
        }
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
                picture,
                projectorState.isMounted,
                isPowered,
                isValid(),
                this);
        if (!projectorState.hasEnergy || !videoEncoder.hasChangesOrNeedsIDR(projectorDevice))
            return;
        ProjectorLoadBalancer.offerFrame(
                this, () -> videoEncoder.encodeFrame(projectorDevice, picture));
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
        return renderBounds.get();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBlockState(final BlockState state) {
        super.setBlockState(state);
        renderBounds.update(state, getBlockPos());
    }

    public void applyProjectorStateClient(final boolean isProjecting, final boolean hasEnergy) {
        if (level == null || !level.isClientSide()) return;
        projectorState.applyClient(isProjecting, hasEnergy);
    }

    public void applyNextFrameClient(final ByteBuffer frameData) {
        if (level == null || !level.isClientSide()) return;
        videoDecoder.applyNextFrameClient(picture, frameData);
    }

    private void handleMountedChanged(final boolean value) {
        projectorState.update(
                level,
                getBlockPos(),
                getBlockState(),
                picture,
                value,
                projectorState.hasEnergy,
                isValid(),
                this);
    }
}