package li.cil.oc2.common.blockentity;

//? if >=26.1 {
/*import javax.annotation.Nullable;
import li.cil.oc2.common.util.nbt.NBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
*///?} else {
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//?}

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
        //? if >=26.1 {
        /*loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
        *///?} else {
        loadWithComponents(tag, registries);
        //?}
    }

    /** Called on the client with the data of {@link #getUpdatePacket()}; by default the data is loaded. */
    public void onDataPacket(
            final Connection connection,
            final ClientboundBlockEntityDataPacket packet,
            final HolderLookup.Provider registries) {
        final CompoundTag tag = packet.getTag();
        if (!tag.isEmpty()) {
            // Update packets carry getUpdateTag's client payload, not a complete world-save tag.
            // Dispatch through the virtual hook so terminal envelopes and render caches are handled.
            handleUpdateTag(tag, registries);
        }
    }

    /** Asks the client to rebuild the render data of this block entity after it changed. */
    public void requestModelDataUpdate() {
        // Read the field once: BlockEntity#level is mutable (it is cleared when the block entity
        // is removed), so a second read below could see null.
        final Level level = this.level;
        if (level != null && level.isClientSide()) {
            final BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_IMMEDIATE);
        }
    }

    /**
     * The render model data of this block entity, as an opaque object (the type is loader specific and
     * client only). Not used on Fabric yet, so there is no model data to return.
     */
    @Nullable
    protected Object getCustomModelData() {
        return null;
    }

    //? if >=26.1 {
    /*protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    @Override
    protected final void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        final CompoundTag tag = new CompoundTag();
        final Level level = getLevel();
        saveAdditional(tag, level != null ? level.registryAccess() : RegistryAccess.EMPTY);
        output.store(NBTUtils.TAG_MAP_CODEC, tag);
    }

    @Override
    protected final void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        loadAdditional(NBTUtils.toTag(input), input.lookup());
    }
    *///?}

}
