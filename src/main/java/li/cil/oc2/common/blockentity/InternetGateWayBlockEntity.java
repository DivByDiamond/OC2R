package li.cil.oc2.common.blockentity;

import java.util.ArrayDeque;
import java.util.Deque;

import li.cil.oc2.api.API;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.inet.InternetAdapter;
import li.cil.oc2.common.inet.InternetConnection;
import li.cil.oc2.common.inet.InternetManagerImpl;
import li.cil.oc2.common.util.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(modid = API.MOD_ID)
public class InternetGateWayBlockEntity extends ModBlockEntity implements NetworkInterface, InternetAdapter {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int QUEUE_MAX = 64;
    private final Deque<byte[]> inboundQueue;
    private final Deque<byte[]> outboundQueue;

    private InternetConnection internetConnection;
    private static final String STATE_TAG = "internet_adapter";
    private Tag internetState;

    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.gatewayEnergyStorage);

    // Animation stuff
    public static final int EMITTER_SIDE_PIXELS = 4;
    public float animProgress[];
    public boolean animReversed[];
    public int inboundCount = 0;
    public int outboundCount = 0;
    public int handledInboundCount = 0;
    public int handledOutboundCount = 0;
    public long lastRender = 0;
    public int pointer = 0;

    protected InternetGateWayBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.INTERNET_GATEWAY.get(), pos, state);
        inboundQueue = new ArrayDeque<>();
        outboundQueue = new ArrayDeque<>();
        animProgress = new float[EMITTER_SIDE_PIXELS*EMITTER_SIDE_PIXELS];
        animReversed = new boolean[EMITTER_SIDE_PIXELS*EMITTER_SIDE_PIXELS];
        internetState = null;
        setNeedsLevelUnloadEvent();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)  {
        super.loadAdditional(tag, registries);
        internetState = tag.get(Constants.INTERNET_ADAPTER_TAG_NAME);
        energy.deserializeNBT(registries, tag.getCompound(Constants.ENERGY_TAG_NAME));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)  {
        super.saveAdditional(tag, registries);
        if (internetConnection != null) {
            internetConnection.saveAdapterState()
                .ifPresent(adapterState -> tag.put(Constants.INTERNET_ADAPTER_TAG_NAME, adapterState));
        }
        tag.put(Constants.ENERGY_TAG_NAME, energy.serializeNBT(registries));
        LOGGER.trace("State saved");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("inbound_count", inboundCount);
        tag.putInt("outbound_count", outboundCount);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider)
    {
        CompoundTag compoundtag = pkt.getTag();
        if (compoundtag != null) {
            handleUpdateTag(compoundtag, lookupProvider);
        }
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        inboundCount = tag.getInt("inbound_count");
        outboundCount = tag.getInt("outbound_count");
        handledInboundCount = Math.max(handledInboundCount, inboundCount-128);
        handledOutboundCount = Math.max(handledOutboundCount, outboundCount-128);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadServer() {
        InternetManagerImpl.getInstance()
            .ifPresent(internetManager -> internetConnection = internetManager.connect(this, internetState));
        if (internetConnection != null) {
            LOGGER.trace("Connected to the internet");
        } else {
            LOGGER.trace("Not connected to the internet");
        }
    }

    protected void unloadServer(final boolean isRemove) {
        if (internetConnection != null) {
            internetConnection.stop();
            LOGGER.trace("Connection stopped");
        }
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
            Capabilities.NetworkInterface.BLOCK,
            (level, pos, state, be, side) -> {
                if (be instanceof final InternetGateWayBlockEntity self) {
                    return self;
                }
                return null;
            },
            Blocks.INTERNET_GATEWAY.get()
        );
        event.registerBlock(
            Capabilities.EnergyStorage.BLOCK,
            (level, pos, state, be, side) -> {
                if (be instanceof final InternetGateWayBlockEntity self) {
                    return self.energy;
                }
                return null;
            },
            Blocks.INTERNET_GATEWAY.get()
        );
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
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
        }
    }
    @Override
    public void sendEthernetFrame(byte[] frame) {
        LOGGER.trace("Got inbound packet");
        if (inboundQueue.size() < QUEUE_MAX) {
            if (tryUseEnergy()) {
                inboundCount += 1;
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
                outboundCount += 1;
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
