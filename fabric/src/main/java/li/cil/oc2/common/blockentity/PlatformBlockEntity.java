package li.cil.oc2.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fabric side of the loader specific block entity base class; the NeoForge module has a class with the
 * same name and API. NeoForge patches a few hooks into {@link BlockEntity} that vanilla does not have
 * ({@code onLoad}, {@code onChunkUnloaded}, {@code handleUpdateTag}, model data). Shared block entity code
 * overrides them here instead; the Fabric glue ({@code FabricBlockEntityHooks} and its mixins) calls
 * them at the same points NeoForge does.
 */
public abstract class PlatformBlockEntity extends BlockEntity {
    protected PlatformBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    /** Called when this block entity is first added to a level, before its first tick. */
    public void onLoad() {
    }

    /** Called when the chunk this block entity is in is unloaded (not when it is removed). */
    public void onChunkUnloaded() {
    }

    /** Called on the client with the data of {@link #getUpdateTag}; by default the data is loaded. */
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        loadWithComponents(tag, registries);
    }

    /** Called on the client with the data of {@link #getUpdatePacket()}; by default the data is loaded. */
    public void onDataPacket(
            final Connection connection,
            final ClientboundBlockEntityDataPacket packet,
            final HolderLookup.Provider registries) {
        final CompoundTag tag = packet.getTag();
        if (!tag.isEmpty()) {
            loadWithComponents(tag, registries);
        }
    }

    /** Asks the client to rebuild the render data of this block entity after it changed. */
    public void requestModelDataUpdate() {
        if (level != null && level.isClientSide()) {
            final BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_IMMEDIATE);
        }
    }

    /**
     * The render model data of this block entity, as an opaque object (the type is loader specific and
     * client only). Not used on Fabric yet.
     */
    protected Object getCustomModelData() {
        return null;
    }
}
