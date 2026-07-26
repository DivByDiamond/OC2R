/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.bus.device.vm.block.ProjectorDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.ProjectorLoadBalancer;
import li.cil.oc2.common.network.message.ProjectorRequestFramebufferMessage;
import li.cil.oc2.common.network.message.ProjectorStateMessage;
import li.cil.oc2.jcodec.codecs.h264.H264Decoder;
import li.cil.oc2.jcodec.codecs.h264.H264Encoder;
import li.cil.oc2.jcodec.codecs.h264.encode.CQPRateControl;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import javax.annotation.Nullable;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@EventBusSubscriber(modid = API.MOD_ID)
public final class ProjectorBlockEntity extends ModBlockEntity implements TickableBlockEntity {
    @FunctionalInterface
    public interface FrameConsumer {
        void processFrame(final Picture picture);
    }

    ///////////////////////////////////////////////////////////////

    public static final int MAX_RENDER_DISTANCE = 16;
    public static final int MAX_GOOD_RENDER_DISTANCE = 12;
    public static final int MAX_WIDTH = MAX_GOOD_RENDER_DISTANCE + 1; // +1 To make it odd, so we can center.
    public static final int MAX_HEIGHT = (MAX_GOOD_RENDER_DISTANCE * ProjectorDevice.HEIGHT / ProjectorDevice.WIDTH) + 1; // + 1 To match horizontal margin.

    private static final String ENERGY_TAG_NAME = "energy";
    private static final String IS_PROJECTING_TAG_NAME = "projecting";
    private static final String HAS_ENERGY_TAG_NAME = "has_energy";
    private static final String DEVICE_ID_TAG_NAME = "device_id";

    private static final ExecutorService DECODER_WORKERS = Executors.newCachedThreadPool(r -> {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("Projector Frame Decoder");
        return thread;
    });

    /**
     * Client-side registry of "primary" (non-contraption) ProjectorBlockEntities
     * keyed by their persistent device id. Same idea as the equivalent map
     * on ComputerBlockEntity / MonitorBlockEntity: Sable / Create:Aeronautics
     * creates a virtual clone of the projector at a far-away position for
     * contraption rendering. The clone never receives
     * ProjectorFramebufferMessage updates. The BER uses this registry to
     * find the primary projector (same device id, real BlockPos) and use
     * its framebuffer + actual world position when setting up the depth
     * camera.
     */
    private static final ConcurrentHashMap<UUID, ProjectorBlockEntity> PRIMARY_BY_DEVICE_ID = new ConcurrentHashMap<>();
    private static final long VIRTUAL_POSITION_THRESHOLD = 1_000_000L;

    ///////////////////////////////////////////////////////////////

    private final ProjectorDevice projectorDevice = new ProjectorDevice(this, this::handleMountedChanged);
    private boolean isMounted, hasEnergy;
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.projectorEnergyStorage);
    private final Picture picture = Picture.create(ProjectorDevice.WIDTH, ProjectorDevice.HEIGHT, ColorSpace.YUV420J);

    /**
     * Persistent UUID identifying this projector across contraption
     * assembly/disassembly. Preserved via NBT so the original projector and
     * any Sable virtual clone share the same id. Used by the client to find
     * the primary projector (which actually receives framebuffer messages)
     * when the BER / depth renderer is asked to render a virtual clone.
     */
    private UUID deviceId = UUID.randomUUID();

    // Video encoding.
    private final H264Encoder encoder = new H264Encoder(new CQPRateControl(12));
    private final ByteBuffer encoderBuffer = ByteBuffer.allocateDirect(1024 * 1024); // Re-used decompression buffer.
    private boolean needsIDR; // Whether we need to send a keyframe next.

    // Video decoding.
    private final H264Decoder decoder = new H264Decoder();
    @Nullable private CompletableFuture<?> runningDecode; // Current decoding operation, if any, to avoid race conditions.
    private final ByteBuffer decoderBuffer = ByteBuffer.allocateDirect(1024 * 1024); // Re-used decompression buffer.
    @Nullable private FrameConsumer frameConsumer; // Where to throw received frames.

    private AABB renderBounds; // Maximum possible render bounds, assuming we project on furthest away surface.
    private long lastKeepAliveSentAt;

    ///////////////////////////////////////////////////////////////

    public ProjectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.PROJECTOR.get(), pos, state);

        encoder.setKeyInterval(100);

        updateRenderBounds();
    }

    ///////////////////////////////////////////////////////////////

    public boolean isProjecting() {
        if (!isMounted || level == null) {
            return false;
        }

        // Previously this required the immediate neighbor block in the
        // projector's facing direction to be non-solid. That broke projectors
        // placed on Create: Aeronautics airships and other Valkyrien Skies
        // ship worlds, where the surrounding geometry lives in a separate
        // ship level and the projector's own level may just be air around
        // the projector block. We now just require the neighbor chunk to be
        // loaded (so we don't try to project into unloaded territory) and
        // let the depth-buffer based projection in ProjectorDepthRenderer
        // sort out where the light actually lands.
        final Direction facing = getBlockState().getValue(ProjectorBlock.FACING);
        final BlockPos neighborPos = getBlockPos().relative(facing);
        final int neighborChunkX = SectionPos.blockToSectionCoord(neighborPos.getX());
        final int neighborChunkZ = SectionPos.blockToSectionCoord(neighborPos.getZ());
        return level.hasChunk(neighborChunkX, neighborChunkZ);
    }

    public boolean hasEnergy() {
        return hasEnergy;
    }

    public UUID getDeviceId() { return deviceId; }

    /**
     * Returns true if this BlockEntity appears to be a Sable/Create:Aeronautics
     * "virtual clone" — i.e. Sable has teleported its BlockPos to a far-away
     * virtual position for contraption rendering.
     */
    public boolean isContraptionVirtualClone() {
        final BlockPos pos = getBlockPos();
        return Math.abs(pos.getX()) > VIRTUAL_POSITION_THRESHOLD
            || Math.abs(pos.getZ()) > VIRTUAL_POSITION_THRESHOLD;
    }

    /**
     * Look up the "primary" (non-virtual) ProjectorBlockEntity that shares
     * the same persistent device id as this one. Used by the BER and
     * ProjectorDepthRenderer to use the primary's framebuffer and actual
     * world position (the virtual clone's raw BlockPos is a far-away
     * virtual coordinate unsuitable for depth-camera setup).
     */
    @Nullable
    public ProjectorBlockEntity getPrimaryForContraptionRendering() {
        if (!isContraptionVirtualClone()) {
            return this;
        }
        final ProjectorBlockEntity primary = PRIMARY_BY_DEVICE_ID.get(deviceId);
        if (primary != null && !primary.isRemoved()) {
            return primary;
        }
        return this;
    }

    private void registerInClientRegistry() {
        if (level == null || !level.isClientSide()) {
            return;
        }
        if (isContraptionVirtualClone()) {
            return;
        }
        PRIMARY_BY_DEVICE_ID.put(deviceId, this);
    }

    private void unregisterFromClientRegistry() {
        if (level == null || !level.isClientSide()) {
            return;
        }
        PRIMARY_BY_DEVICE_ID.remove(deviceId, this);
    }

    public void setRequiresKeyframe() {
        needsIDR = true;
    }

    public void setFrameConsumer(@Nullable final FrameConsumer consumer) {
        if (consumer == frameConsumer) {
            return;
        }
        synchronized (picture) {
            this.frameConsumer = consumer;
            if (frameConsumer != null) {
                frameConsumer.processFrame(picture);
            }
        }
    }

    public void onRendering() {
        final long now = System.currentTimeMillis();
        if (now - lastKeepAliveSentAt > 1000) {
            lastKeepAliveSentAt = now;
            Network.sendToServer(new ProjectorRequestFramebufferMessage(this));
        }
    }

    @Override
    public void clientTick() {
        registerInClientRegistry();
    }

    @Override
    protected void loadClient() {
        // ProjectorBlock.getTicker uses createServerTicker, so clientTick()
        // is never called for projectors. Register here instead — loadClient
        // is invoked from ModBlockEntity.onLoad() once the BE is added to
        // a client-side level.
        registerInClientRegistry();
    }

    @Override
    public void serverTick() {
        if (!isMounted) {
            return;
        }

        final boolean isPowered;
        if (Config.projectorsUseEnergy()) {
            isPowered = energy.extractEnergy(Config.projectorEnergyPerTick, true) >= Config.projectorEnergyPerTick;
            if (isPowered) {
                energy.extractEnergy(Config.projectorEnergyPerTick, false);
            }
        } else {
            isPowered = true;
        }

        updateProjectorState(isMounted, isPowered);

        if (!hasEnergy || (!projectorDevice.hasChanges() && !needsIDR)) {
            return;
        }

        ProjectorLoadBalancer.offerFrame(this, this::encodeFrame);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);

        tag.putBoolean(IS_PROJECTING_TAG_NAME, isMounted);
        tag.putBoolean(HAS_ENERGY_TAG_NAME, hasEnergy);
        tag.putUUID(DEVICE_ID_TAG_NAME, deviceId);

        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);

        isMounted = tag.getBoolean(IS_PROJECTING_TAG_NAME);
        hasEnergy = tag.getBoolean(HAS_ENERGY_TAG_NAME);
        if (tag.hasUUID(DEVICE_ID_TAG_NAME)) {
            deviceId = tag.getUUID(DEVICE_ID_TAG_NAME);
        }
        registerInClientRegistry();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unregisterFromClientRegistry();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unregisterFromClientRegistry();
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
        if (tag.hasUUID(DEVICE_ID_TAG_NAME)) {
            deviceId = tag.getUUID(DEVICE_ID_TAG_NAME);
        }
    }

    public AABB getRenderBoundingBox() {
        return renderBounds;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBlockState(final BlockState state) {
        super.setBlockState(state);

        updateRenderBounds();
    }

    public void applyProjectorStateClient(final boolean isProjecting, final boolean hasEnergy) {
        if (level == null || !level.isClientSide()) {
            return;
        }

        this.isMounted = isProjecting;
        this.hasEnergy = hasEnergy;
    }

    public void applyNextFrameClient(final ByteBuffer frameData) {
        if (level == null || !level.isClientSide()) {
            return;
        }

        final CompletableFuture<?> lastDecode = runningDecode;
        runningDecode = CompletableFuture.runAsync(() -> {
            try {
                try {
                    if (lastDecode != null) lastDecode.join();
                } catch (final CompletionException ignored) {
                }

                final Inflater inflater = new Inflater();
                inflater.setInput(frameData);

                decoderBuffer.clear();
                inflater.inflate(decoderBuffer);
                decoderBuffer.flip();

                decoder.decodeFrame(decoderBuffer, picture.getData());

                synchronized (picture) {
                    if (frameConsumer != null) {
                        frameConsumer.processFrame(picture);
                    }
                }
            } catch (final DataFormatException ignored) {
            }
        }, DECODER_WORKERS);
    }

    ///////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (Config.projectorsUseEnergy()) {
            event.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final ProjectorBlockEntity self) {
                        return self.energy;
                    }
                    return null;
                },
                Blocks.PROJECTOR.get()
            );
        }
        event.registerBlock(
            Capabilities.Device.BLOCK,
            (level, pos, state, be, side) -> {
                if (be instanceof final ProjectorBlockEntity self) {
                    if (side == self.getBlockState().getValue(ProjectorBlock.FACING).getOpposite()) {
                        return self.projectorDevice;
                    }
                }
                return null;
            },
            Blocks.PROJECTOR.get()
        );
    }

    ///////////////////////////////////////////////////////////////

    private void handleMountedChanged(final boolean value) {
        updateProjectorState(value, hasEnergy);
    }

    private void updateProjectorState(final boolean isMounted, final boolean hasEnergy) {
        if ((isMounted == this.isMounted && hasEnergy == this.hasEnergy) || !isValid()) {
            return;
        }

        // We may get called from unmount() of our device, which can be triggered due to chunk unload.
        // Hence, we need to check the loaded state here, lest we ghost load the chunk, breaking everything.
        if (level != null && !level.isClientSide() && level.isLoaded(getBlockPos())) {
            if (this.isMounted && !isMounted) {
                Arrays.fill(picture.getPlaneData(0), (byte) -128);
                Arrays.fill(picture.getPlaneData(1), (byte) 0);
                Arrays.fill(picture.getPlaneData(2), (byte) 0);
            }

            this.isMounted = isMounted;
            this.hasEnergy = hasEnergy;

            level.setBlock(getBlockPos(), getBlockState().setValue(ProjectorBlock.LIT, isMounted), Block.UPDATE_CLIENTS);

            Network.sendToClientsTrackingBlockEntity(new ProjectorStateMessage(this, isMounted, hasEnergy), this);
        }
    }

    @Nullable
    private ByteBuffer encodeFrame() {
        final boolean hasChanges = projectorDevice.applyChanges(picture);
        if (!hasChanges && !needsIDR) {
            return null;
        }

        encoderBuffer.clear();
        final ByteBuffer frameData;
        try {
            if (needsIDR) {
                frameData = encoder.encodeIDRFrame(picture, encoderBuffer);
                needsIDR = false;
            } else {
                frameData = encoder.encodeFrame(picture, encoderBuffer).data();
            }
        } catch (final BufferOverflowException ignored) {
            return null;
        }

        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(frameData);
        deflater.finish();
        final ByteBuffer compressedFrameData = ByteBuffer.allocateDirect(1024 * 1024);
        deflater.deflate(compressedFrameData, Deflater.FULL_FLUSH);
        deflater.end();
        compressedFrameData.flip();

        return compressedFrameData;
    }

    private void updateRenderBounds() {
        final Direction blockFacing = getBlockState().getValue(ProjectorBlock.FACING);
        final Direction canvasUp = Direction.UP;
        final Direction canvasLeft = blockFacing.getCounterClockWise();

        final BlockPos projectorPos = getBlockPos();
        final BlockPos screenBasePos = projectorPos.relative(blockFacing, MAX_RENDER_DISTANCE);
        final BlockPos screenMinPos = screenBasePos.relative(canvasLeft.getOpposite(), MAX_WIDTH / 2);
        final BlockPos screenMaxPos = screenBasePos.relative(canvasLeft, MAX_WIDTH / 2)
            // -1 for the MAX_HEIGHT padding, -1 for auto-expansion of AABB constructor
            .relative(canvasUp, MAX_HEIGHT - 2);

        renderBounds = new AABB(getBlockPos()).minmax(new AABB(screenMinPos)).minmax(new AABB(screenMaxPos));
    }
}
