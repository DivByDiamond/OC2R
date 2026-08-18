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
    private static final int MAX_CONNECTION_COUNT = 2;

    public static void writeToUpdateTag(
            final CompoundTag tag,
            final HolderLookup.Provider registries,
            final Set<BlockPos> connectorPositions) {
        final List<Tag> connections = new ListTag();
        for (final BlockPos position : connectorPositions) {
            final CompoundTag connectionTag = new CompoundTag(1); // NOPMD per-connection data
            connectionTag.put("pos", NbtUtils.writeBlockPos(position));
            connections.add(connectionTag);
        }
        tag.put(CONNECTIONS_TAG_NAME, (ListTag) connections);
    }

    public static void readFromUpdateTag(
            final CompoundTag tag,
            final HolderLookup.Provider registries,
            final Set<BlockPos> connectorPositions,
            final Set<BlockPos> dirtyConnectors) {
        final List<Tag> connections = tag.getList(CONNECTIONS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(connections.size(), MAX_CONNECTION_COUNT); i++) {
            final CompoundTag connectionTag = (CompoundTag) connections.get(i);
            final BlockPos position = NbtUtils.readBlockPos(connectionTag, "pos").orElseThrow();
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
            final CompoundTag connectionTag = new CompoundTag(2); // NOPMD per-connection data
            connectionTag.put("pos", NbtUtils.writeBlockPos(position));
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
        final List<Tag> connections = tag.getList(CONNECTIONS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(connections.size(), MAX_CONNECTION_COUNT); i++) {
            final CompoundTag connectionTag = (CompoundTag) connections.get(i);
            final BlockPos position =
                    NbtUtils.readBlockPos(connectionTag, "pos")
                            .or(() -> NBTUtils.readBlockPosLegacy(connectionTag))
                            .orElseThrow();
            connectorPositions.add(position);
            dirtyConnectors.add(position);
            if (connectionTag.getBoolean(IS_OWNER_TAG_NAME)) {
                ownedCables.add(position);
            }
        }
    }
}