package li.cil.oc2.common.blockentity.radio;

import com.google.gson.Gson;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import li.cil.oc2.common.blockentity.gps.GpsTowerBlockEntity;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.radio.RadioAccessPoints;
import li.cil.oc2.common.radio.RadioRegistry;
import li.cil.oc2.common.radio.RadioScheduler;
import li.cil.oc2.common.radio.RadioTopology;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import net.minecraft.core.BlockPos;
//? if <26.1 {
import net.minecraft.core.HolderLookup;
//?}
//? if <26.1 {
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
//?}
//? if >=26.1 {
/*import net.minecraft.resources.Identifier;
*///?}
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=26.1 {
/*import net.minecraft.world.level.saveddata.SavedDataType;
*///?}

/** Dimension-private enrollment and saved relay topology; unloaded nodes never receive frames. */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName") // Fields back their same-named accessors.
public final class RadioNetworkData extends SavedData {
    private record Snapshot(RadioRegistry.State registry, List<RadioTopology.Node> nodes) {}

    private static final Gson GSON = new Gson();
    // Read legacy strings, but write bytes so reservation history can exceed NBT's UTF limit.
    static final Codec<RadioNetworkData> CODEC =
            Codec.either(Codec.STRING, Codec.BYTE_BUFFER)
                    .fieldOf("radio")
                    .codec()
                    .xmap(
                            value ->
                                    decode(
                                            value.map(
                                                    json -> json,
                                                    bytes ->
                                                            StandardCharsets.UTF_8
                                                                    .decode(bytes.duplicate())
                                                                    .toString())),
                            data ->
                                    Either.right(
                                            ByteBuffer.wrap(
                                                    data.encode()
                                                            .getBytes(StandardCharsets.UTF_8))));
    //? if >=26.1 {
    /*private static final SavedDataType<RadioNetworkData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("oc2r", "radio_networks"), RadioNetworkData::new, CODEC, null);
    *///?} else {
    private static final SavedData.Factory<RadioNetworkData> FACTORY =
            new SavedData.Factory<>(RadioNetworkData::new, RadioNetworkData::load, null);
    //?}
    private final RadioRegistry registry;
    private final RadioTopology topology = new RadioTopology();
    private final RadioScheduler scheduler;
    private li.cil.oc2.common.radio.service.RadioServiceBroker broker;
    private OraApplicationLink applicationLink;
    private boolean ticking;
    private boolean delivering;

    public RadioNetworkData() {
        this(new RadioRegistry());
    }

    private RadioNetworkData(final RadioRegistry registry) {
        super();
        this.registry = registry;
        scheduler =
                new RadioScheduler(
                        registry,
                        topology,
                        new RadioScheduler.Config(
                                Config.radioHopDelayTicks,
                                Config.radioMaxHops,
                                Config.radioMaxFrameBytes,
                                Config.radioQueueLimit,
                                Config.radioNetworkQueueLimit,
                                Config.radioDeviceQueueLimit,
                                8192,
                                Config.radioPacketLifetimeTicks,
                                Config.radioLinkBytesPerTick,
                                Config.radioMaxDeliveryTicks));
    }

    public static RadioNetworkData get(final ServerLevel level) {
        //? if >=26.1 {
        /*final RadioNetworkData data = level.getDataStorage().computeIfAbsent(TYPE);
         *///?} else {
        final RadioNetworkData data =
                level.getDataStorage().computeIfAbsent(FACTORY, "oc2r_radio_networks");
        //?}
        if (data.broker == null) {
            data.applicationLink = new OraApplicationLink(data, level);
            data.broker =
                    new li.cil.oc2.common.radio.service.RadioServiceBroker(
                            data.registry, data.applicationLink, System::nanoTime);
            data.broker.tick(level.getGameTime());
        }
        if (!data.ticking) {
            data.ticking = true;
            ServerScheduler.schedule(level, () -> data.tick(level), 1);
        }
        return data;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Callers use the live broker of this level.")
    public li.cil.oc2.common.radio.service.RadioServiceBroker broker() {
        if (broker == null) throw new IllegalStateException("Radio data has no server level");
        return broker;
    }

    public void attach(final OraEndpoint endpoint) {
        applicationLink.attach(endpoint);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Callers use the live registry of this level.")
    public RadioRegistry registry() {
        return registry;
    }

    RadioTopology topology() {
        return topology;
    }

    public boolean register(final RadioTopology.Node node) {
        final var previous = topology.node(node.device()).orElse(null);
        if (previous != null) {
            // Copied block NBT cannot move an existing endpoint identity to another tower.
            if (!position(previous).equals(position(node))
                    || !previous.dimension().equals(node.dimension())) return false;
            if (previous.equals(node)) return true;
        }
        topology.upsert(node);
        setDirty();
        return true;
    }

    public void removeAt(final BlockPos position) {
        for (final RadioTopology.Node node : topology.nodes()) {
            if (position(node).equals(position)) topology.remove(node.device());
        }
        setDirty();
    }

    public void offline(final UUID id) {
        topology.node(id)
                .ifPresent(
                        node -> {
                            if (node.online()) {
                                topology.upsert(
                                        new RadioTopology.Node(
                                                id,
                                                node.network(),
                                                node.dimension(),
                                                node.x(),
                                                node.y(),
                                                node.z(),
                                                node.range(),
                                                false));
                                setDirty();
                            }
                        });
    }

    /**
     * Clients attach only through a loaded own-network relay: directly to a tower in range, or
     * else to a loaded robot dock station in range that uplinks through the nearest tower within
     * tower range of the dock. Height does not affect coverage.
     */
    public java.util.Optional<RadioAccessPoints.Attachment> access(
            final ServerLevel level,
            final UUID endpoint,
            final String credential,
            final double x,
            final double z) {
        reconcile(level);
        final var member = registry.authenticate(endpoint, credential);
        if (member.isEmpty()) return java.util.Optional.empty();
        return RadioAccessPoints.resolve(
                topology.nodes(),
                DockStations.near(level, x, z),
                member.get().network(),
                member.get().dimension(),
                x,
                z);
    }

    public void transmit(
            final ServerLevel level,
            final UUID source,
            final String credential,
            final byte[] frame,
            final int ttl) {
        if (delivering || registry.authenticate(source, credential).isEmpty()) return;
        reconcile(level);
        final var member = registry.member(source).orElseThrow();
        final UUID packet = UUID.randomUUID();
        for (final RadioTopology.Node node : topology.nodes()) {
            if (node.online()
                    && node.network().equals(member.network())
                    && node.dimension().equals(member.dimension())
                    && !node.device().equals(source))
                scheduler.submit(packet, source, node.device(), frame, ttl, level.getGameTime());
        }
    }

    private void tick(final ServerLevel level) {
        reconcile(level);
        if (registry.expireInvites(level.getGameTime()) > 0) setDirty();
        if (registry.expireEnrollmentRequests(level.getGameTime()) > 0) setDirty();
        broker.tick(level.getGameTime());
        applicationLink.tick();
        scheduler.tick(
                level.getGameTime(),
                (source, target, frame, ttl) -> {
                    topology.node(target)
                            .ifPresent(
                                    node -> {
                                        if (level.hasChunkAt(position(node))
                                                && level.getBlockEntity(position(node))
                                                        instanceof final GpsTowerBlockEntity tower
                                                && tower.matchesRadio(node)) {
                                            delivering = true; // NOPMD - read by re-entrant transmit
                                            try {
                                                tower.receiveRadioFrame(frame, ttl);
                                            } finally {
                                                delivering = false;
                                            }
                                        }
                                    });
                });
        ticking = false;
        if (broker != null || scheduler.queued() > 0) {
            ticking = true;
            ServerScheduler.schedule(level, () -> tick(level), 1);
        }
    }

    private void reconcile(final ServerLevel level) {
        reconcile(
                level::hasChunkAt,
                node ->
                        level.getBlockEntity(position(node))
                                        instanceof final GpsTowerBlockEntity tower
                                && tower.matchesRadio(node));
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // Nodes are immutable records.
    void reconcile(final Predicate<BlockPos> loaded, final Predicate<RadioTopology.Node> matches) {
        for (final RadioTopology.Node node : topology.nodes()) {
            final var member = registry.member(node.device());
            if (member.isEmpty()
                    || !member.get().network().equals(node.network())
                    || !member.get().dimension().equals(node.dimension())) {
                topology.remove(node.device());
                setDirty();
                continue;
            }
            if (node.range() != Config.gpsTowerRange) {
                topology.upsert(
                        new RadioTopology.Node(
                                node.device(),
                                node.network(),
                                node.dimension(),
                                node.x(),
                                node.y(),
                                node.z(),
                                Config.gpsTowerRange,
                                node.online()));
                setDirty();
            }
            final BlockPos pos = position(node);
            if (!loaded.test(pos)) {
                if (node.online()) offline(node.device());
                continue;
            }
            if (!matches.test(node)) {
                topology.remove(node.device());
                setDirty();
            }
        }
    }

    private static BlockPos position(final RadioTopology.Node node) {
        return BlockPos.containing(node.x(), node.y(), node.z());
    }

    private String encode() {
        return GSON.toJson(new Snapshot(registry.exportState(), topology.nodes()));
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // Nodes are immutable records.
    private static RadioNetworkData decode(final String json) {
        final Snapshot snapshot = GSON.fromJson(json, Snapshot.class);
        final RadioNetworkData data =
                new RadioNetworkData(RadioRegistry.importState(snapshot.registry()));
        for (final RadioTopology.Node node : snapshot.nodes()) {
            data.topology.upsert(
                    new RadioTopology.Node(
                            node.device(),
                            node.network(),
                            node.dimension(),
                            node.x(),
                            node.y(),
                            node.z(),
                            Config.gpsTowerRange,
                            false));
        }
        return data;
    }

    //? if <26.1 {
    private static RadioNetworkData load(
            final CompoundTag tag, final HolderLookup.Provider registries) { // NOPMD - SavedData.Factory signature
        return CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.merge((CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow());
        return tag;
    }
    //?}
}
