//? if >=26.1 {
/*package li.cil.oc2.client.model.monitor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Dynamic block state model for the fragment-based monitor, ported from OpenComputers' ScreenModel.
// The frame fragment textures depend only on the multiblock position stored in the block state, so
// the geometry of each distinct position is built once and cached.
public final class MonitorBakedModel implements DynamicBlockStateModel {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<String, Material.Baked> materials;
    private final Map<MonitorModelTypes.MonitorData, BlockStateModelPart> partByData =
            new ConcurrentHashMap<>();
    private final Material.Baked particleMaterial;

    MonitorBakedModel(final Map<String, Material.Baked> materials) {
        this.materials = materials;
        this.particleMaterial = materials.get(MonitorModelTypes.PARTICLE_TEXTURE);
    }

    private record GeometryKey(MonitorBakedModel model, @Nullable MonitorModelTypes.MonitorData data) {}

    @Override
    public Object createGeometryKey(
            final BlockAndTintGetter level,
            final BlockPos pos,
            final BlockState state,
            final RandomSource random) {
        return new GeometryKey(this, MonitorModelTypes.dataFromState(state));
    }

    @Override
    public void collectParts(
            final BlockAndTintGetter level,
            final BlockPos pos,
            final BlockState state,
            final RandomSource random,
            final List<BlockStateModelPart> parts) {
        try {
            final MonitorModelTypes.MonitorData data = MonitorModelTypes.dataFromState(state);
            if (data == null) {
                return;
            }
            parts.add(partByData.computeIfAbsent(data, this::buildPart));
        } catch (final RuntimeException e) {
            // Never throw into the chunk mesher; the monitor stays invisible and the log says why.
            LOGGER.error("Failed to build monitor model part at {} for {}", pos, state, e);
        }
    }

    private BlockStateModelPart buildPart(final MonitorModelTypes.MonitorData data) {
        return new SimpleModelWrapper(MonitorQuads.quads(materials, data), true, particleMaterial);
    }

    @Override
    @Deprecated
    public Material.Baked particleMaterial() {
        return particleMaterial;
    }

    @Override
    @Deprecated
    @BakedQuad.MaterialFlags
    public int materialFlags() {
        return 0;
    }
}
*///?} else {
package li.cil.oc2.client.model.monitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Dynamic baked model for the fragment-based monitor, ported from OpenComputers'
 * {@code ScreenModel}. Generates one quad per face of a full cube, picking the frame fragment
 * texture from the block's position inside the multiblock (read from {@link ModelData}, falling
 * back to the BlockState).
 */
public final class MonitorBakedModel implements IDynamicBakedModel {
    private final Map<String, TextureAtlasSprite> sprites;

    MonitorBakedModel(final Map<String, TextureAtlasSprite> sprites) {
        this.sprites = sprites;
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(
            @Nullable final BlockState state,
            @Nullable final Direction side,
            final RandomSource rand,
            final ModelData extraData,
            @Nullable final RenderType renderType) {
        if (renderType != null && !renderType.equals(RenderType.solid())) {
            return Collections.emptyList();
        }

        MonitorModelTypes.MonitorData data = extraData.get(MonitorModelData.MONITOR_PROPERTY);
        if (data == null) {
            data = MonitorModelTypes.dataFromState(state);
        }
        return MonitorQuads.quads(sprites, data, side);
    }

    @Override
    public ModelData getModelData(
            final BlockAndTintGetter level,
            final BlockPos pos,
            final BlockState state,
            final ModelData blockEntityData) {
        return MonitorModelTypes.dataFromState(state) != null
                ? MonitorModelData.fromState(state)
                : blockEntityData;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation") // BakedModel#getParticleIcon is the supported override point
    public TextureAtlasSprite getParticleIcon() {
        return sprites.get(MonitorModelTypes.PARTICLE_TEXTURE);
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
//?}
