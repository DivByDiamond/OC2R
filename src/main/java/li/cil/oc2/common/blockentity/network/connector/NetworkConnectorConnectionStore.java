package li.cil.oc2.common.blockentity.network.connector;

import java.util.List;
import java.util.Set;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import li.cil.oc2.common.util.nbt.NBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

public final class NetworkConnectorConnectionStore {
    private static final String CONNECTIONS_TAG_NAME = "connections";
    private static final String IS_OWNER_TAG_NAME = "is_owner";
    // Upper bound for accepted saved links, independent of the configurable port count so a lowered
    // config never drops existing connections while loading.
    private static final int MAX_CONNECTION_COUNT = 8;

    public static void writeToUpdateTag(
            final CompoundTag tag,
            final HolderLookup.Provider registries,
            final Set<BlockPos> connectorPositions) {
        final List<Tag> connections = new ListTag();
        for (final BlockPos position : connectorPositions) {
            final CompoundTag connectionTag = new CompoundTag(); // NOPMD per-connection data
            //? if >=26.1 {
            /*connectionTag.store("pos", BlockPos.CODEC, position);
            *///?} else {
            connectionTag.put("pos", NbtUtils.writeBlockPos(position));
            //?}
            connections.add(connectionTag);
        }
        tag.put(CONNECTIONS_TAG_NAME, (ListTag) connections);
    }

    public static void readFromUpdateTag(
            final CompoundTag tag,
            final HolderLookup.Provider registries,
            final Set<BlockPos> connectorPositions,
            final Set<BlockPos> dirtyConnectors) {
        // The update tag is the full state: links removed since the last sync must disappear.
        connectorPositions.clear();
        dirtyConnectors.clear();
        //? if >=26.1 {
        /*final List<Tag> connections = tag.getListOrEmpty(CONNECTIONS_TAG_NAME);
        *///?} else {
        final List<Tag> connections = tag.getList(CONNECTIONS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        //?}
        for (int i = 0; i < Math.min(connections.size(), MAX_CONNECTION_COUNT); i++) {
            final CompoundTag connectionTag = (CompoundTag) connections.get(i);
            //? if >=26.1 {
            /*final BlockPos position = connectionTag.read("pos", BlockPos.CODEC).orElseThrow();
            *///?} else {
            final BlockPos position = NbtUtils.readBlockPos(connectionTag, "pos").orElseThrow();
            //?}
            connectorPositions.add(position);
            dirtyConnectors.add(position);
        }
    }

    public static void save(
            final CompoundTag tag,
            final HolderLookup.Provider registries,
            final Set<BlockPos> connectorPositions,
            final Set<BlockPos> ownedCables) {
        final List<Tag> connections = new ListTag();
        for (final BlockPos position : connectorPositions) {
            final CompoundTag connectionTag = new CompoundTag(); // NOPMD per-connection data
            //? if >=26.1 {
            /*connectionTag.store("pos", BlockPos.CODEC, position);
            *///?} else {
            connectionTag.put("pos", NbtUtils.writeBlockPos(position));
            //?}
            if (ownedCables.contains(position)) {
                connectionTag.putBoolean(IS_OWNER_TAG_NAME, true);
            }
            connections.add(connectionTag);
        }
        tag.put(CONNECTIONS_TAG_NAME, (ListTag) connections);
    }

    public static void load(
            final CompoundTag tag,
            final HolderLookup.Provider registries,
            final Set<BlockPos> connectorPositions,
            final Set<BlockPos> dirtyConnectors,
            final Set<BlockPos> ownedCables) {
        connectorPositions.clear();
        dirtyConnectors.clear();
        ownedCables.clear();
        //? if >=26.1 {
        /*final List<Tag> connections = tag.getListOrEmpty(CONNECTIONS_TAG_NAME);
        *///?} else {
        final List<Tag> connections = tag.getList(CONNECTIONS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        //?}
        for (int i = 0; i < Math.min(connections.size(), MAX_CONNECTION_COUNT); i++) {
            final CompoundTag connectionTag = (CompoundTag) connections.get(i);
            final BlockPos position =
                    //? if >=26.1 {
                    /*connectionTag.read("pos", BlockPos.CODEC)
                    *///?} else {
                    NbtUtils.readBlockPos(connectionTag, "pos")
                    //?}
                            .or(() -> NBTUtils.readBlockPosLegacy(connectionTag))
                            .orElseThrow();
            connectorPositions.add(position);
            dirtyConnectors.add(position);
            //? if >=26.1 {
            /*if (connectionTag.getBooleanOr(IS_OWNER_TAG_NAME, false)) {
            *///?} else {
            if (connectionTag.getBoolean(IS_OWNER_TAG_NAME)) {
            //?}
                ownedCables.add(position);
            }
        }
    }
}