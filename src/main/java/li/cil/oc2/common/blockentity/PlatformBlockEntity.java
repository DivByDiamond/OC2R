package li.cil.oc2.common.blockentity;

import net.minecraft.core.BlockPos;
//? if >=26.1 {
/*import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
*///?}
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NeoForge side of the loader specific block entity base class; the Fabric module provides a class with
 * the same name and API (this file is excluded from the Fabric compile, see fabric/build.gradle.kts).
 *
 * <p>NeoForge patches a few hooks into {@link BlockEntity} that vanilla does not have: {@code onLoad},
 * {@code onChunkUnloaded}, {@code handleUpdateTag}, {@code onDataPacket}, {@code requestModelDataUpdate}
 * and the model data. They are inherited from NeoForge's block entity extension and called by the game;
 * the Fabric class declares them itself and its glue code calls them at the same points, so shared block
 * entity code can override them (and call {@code super}) on every loader.
 */
public abstract class PlatformBlockEntity extends BlockEntity {
    protected PlatformBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    // Spelled out instead of imported: the client import guard test only allows client imports in legacy files.
    //? if >=26.1 {
    /*@Override
    public final net.neoforged.neoforge.model.data.ModelData getModelData() {
        return getCustomModelData() instanceof final net.neoforged.neoforge.model.data.ModelData data ? data : net.neoforged.neoforge.model.data.ModelData.EMPTY;
    }
    *///?} else {
    @Override
    public final net.neoforged.neoforge.client.model.data.ModelData getModelData() {
        return getCustomModelData() instanceof final net.neoforged.neoforge.client.model.data.ModelData data ? data : net.neoforged.neoforge.client.model.data.ModelData.EMPTY;
    }
    //?}

    /**
     * The render model data of this block entity, as an opaque object: the type is loader specific and
     * client only. On NeoForge this is {@code ModelData}; anything else is treated as empty.
     */
    protected Object getCustomModelData() {
        //? if >=26.1 {
        /*return net.neoforged.neoforge.model.data.ModelData.EMPTY;
        *///?} else {
        return net.neoforged.neoforge.client.model.data.ModelData.EMPTY;
        //?}
    }

    //? if >=26.1 {
    /*// Minecraft 26.x replaced the CompoundTag based save/load hooks with ValueOutput/ValueInput. The block
    // entities keep their tag based implementations by overriding the hooks below, which have the 1.21 signatures.

    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    protected final void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        final CompoundTag tag = new CompoundTag();
        final Level level = getLevel();
        saveAdditional(tag, level != null ? level.registryAccess() : RegistryAccess.EMPTY);
        output.store(tag);
    }

    @Override
    protected final void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        loadAdditional(toTag(input), input.lookup());
    }

    @Override
    public final void handleUpdateTag(final ValueInput input) {
        handleUpdateTag(toTag(input), input.lookup());
    }

    @Override
    public final void onDataPacket(final net.minecraft.network.Connection connection, final ValueInput input) {
        handleUpdateTag(input);
    }

    private static CompoundTag toTag(final ValueInput input) {
        return input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new);
    }
    *///?}
}
