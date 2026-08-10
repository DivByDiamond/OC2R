package li.cil.oc2.common.blockentity.misc;

import java.util.ArrayDeque;
import java.util.Deque;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.inet.internet.InternetAdapter;
import li.cil.oc2.common.inet.internet.InternetConnection;
import li.cil.oc2.common.inet.internet.InternetManagerImpl;
import li.cil.oc2.common.util.world.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InternetGateWayBlockEntity extends ModBlockEntity
        implements NetworkInterface, InternetAdapter {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int QUEUE_MAX = 64;
    private final Deque<byte[]> inboundQueue;
    private final Deque<byte[]> outboundQueue;

    private InternetConnection internetConnection;
    private Tag internetState;

    final FixedEnergyStorage energy = new FixedEnergyStorage(Config.gatewayEnergyStorage);

    public final GatewayAnimationState animation = new GatewayAnimationState();

    public InternetGateWayBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.INTERNET_GATEWAY.get(), pos, state);
        inboundQueue = new ArrayDeque<>();
        outboundQueue = new ArrayDeque<>();
        internetState = null;
        setNeedsLevelUnloadEvent();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        internetState = tag.get(Constants.INTERNET_ADAPTER_TAG_NAME);
        energy.deserializeNBT(registries, tag.getCompound(Constants.ENERGY_TAG_NAME));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (internetConnection != null) {
            internetConnection
                    .saveAdapterState()
                    .ifPresent(
                            adapterState ->
                                    tag.put(Constants.INTERNET_ADAPTER_TAG_NAME, adapterState));
        }
        tag.put(Constants.ENERGY_TAG_NAME, energy.serializeNBT(registries));
        LOGGER.trace("State saved");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("inbound_count", animation.inboundCount);
        tag.putInt("outbound_count", animation.outboundCount);
        return tag;
    }

    @Override
    public void onDataPacket(
            Connection net,
            ClientboundBlockEntityDataPacket pkt,
            HolderLookup.Provider lookupProvider) {
        CompoundTag compoundtag = pkt.getTag();
        if (compoundtag != null) {
            handleUpdateTag(compoundtag, lookupProvider);
        }
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        animation.inboundCount = tag.getInt("inbound_count");
        animation.outboundCount = tag.getInt("outbound_count");
        animation.handledInboundCount =
                Math.max(animation.handledInboundCount, animation.inboundCount - 128);
        animation.handledOutboundCount =
                Math.max(animation.handledOutboundCount, animation.outboundCount - 128);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadServer() {
        InternetManagerImpl.getInstance()
                .ifPresent(
                        internetManager ->
                                internetConnection = internetManager.connect(this, internetState));
        if (internetConnection != null) {
            LOGGER.trace("Connected to the internet");
        } else {
            LOGGER.trace("Not connected to the internet");
        }
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        if (internetConnection != null) {
            internetConnection.stop();
            LOGGER.trace("Connection stopped");
        }
    }

    @Override
    public byte[] receiveEthernetFrame() {
        return outboundQueue.pollFirst();
    }

    private boolean tryUseEnergy() {
        boolean hasEnough = energy.getEnergyStored() >= Config.gatewayEnergyPerPacket;
        if (hasEnough) {
            energy.extractEnergy(Config.gatewayEnergyPerPacket, false);
            Level level = getLevel();
            if (level != null) {
                ChunkUtils.setLazyUnsaved(level, getBlockPos());
            }
        }
        return hasEnough;
    }

    private void notifyPlayers() {
        Level level = getLevel();
        if (level != null) {
            // Broadcast the block entity data packet so clients receive the
            // updated inbound/outboundCount values for the sensor animation.
            // sendBlockUpdated alone only triggers a plain block update and does
            // not carry the block entity data (the animation counters).
            if (level instanceof final ServerLevel serverLevel) {
                for (final ServerPlayer player :
                        serverLevel.getChunkSource()
                                .chunkMap
                                .getPlayers(new ChunkPos(getBlockPos()), false)) {
                    player.connection.send(ClientboundBlockEntityDataPacket.create(this));
                }
            }
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public void sendEthernetFrame(byte[] frame) {
        LOGGER.trace("Got inbound packet");
        if (inboundQueue.size() < QUEUE_MAX) {
            if (tryUseEnergy()) {
                animation.inboundCount += 1;
                notifyPlayers();
                inboundQueue.addLast(frame);
            }
        }
    }

    @Override
    public byte[] readEthernetFrame() {
        return inboundQueue.pollFirst();
    }

    @Override
    public void writeEthernetFrame(NetworkInterface source, byte[] frame, int timeToLive) {
        LOGGER.trace("Got outbound packet");
        if (outboundQueue.size() < QUEUE_MAX) {
            if (tryUseEnergy()) {
                animation.outboundCount += 1;
                notifyPlayers();
                outboundQueue.addLast(frame);
            }
        }
    }

    public AABB getRenderBoundingBox() {
        var orig = new AABB(getBlockPos());
        return orig.setMaxY(orig.getMaxPosition().y + 1);
    }
}