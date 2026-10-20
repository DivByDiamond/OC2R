//? if >=26.1 {
/*package li.cil.oc2.client.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;

//? if <26.1 {
import net.minecraft.core.Direction;
//?}
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Cable geometry follows explicit block-state connections. A neighboring solid block alone
// must not create a visible connection; model data only supplies camouflage.
public final class BusCableBakedModel implements DynamicBlockStateModel {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicBoolean FAILURE_LOGGED = new AtomicBoolean();

    private final BlockStateModelPart base;
    private final BlockStateModelPart[] straightModelByAxis;
    private final BlockStateModelPart[] supportModelByFace;
    @BakedQuad.MaterialFlags
    private final int materialFlags;

    BusCableBakedModel(
            final BlockStateModelPart base,
            final BlockStateModelPart[] straightModelByAxis,
            final BlockStateModelPart... supportModelByFace) {
        this.base = base;
        this.straightModelByAxis = straightModelByAxis.clone();
        this.supportModelByFace = supportModelByFace.clone();

        int flags = base.materialFlags();
        for (final BlockStateModelPart part : this.straightModelByAxis) {
            flags |= part.materialFlags();
        }
        for (final BlockStateModelPart part : this.supportModelByFace) {
            flags |= part.materialFlags();
        }
        this.materialFlags = flags;
    }

    @Override
    public void collectParts(
            final BlockAndTintGetter level,
            final BlockPos pos,
            final BlockState state,
            final RandomSource random,
            final List<BlockStateModelPart> parts) {
        try {
            if (isFacade(state)) {
                final BlockState facadeState = getFacadeState(level, pos);
                final BlockStateModel facadeModel = getFacadeModel(facadeState);
                if (facadeModel != null) {
                    facadeModel.collectParts(level, pos, facadeState, random, parts);
                }
                return;
            }

            if (!state.hasProperty(BusCableStateProperties.HAS_CABLE)
                    || !state.getValue(BusCableStateProperties.HAS_CABLE)) {
                return;
            }

            for (int i = 0; i < Constants.AXES.length; i++) {
                if (BusCableModelUtils.isStraightAlongAxis(state, Constants.AXES[i])) {
                    parts.add(straightModelByAxis[i]);
                    return;
                }
            }

            parts.add(base);
        } catch (final RuntimeException e) {
            // Never throw into the chunk mesher; render nothing for this block and say why once.
            if (FAILURE_LOGGED.compareAndSet(false, true)) {
                LOGGER.error("Failed to collect bus cable model parts at {} for {}", pos, state, e);
            }
        }
    }

    @Override
    @Deprecated
    public Material.Baked particleMaterial() {
        return base.particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial(
            final BlockAndTintGetter level, final BlockPos pos, final BlockState state) {
        if (isFacade(state)) {
            final BlockState facadeState = getFacadeState(level, pos);
            final BlockStateModel facadeModel = getFacadeModel(facadeState);
            if (facadeModel != null) {
                return facadeModel.particleMaterial(level, pos, facadeState);
            }
        }
        return base.particleMaterial();
    }

    @Override
    @Deprecated
    @BakedQuad.MaterialFlags
    public int materialFlags() {
        return materialFlags;
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags(
            final BlockAndTintGetter level, final BlockPos pos, final BlockState state) {
        if (isFacade(state)) {
            final BlockState facadeState = getFacadeState(level, pos);
            final BlockStateModel facadeModel = getFacadeModel(facadeState);
            if (facadeModel != null) {
                return materialFlags | facadeModel.materialFlags(level, pos, facadeState);
            }
        }
        return materialFlags;
    }

    private static boolean isFacade(final BlockState state) {
        return state.hasProperty(BusCableStateProperties.HAS_FACADE)
                && state.getValue(BusCableStateProperties.HAS_FACADE);
    }

    private static BlockState getFacadeState(final BlockAndTintGetter level, final BlockPos pos) {
        final ModelData data = level.getModelData(pos);
        final BlockState facadeState =
                data.has(BusCableModelTypes.BUS_CABLE_FACADE_STATE_PROPERTY)
                        ? data.get(BusCableModelTypes.BUS_CABLE_FACADE_STATE_PROPERTY)
                        : null;
        return facadeState != null ? facadeState : Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Nullable
    private BlockStateModel getFacadeModel(final BlockState facadeState) {
        final BlockStateModel model =
                Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(facadeState);
        return model == this ? null : model; // NOPMD - A cable as its own facade would recurse forever.
    }
}
*///?} else {
package li.cil.oc2.client.model;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
//? if <26.1 {
import net.minecraft.core.Direction;
//?}
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class BusCableBakedModel implements IDynamicBakedModel {
    private final BakedModel proxy;
    private final BakedModel[] straightModelByAxis;

    BusCableBakedModel(
            BakedModel proxy, BakedModel[] straightModelByAxis, BakedModel... supportModelByFace) { // NOPMD - Legacy supports use procedural geometry; retain the shared bake signature.
        this.proxy = proxy;
        this.straightModelByAxis = straightModelByAxis.clone();
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(
            @Nullable final BlockState state,
            @Nullable final Direction side,
            final RandomSource rand,
            final ModelData extraData,
            @Nullable RenderType renderType) {
        final RenderType layer = RenderType.solid();

        if (extraData.has(BusCableModelTypes.BUS_CABLE_FACADE_PROPERTY)) {
            final BusCableModelTypes.BusCableFacade facade = extraData.get(BusCableModelTypes.BUS_CABLE_FACADE_PROPERTY);
            if (facade != null) {
                return facade.model().getQuads(
                        facade.blockState(), side, rand, facade.data(), renderType);
            } else {
                return Collections.emptyList();
            }
        }

        if (state == null
                || !state.getValue(BusCableStateProperties.HAS_CABLE)
                || !layer.equals(RenderType.solid())) {
            return Collections.emptyList();
        }

        for (int i = 0; i < Constants.AXES.length; i++) {
            final Direction.Axis axis = Constants.AXES[i];
            if (BusCableModelUtils.isStraightAlongAxis(state, axis)) {
                return straightModelByAxis[i].getQuads(
                        state, side, rand, extraData, RenderType.solid());
            }
        }

        return proxy.getQuads(state, side, rand, extraData, RenderType.solid());
    }

    /**
     * A facade is drawn in the layers of the block it imitates: blocks with a transparent overlay
     * (grass, mycelium) need their cutout layer, otherwise the overlay's transparent pixels cover the
     * block in black.
     */
    @Override
    public ChunkRenderTypeSet getRenderTypes(
            final BlockState state, final RandomSource rand, final ModelData data) {
        final BusCableModelTypes.BusCableFacade facade = data.get(BusCableModelTypes.BUS_CABLE_FACADE_PROPERTY);
        if (facade != null) {
            return facade.model().getRenderTypes(facade.blockState(), rand, facade.data());
        }
        return IDynamicBakedModel.super.getRenderTypes(state, rand, data);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return proxy.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return proxy.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return proxy.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return proxy.isCustomRenderer();
    }

    @Override
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getParticleIcon() {
        return proxy.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(final ModelData data) {
        return proxy.getParticleIcon(data);
    }

    @Override
    public ItemOverrides getOverrides() {
        return proxy.getOverrides();
    }

    @Override
    @Nonnull
    public ModelData getModelData(
            final BlockAndTintGetter level,
            final BlockPos pos,
            final BlockState state,
            final ModelData blockEntityData) {
        if (state.hasProperty(BusCableStateProperties.HAS_FACADE)
                && state.getValue(BusCableStateProperties.HAS_FACADE)) {
            return getFacadeModelData(level, pos, blockEntityData);
        }

        // Neighbouring solid blocks do not imply a cable or interface connection.
        return ModelData.EMPTY;
    }

    @Nullable
    private ModelData getFacadeModelData(
            final BlockAndTintGetter level,
            final BlockPos pos,
            final ModelData blockEntityData) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);

        BlockState facadeState = null;
        if (blockEntity instanceof final BusCableBlockEntity busCable) {
            final ItemStack facadeItem = busCable.getFacade();
            facadeState = ItemStackUtils.getBlockState(facadeItem);
        }
        if (facadeState == null) {
            facadeState = Blocks.IRON_BLOCK.defaultBlockState();
        }

        final BlockModelShaper shapes =
                Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        final BakedModel model = shapes.getBlockModel(facadeState);
        final ModelData data = model.getModelData(level, pos, facadeState, blockEntityData);

        return ModelData.builder()
                .with(BusCableModelTypes.BUS_CABLE_FACADE_PROPERTY, new BusCableModelTypes.BusCableFacade(facadeState, model, data))
                .build();
    }
}
//?}
