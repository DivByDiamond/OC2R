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
    private static final ResourceLocation BUS_CABLE_SUPPORT_MODEL =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/cable_support");
    private static final ResourceLocation MONITOR_MODEL =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "block/monitor");

    private FabricModels() {}

    public static void register() {
        ModelLoadingPlugin.register(context -> {
            context.addModels(BUS_CABLE_STRAIGHT_MODEL, BUS_CABLE_SUPPORT_MODEL);
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
        final BakedModel[] supportModelByFace = {
            requireNonNull(baker.bake(BUS_CABLE_SUPPORT_MODEL, BlockModelRotation.X270_Y0)), // -y
            requireNonNull(baker.bake(BUS_CABLE_SUPPORT_MODEL, BlockModelRotation.X90_Y0)), // +y
            requireNonNull(baker.bake(BUS_CABLE_SUPPORT_MODEL, BlockModelRotation.X0_Y180)), // -z
            requireNonNull(baker.bake(BUS_CABLE_SUPPORT_MODEL, settings)), // +z
            requireNonNull(baker.bake(BUS_CABLE_SUPPORT_MODEL, BlockModelRotation.X0_Y90)), // -x
            requireNonNull(baker.bake(BUS_CABLE_SUPPORT_MODEL, BlockModelRotation.X0_Y270)) // +x
        };
        return new FabricBusCableModel(base, straightModelByAxis, supportModelByFace);
    }

    @SuppressWarnings("deprecation")
    private static BakedModel createMonitor(final ModelModifier.AfterBake.Context context) {
        final Map<String, TextureAtlasSprite> sprites = new ConcurrentHashMap<>();
        for (final String name : MonitorModelTypes.TEXTURE_NAMES) {
            final Material material =
                    new Material(TextureAtlas.LOCATION_BLOCKS, MonitorModelTypes.texture(name)); // NOPMD allocation depends on loop iteration
            sprites.put(name, context.textureGetter().apply(material));
        }
        final ItemTransforms transforms = context.sourceModel() instanceof final BlockModel blockModel
                ? blockModel.getTransforms()
                : ItemTransforms.NO_TRANSFORMS;
        return new FabricMonitorModel(sprites, transforms);
    }
}
