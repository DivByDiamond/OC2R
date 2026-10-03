package li.cil.oc2.client.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import li.cil.oc2.client.model.monitor.MonitorModelTypes;
import li.cil.oc2.client.model.monitor.MonitorQuads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fabric counterpart of the NeoForge {@code MonitorBakedModel}: a full cube whose face textures depend
 * on the position of the block inside the monitor multiblock, which is read from the block state, so
 * no render data is needed.
 */
@Environment(EnvType.CLIENT)
public final class FabricMonitorModel implements BakedModel {
    private final Map<String, TextureAtlasSprite> sprites;
    private final ItemTransforms transforms;

    FabricMonitorModel(final Map<String, TextureAtlasSprite> sprites, final ItemTransforms transforms) {
        this.sprites = sprites;
        this.transforms = transforms;
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable final BlockState state,
            @Nullable final Direction side,
            final RandomSource random) {
        // Vanilla queries each of the six sides and then the unculled quads; all faces are on a side.
        if (side == null) {
            return Collections.emptyList();
        }
        return MonitorQuads.quads(sprites, MonitorModelTypes.dataFromState(state), side);
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
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getParticleIcon() {
        return sprites.get(MonitorModelTypes.PARTICLE_TEXTURE);
    }

    @Override
    public ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
