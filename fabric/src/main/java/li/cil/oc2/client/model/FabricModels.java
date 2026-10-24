//? if >=26.1 {
/*
package li.cil.oc2.client.model;

import com.mojang.math.Quadrant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.api.API;
import li.cil.oc2.client.model.monitor.MonitorModelTypes;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.types.ConnectionType;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

// Modern Fabric models use the same baked geometry as the other loader, with Fabric's dynamic mesh API.
public final class FabricModels {
    private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_base");
    private static final ResourceLocation STRAIGHT =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_straight");
    private static final ResourceLocation LINK = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_link");
    private static final ResourceLocation PLUG = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_plug");
    private static final Quadrant[] LINK_X = {Quadrant.R270, Quadrant.R90, Quadrant.R0,
            Quadrant.R0, Quadrant.R0, Quadrant.R0};
    private static final Quadrant[] LINK_Y = {Quadrant.R270, Quadrant.R270, Quadrant.R180,
            Quadrant.R0, Quadrant.R90, Quadrant.R270};
    private static final ModelDebugName DEBUG_NAME = () -> "oc2r:monitor";
    private static final Map<String, Material> MONITOR_MATERIALS = new ConcurrentHashMap<>();

    private FabricModels() {}

    public static void register() {
        ModelLoadingPlugin.register(context -> {
            context.registerBlockStateResolver(Blocks.BUS_CABLE.get(), states -> {
                final CableRoot root = new CableRoot();
                for (final BlockState state : states.block().getStateDefinition().getPossibleStates()) {
                    states.setModel(state, root);
                }
            });
            context.registerBlockStateResolver(Blocks.MONITOR.get(), states -> {
                final MonitorRoot root = new MonitorRoot();
                for (final BlockState state : states.block().getStateDefinition().getPossibleStates()) {
                    states.setModel(state, root);
                }
            });
        });
    }

    private static final class CableRoot implements BlockStateModel.UnbakedRoot {
        @Override
        public void resolveDependencies(final ResolvableModel.Resolver resolver) {
            resolver.markDependency(BASE);
            resolver.markDependency(STRAIGHT);
            resolver.markDependency(LINK);
            resolver.markDependency(PLUG);
        }

        @Override
        public BlockStateModel bake(final BlockState state, final ModelBaker baker) {
            final BlockStateModelPart base = new Variant(BASE).bake(baker);
            final BlockStateModelPart[] straight = {
                new Variant(STRAIGHT).withYRot(Quadrant.R90).bake(baker),
                new Variant(STRAIGHT).withXRot(Quadrant.R90).bake(baker),
                new Variant(STRAIGHT).bake(baker)
            };
            final List<BlockStateModelPart> connections = new ArrayList<>();
            if (!state.getValue(BusCableStateProperties.HAS_FACADE)) {
                for (final Direction direction : Direction.values()) {
                    final ConnectionType connection = state.getValue(
                            BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(direction));
                    if (connection == ConnectionType.NONE) continue;
                    final int face = direction.get3DDataValue();
                    connections.add(new Variant(connection == ConnectionType.CABLE ? LINK : PLUG)
                            .withXRot(LINK_X[face]).withYRot(LINK_Y[face]).bake(baker));
                }
            }
            return new FabricBusCableModel(state, base, straight, connections);
        }

        @Override
        public Object visualEqualityGroup(final BlockState state) {
            return state;
        }
    }

    private static final class MonitorRoot implements BlockStateModel.UnbakedRoot {
        @Override
        public void resolveDependencies(final ResolvableModel.Resolver resolver) {}

        @Override
        public BlockStateModel bake(final BlockState state, final ModelBaker baker) {
            final Map<String, Material.Baked> materials = new ConcurrentHashMap<>();
            for (final String name : MonitorModelTypes.TEXTURE_NAMES) {
                final Material material = MONITOR_MATERIALS.computeIfAbsent(name, FabricModels::monitorMaterial);
                materials.put(name, baker.materials().get(material, DEBUG_NAME));
            }
            return new FabricMonitorModel(state, materials);
        }

        @Override
        public Object visualEqualityGroup(final BlockState state) {
            return MonitorModelTypes.dataFromState(state);
        }
    }

    private static Material monitorMaterial(final String name) {
        return new Material(MonitorModelTypes.texture(name));
    }
}
*///?} else {

package li.cil.oc2.client.model;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.client.model.monitor.MonitorModelTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Replaces the models that NeoForge builds through its custom geometry loaders ({@code oc2r:bus_cable}
 * and {@code oc2r:monitor} in the model JSON, which vanilla ignores) after they have been baked from
 * their plain JSON content.
 */
@Environment(EnvType.CLIENT)
public final class FabricModels {
    private static final ResourceLocation BUS_CABLE_BASE_MODEL =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_base");
    private static final ResourceLocation BUS_CABLE_STRAIGHT_MODEL =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_straight");
    private static final ResourceLocation MONITOR_MODEL =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/monitor");

    private FabricModels() {}

    public static void register() {
        ModelLoadingPlugin.register(context -> {
            context.addModels(BUS_CABLE_STRAIGHT_MODEL);
            context.modifyModelAfterBake().register(ModelModifier.WRAP_PHASE, FabricModels::modify);
        });
    }

    @Nullable
    private static BakedModel modify(
            @Nullable final BakedModel model, final ModelModifier.AfterBake.Context context) {
        if (model == null) {
            return null;
        }
        // Models baked as a part of a block state have a resource id; the models of items are top
        // level models without one, which is why the monitor item is recognised by its parent.
        final ResourceLocation id = context.resourceId();
        if (id != null && id.equals(BUS_CABLE_BASE_MODEL)) {
            return wrapBusCable(model, context);
        }
        if (id != null && id.equals(MONITOR_MODEL) || dependsOn(context.sourceModel(), MONITOR_MODEL)) {
            return createMonitor(context);
        }
        return model;
    }

    private static boolean dependsOn(final UnbakedModel model, final ResourceLocation id) {
        return model.getDependencies().contains(id);
    }

    private static BakedModel wrapBusCable(
            final BakedModel base, final ModelModifier.AfterBake.Context context) {
        final var baker = context.baker();
        final var settings = context.settings();
        final BakedModel[] straightModelByAxis = {
            requireNonNull(baker.bake(BUS_CABLE_STRAIGHT_MODEL, BlockModelRotation.X0_Y90)),
            requireNonNull(baker.bake(BUS_CABLE_STRAIGHT_MODEL, BlockModelRotation.X90_Y0)),
            requireNonNull(baker.bake(BUS_CABLE_STRAIGHT_MODEL, settings))
        };
        return new FabricBusCableModel(base, straightModelByAxis);
    }

    @SuppressWarnings("deprecation")
    private static BakedModel createMonitor(final ModelModifier.AfterBake.Context context) {
        final Map<String, TextureAtlasSprite> sprites = new ConcurrentHashMap<>();
        for (final String name : MonitorModelTypes.TEXTURE_NAMES) {
            final Material material =
                    // allocation depends on loop iteration
                    new Material(TextureAtlas.LOCATION_BLOCKS, MonitorModelTypes.texture(name)); // NOPMD
            sprites.put(name, context.textureGetter().apply(material));
        }
        final ItemTransforms transforms = context.sourceModel() instanceof final BlockModel blockModel
                ? blockModel.getTransforms()
                : ItemTransforms.NO_TRANSFORMS;
        return new FabricMonitorModel(sprites, transforms);
    }
}
//?}
