//? if >=26.1 {
/*
package li.cil.oc2.client.model;

import java.util.List;
import java.util.function.Predicate;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Cable shape follows saved connections; facades are resolved from the chunk mesh's block view.
public final class FabricBusCableModel implements BlockStateModel, FabricBlockStateModel {
    private final BlockState state;
    private final BlockStateModelPart part;
    private final List<BlockStateModelPart> connections;
    private final int flags;

    FabricBusCableModel(final BlockState state, final BlockStateModelPart base,
                        final BlockStateModelPart[] straight, final List<BlockStateModelPart> connections) {
        this.state = state;
        this.connections = List.copyOf(connections);
        BlockStateModelPart selected = base;
        for (int i = 0; i < Constants.AXES.length; i++) {
            if (BusCableModelUtils.isStraightAlongAxis(state, Constants.AXES[i])) {
                selected = straight[i];
                break;
            }
        }
        this.part = selected;
        int materialFlags = selected.materialFlags();
        for (final BlockStateModelPart connection : this.connections) {
            materialFlags |= connection.materialFlags();
        }
        this.flags = materialFlags;
    }

    @Override
    public void collectParts(final RandomSource random, final List<BlockStateModelPart> parts) {
        if (state.getValue(BusCableStateProperties.HAS_CABLE)) {
            parts.add(part);
        }
        parts.addAll(connections);
    }

    @Override
    public void emitQuads(final QuadEmitter emitter, final BlockAndTintGetter level,
                          final BlockPos pos, final BlockState blockState, final RandomSource random,
                          final Predicate<Direction> cullTest) {
        final BlockState facade = facade(level, pos, blockState);
        if (facade != null) {
            final BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(facade);
            if (model != this) { // NOPMD - identity prevents recursive rendering for a self facade.
                model.emitQuads(emitter, level, pos, facade, random, cullTest);
            }
        } else {
            if (blockState.getValue(BusCableStateProperties.HAS_CABLE)) {
                part.emitQuads(emitter, cullTest);
            }
            for (final BlockStateModelPart connection : connections) {
                connection.emitQuads(emitter, cullTest);
            }
        }
    }

    @Override
    public Object createGeometryKey(final BlockAndTintGetter level, final BlockPos pos,
                                    final BlockState blockState, final RandomSource random) {
        final BlockState facade = facade(level, pos, blockState);
        if (facade == null) return this;
        final BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(facade);
        if (model == this) return null; // NOPMD - identity prevents recursive geometry resolution.
        final Object key = model.createGeometryKey(level, pos, facade, random);
        return key != null ? new GeometryKey(this, facade, key) : null;
    }

    @Override
    public Material.Baked particleMaterial(final BlockAndTintGetter level, final BlockPos pos,
                                           final BlockState blockState) {
        final BlockState facade = facade(level, pos, blockState);
        if (facade != null) {
            final BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(facade);
            if (model != this) { // NOPMD - identity prevents recursive material resolution.
                return model.particleMaterial(level, pos, facade);
            }
        }
        return particleMaterial();
    }

    @Override
    public int materialFlags(final BlockAndTintGetter level, final BlockPos pos,
                             final BlockState blockState, final RandomSource random) {
        final BlockState facade = facade(level, pos, blockState);
        if (facade != null) {
            final BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(facade);
            if (model != this) { // NOPMD - identity prevents recursive material resolution.
                return model.materialFlags(level, pos, facade, random);
            }
        }
        return materialFlags();
    }

    private record GeometryKey(FabricBusCableModel model, BlockState facade, Object delegatedKey) {}

    private static BlockState facade(final BlockAndTintGetter level, final BlockPos pos, final BlockState state) {
        if (!state.getValue(BusCableStateProperties.HAS_FACADE)) {
            return null;
        }
        if (level.getBlockEntity(pos) instanceof final BusCableBlockEntity cable) {
            final BlockState facade = ItemStackUtils.getBlockState(cable.getFacade());
            if (facade != null) {
                return facade;
            }
        }
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public Material.Baked particleMaterial() {
        return part.particleMaterial();
    }

    @Override
    public int materialFlags() {
        // Static callers have no block view: a facade may provide animated or translucent material.
        return state.getValue(BusCableStateProperties.HAS_FACADE)
                ? flags | net.minecraft.client.resources.model.geometry.BakedQuad.FLAG_ANIMATED
                        | net.minecraft.client.resources.model.geometry.BakedQuad.FLAG_TRANSLUCENT
                : flags;
    }
}
*///?} else {

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
 * variant and the facade of a camouflaged cable. Only explicit block-state connections add cable
 * geometry; adjacent solid blocks must not create phantom supports.
 */
@Environment(EnvType.CLIENT)
public final class FabricBusCableModel implements BakedModel {
    private final BakedModel base;
    private final BakedModel[] straightModelByAxis;

    FabricBusCableModel(final BakedModel base, final BakedModel... straightModelByAxis) {
        this.base = base;
        this.straightModelByAxis = straightModelByAxis.clone();
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
//?}
