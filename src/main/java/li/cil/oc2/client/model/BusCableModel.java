//? if >=26.1 {
/*
package li.cil.oc2.client.model;

import com.mojang.math.Quadrant;
import com.mojang.serialization.MapCodec;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

// Block state model of the bus cable core: bakes the centre piece together with the straight and
// support variants that BusCableBakedModel picks from at mesh time. Referenced from the blockstate
// as {"type": "oc2r:bus_cable"} (the build rewrites the 1.21 "model" reference on 26.x).
public final class BusCableModel implements CustomUnbakedBlockStateModel {
    public static final BusCableModel INSTANCE = new BusCableModel();
    public static final MapCodec<BusCableModel> CODEC = MapCodec.unit(INSTANCE);

    private static final ResourceLocation BUS_CABLE_BASE_MODEL =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_base");
    private static final ResourceLocation BUS_CABLE_STRAIGHT_MODEL =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_straight");
    private static final ResourceLocation BUS_CABLE_SUPPORT_MODEL =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_support");

    private BusCableModel() {}

    @Override
    public MapCodec<BusCableModel> codec() {
        return CODEC;
    }

    @Override
    public void resolveDependencies(final Resolver resolver) {
        resolver.markDependency(BUS_CABLE_BASE_MODEL);
        resolver.markDependency(BUS_CABLE_STRAIGHT_MODEL);
        resolver.markDependency(BUS_CABLE_SUPPORT_MODEL);
    }

    @Override
    public BlockStateModel bake(final ModelBaker baker) {
        final BlockStateModelPart base =
                SimpleModelWrapper.bake(baker, BUS_CABLE_BASE_MODEL, BlockModelRotation.IDENTITY);
        // Indexed like Constants.AXES.
        final BlockStateModelPart[] straightModelByAxis = {
            bake(baker, BUS_CABLE_STRAIGHT_MODEL, Quadrant.R0, Quadrant.R90),
            bake(baker, BUS_CABLE_STRAIGHT_MODEL, Quadrant.R90, Quadrant.R0),
            bake(baker, BUS_CABLE_STRAIGHT_MODEL, Quadrant.R0, Quadrant.R0)
        };
        // Indexed like Direction#get3DDataValue: -y, +y, -z, +z, -x, +x.
        final BlockStateModelPart[] supportModelByFace = {
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R270, Quadrant.R0),
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R90, Quadrant.R0),
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R0, Quadrant.R180),
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R0, Quadrant.R0),
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R0, Quadrant.R90),
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R0, Quadrant.R270)
        };
        return new BusCableBakedModel(base, straightModelByAxis, supportModelByFace);
    }

    private static BlockStateModelPart bake(
            final ModelBaker baker,
            final ResourceLocation model,
            final Quadrant xRotation,
            final Quadrant yRotation) {
        final ModelState state =
                new Variant.SimpleModelState(xRotation, yRotation, Quadrant.R0, false).asModelState();
        return SimpleModelWrapper.bake(baker, model, state);
    }
}
*///?} else {

package li.cil.oc2.client.model;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

public final class BusCableModel implements IUnbakedGeometry<BusCableModel> {
    private static final ResourceLocation BUS_CABLE_STRAIGHT_MODEL =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_straight");
    private static final ResourceLocation BUS_CABLE_SUPPORT_MODEL =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_support");
    private final IUnbakedGeometry<?> proxy;

    BusCableModel(final IUnbakedGeometry<?> proxy) {
        this.proxy = proxy;
    }

    @Override
    public BakedModel bake(
            final IGeometryBakingContext owner,
            final ModelBaker baker,
            final Function<Material, TextureAtlasSprite> spriteGetter,
            final ModelState modelTransform,
            final ItemOverrides overrides) {
        final BakedModel bakedBaseModel =
                proxy.bake(owner, baker, spriteGetter, modelTransform, overrides);
        final BakedModel[] straightModelByAxis = {
            requireNonNull(
                    baker.bake(BUS_CABLE_STRAIGHT_MODEL, BlockModelRotation.X0_Y90, spriteGetter)),
            requireNonNull(
                    baker.bake(BUS_CABLE_STRAIGHT_MODEL, BlockModelRotation.X90_Y0, spriteGetter)),
            requireNonNull(baker.bake(BUS_CABLE_STRAIGHT_MODEL, modelTransform, spriteGetter))
        };
        final BakedModel[] supportModelByFace = {
            requireNonNull(
                    baker.bake(
                            BUS_CABLE_SUPPORT_MODEL,
                            BlockModelRotation.X270_Y0,
                            spriteGetter)), // -y
            requireNonNull(
                    baker.bake(
                            BUS_CABLE_SUPPORT_MODEL,
                            BlockModelRotation.X90_Y0,
                            spriteGetter)), // +y
            requireNonNull(
                    baker.bake(
                            BUS_CABLE_SUPPORT_MODEL,
                            BlockModelRotation.X0_Y180,
                            spriteGetter)), // -z
            requireNonNull(baker.bake(BUS_CABLE_SUPPORT_MODEL, modelTransform, spriteGetter)), // +z
            requireNonNull(
                    baker.bake(
                            BUS_CABLE_SUPPORT_MODEL,
                            BlockModelRotation.X0_Y90,
                            spriteGetter)), // -x
            requireNonNull(
                    baker.bake(
                            BUS_CABLE_SUPPORT_MODEL,
                            BlockModelRotation.X0_Y270,
                            spriteGetter)) // +x
        };

        return new BusCableBakedModel(bakedBaseModel, straightModelByAxis, supportModelByFace);
    }
}
//?}
