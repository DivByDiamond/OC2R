package li.cil.oc2.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.blockentity.network.BusCableBlockEntity;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class BusCableBakedModel implements IDynamicBakedModel {
    private final BakedModel proxy;
    private final BakedModel[] straightModelByAxis;
    private final BakedModel[] supportModelByFace;

    BusCableBakedModel(
            BakedModel proxy, BakedModel[] straightModelByAxis, BakedModel... supportModelByFace) {
        this.proxy = proxy;
        this.straightModelByAxis = straightModelByAxis.clone();
        this.supportModelByFace = supportModelByFace.clone();
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
                return facade.model.getQuads(
                        facade.blockState, side, rand, facade.data, RenderType.solid());
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
            if (BusCableModelTypes.isStraightAlongAxis(state, axis)) {
                return straightModelByAxis[i].getQuads(
                        state, side, rand, extraData, RenderType.solid());
            }
        }

        final List<BakedQuad> quads =
                new ArrayList<>(proxy.getQuads(state, side, rand, extraData, RenderType.solid()));

        final BusCableModelTypes.BusCableSupportSide supportSide = extraData.get(BusCableModelTypes.BUS_CABLE_SUPPORT_PROPERTY);
        if (supportSide != null) {
            quads.addAll(
                    supportModelByFace[supportSide.value.get3DDataValue()].getQuads(
                            state, side, rand, extraData, RenderType.solid()));
        }

        return quads;
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

    @SuppressWarnings("deprecation")
    @Override
    public TextureAtlasSprite getParticleIcon() {
        return proxy.getParticleIcon();
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

        Direction supportSide = null;
        for (final Direction direction : Constants.DIRECTIONS) {
            if (BusCableModelTypes.isNeighborInDirectionSolid(level, pos, direction)) {
                final EnumProperty<ConnectionType> property =
                        BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(direction);
                if (state.hasProperty(property)
                        && state.getValue(property) == ConnectionType.INTERFACE) {
                    return blockEntityData; // Plug is already supporting us, bail.
                }

                if (supportSide == null) { // Prefer vertical supports.
                    supportSide = direction;
                }
            }
        }

        if (supportSide != null) {
            return ModelData.builder()
                    .with(BusCableModelTypes.BUS_CABLE_SUPPORT_PROPERTY, new BusCableModelTypes.BusCableSupportSide(supportSide))
                    .build();
        }

        return blockEntityData;
    }
}