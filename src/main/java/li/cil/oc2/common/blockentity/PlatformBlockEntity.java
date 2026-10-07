package li.cil.oc2.common.blockentity;

import net.minecraft.core.BlockPos;
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
    @Override
    public final net.neoforged.neoforge.client.model.data.ModelData getModelData() {
        return getCustomModelData() instanceof final net.neoforged.neoforge.client.model.data.ModelData data ? data : net.neoforged.neoforge.client.model.data.ModelData.EMPTY;
    }

    /**
     * The render model data of this block entity, as an opaque object: the type is loader specific and
     * client only. On NeoForge this is {@code ModelData}; anything else is treated as empty.
     */
    protected Object getCustomModelData() {
        return net.neoforged.neoforge.client.model.data.ModelData.EMPTY;
    }
}
