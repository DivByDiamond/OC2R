package li.cil.oc2.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fabric counterpart of the NeoForge {@code BusCableBakedModel}: the cable core model with a straight
 * variant, a support stub towards a solid neighbour and the facade of a camouflaged cable. Instead of
 * NeoForge's {@code ModelData} it reads what it needs from the block view while the chunk is rendered
 * (the facade from the block entity, the support from the neighbouring blocks).
 */
@Environment(EnvType.CLIENT)
public final class FabricBusCableModel implements BakedModel {
    private final BakedModel base;
    private final BakedModel[] straightModelByAxis;
    private final BakedModel[] supportModelByFace;

    FabricBusCableModel(
            final BakedModel base,
            final BakedModel[] straightModelByAxis,
            final BakedModel[] supportModelByFace) {
        this.base = base;
        this.straightModelByAxis = straightModelByAxis.clone();
        this.supportModelByFace = supportModelByFace.clone();
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(
            final BlockAndTintGetter view,
            final BlockState state,
            final BlockPos pos,
            final Supplier<RandomSource> random,
            final RenderContext context) {
        if (state.hasProperty(BusCableStateProperties.HAS_FACADE)
                && state.getValue(BusCableStateProperties.HAS_FACADE)) {
            final BlockState facadeState = getFacadeState(view, pos);
            final BakedModel facadeModel = Minecraft.getInstance()
                    .getBlockRenderer()
                    .getBlockModelShaper()
                    .getBlockModel(facadeState);
            facadeModel.emitBlockQuads(view, facadeState, pos, random, context);
            return;
        }

        if (!state.getValue(BusCableStateProperties.HAS_CABLE)) {
            return;
        }

        for (int i = 0; i < Constants.AXES.length; i++) {
            if (BusCableModelUtils.isStraightAlongAxis(state, Constants.AXES[i])) {
                straightModelByAxis[i].emitBlockQuads(view, state, pos, random, context);
                return;
            }
        }

        base.emitBlockQuads(view, state, pos, random, context);

        final Direction supportSide = BusCableModelUtils.getSupportSide(view, pos, state);
        if (supportSide != null) {
            supportModelByFace[supportSide.get3DDataValue()].emitBlockQuads(
                    view, state, pos, random, context);
        }
    }

    private static BlockState getFacadeState(final BlockAndTintGetter view, final BlockPos pos) {
        final BlockEntity blockEntity = view.getBlockEntity(pos);
        BlockState facadeState = null;
        if (blockEntity instanceof final BusCableBlockEntity busCable) {
            final ItemStack facadeItem = busCable.getFacade();
            facadeState = ItemStackUtils.getBlockState(facadeItem);
        }
        if (facadeState == null) {
            facadeState = Blocks.IRON_BLOCK.defaultBlockState();
        }
        return facadeState;
    }

    /** Used where no block view is available: the cable without a support stub. */
    @Override
    public List<BakedQuad> getQuads(
            @Nullable final BlockState state,
            @Nullable final Direction side,
            final RandomSource random) {
        if (state == null || !state.getValue(BusCableStateProperties.HAS_CABLE)) {
            return Collections.emptyList();
        }

        for (int i = 0; i < Constants.AXES.length; i++) {
            if (BusCableModelUtils.isStraightAlongAxis(state, Constants.AXES[i])) {
                return straightModelByAxis[i].getQuads(state, side, random);
            }
        }

        return new ArrayList<>(base.getQuads(state, side, random));
    }

    @Override
    public boolean useAmbientOcclusion() {
        return base.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return base.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return base.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return base.isCustomRenderer();
    }

    @Override
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getParticleIcon() {
        return base.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return base.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return base.getOverrides();
    }
}
