package li.cil.oc2.common.blockentity.network;

import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;

import java.util.HashSet;
import java.util.Set;

final class NetworkConnectorConnectionStore {
    private static final String CONNECTIONS_TAG_NAME = "connections";
    private static final String IS_OWNER_TAG_NAME = "is_owner";
    private static final int MAX_CONNECTION_COUNT = 2;

    static void writeToUpdateTag(
            final CompoundTag tag,
            final HolderLookup.Provider registries,
            final Set<BlockPos> connectorPositions) {
        final ListTag connections = new ListTag();
        for (final BlockPos position : connectorPositions) {
            final CompoundTag connectionTag = new CompoundTag(1);
            connectionTag.put("pos", NbtUtils.writeBlockPos(position));
            connections.add(connectionTag);
        }
        tag.put(CONNECTIONS_TAG_NAME, connections);
    }

    static void readFromUpdateTag(
            final CompoundTag tag,
            final HolderLookup.Provider registries,
            final HashSet<BlockPos> connectorPositions,
            final HashSet<BlockPos> dirtyConnectors) {
        final ListTag connections = tag.getList(CONNECTIONS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(connections.size(), MAX_CONNECTION_COUNT); i++) {
            final CompoundTag connectionTag = connections.getCompound(i);
            final BlockPos position = NbtUtils.readBlockPos(connectionTag, "pos").orElseThrow();
            connectorPositions.add(position);
            dirtyConnectors.add(position);
        }
    }

    static void save(
            final CompoundTag tag,
            final HolderLookup.Provider registries,
            final Set<BlockPos> connectorPositions,
            final Set<BlockPos> ownedCables) {
        final ListTag connections = new ListTag();
        for (final BlockPos position : connectorPositions) {
            final CompoundTag connectionTag = new CompoundTag(2);
            connectionTag.put("pos", NbtUtils.writeBlockPos(position));
            if (ownedCables.contains(position)) {
                connectionTag.putBoolean(IS_OWNER_TAG_NAME, true);
            }
            connections.add(connectionTag);
        }
        tag.put(CONNECTIONS_TAG_NAME, connections);
    }

    static void load(
            final CompoundTag tag,
            final HolderLookup.Provider registries,
            final HashSet<BlockPos> connectorPositions,
            final HashSet<BlockPos> dirtyConnectors,
            final HashSet<BlockPos> ownedCables) {
        final ListTag connections = tag.getList(CONNECTIONS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(connections.size(), MAX_CONNECTION_COUNT); i++) {
            final CompoundTag connectionTag = connections.getCompound(i);
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
