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
