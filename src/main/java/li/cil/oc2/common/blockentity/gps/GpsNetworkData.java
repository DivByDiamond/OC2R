package li.cil.oc2.common.blockentity.gps;

import com.mojang.serialization.Codec;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.function.Predicate;
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

/** Persists radio membership across chunk unloads and world restarts, separately per dimension. */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName") // The network field backs network().
public final class GpsNetworkData extends SavedData {
    static final Codec<GpsNetworkData> CODEC = GpsNetworkNode.CODEC.listOf().fieldOf("nodes")
            .codec().xmap(GpsNetworkData::new, data -> List.copyOf(data.network.nodes()));
    //? if >=26.1 {
    /*private static final SavedDataType<GpsNetworkData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("oc2r", "gps_networks"), GpsNetworkData::new, CODEC, null);
    *///?} else {
    private static final SavedData.Factory<GpsNetworkData> FACTORY =
            new SavedData.Factory<>(GpsNetworkData::new, GpsNetworkData::load, null);
    private static final String DATA_NAME = "oc2r_gps_networks";
    //?}

    private final GpsNetwork network = new GpsNetwork();

    public GpsNetworkData() {
        super();
    }

    private GpsNetworkData(final List<GpsNetworkNode> nodes) {
        super();
        nodes.forEach(network::put);
    }

    public static GpsNetworkData get(final ServerLevel level) {
        //? if >=26.1 {
        /*return level.getDataStorage().computeIfAbsent(TYPE);
        *///?} else {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        //?}
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Callers query the live network of this level.")
    public GpsNetwork network() {
        return network;
    }

    public void register(final GpsNetworkNode node) {
        if (network.put(node)) setDirty();
    }

    public void remove(final java.util.UUID id) {
        if (network.remove(id)) setDirty();
    }

    public void removeAt(final net.minecraft.core.BlockPos position) {
        for (final GpsNetworkNode node : network.nodes()) {
            if (node.position().equals(position)) remove(node.id());
        }
    }

    /** Validates loaded stations without requesting chunks; unloaded stations retain their last state. */
    public void reconcile(final ServerLevel level) {
        reconcile(level::hasChunkAt, node ->
                level.getBlockEntity(node.position()) instanceof final GpsTowerBlockEntity tower
                        && tower.isAssembled() && tower.matches(node));
    }

    void reconcile(final Predicate<BlockPos> loaded, final Predicate<GpsNetworkNode> valid) {
        for (final GpsNetworkNode node : network.nodes()) {
            if (loaded.test(node.position()) && !valid.test(node)) remove(node.id());
        }
    }

    //? if <26.1 {
    static GpsNetworkData load(final CompoundTag tag, final HolderLookup.Provider registries) {
        return CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.merge((CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow());
        return tag;
    }
    //?}
}
