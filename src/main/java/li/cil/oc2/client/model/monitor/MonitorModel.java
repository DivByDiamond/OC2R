//? if >=26.1 {
/*package li.cil.oc2.client.model.monitor;

import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

// Referenced from the blockstate as {"type": "oc2r:monitor"} (the build rewrites the 1.21
// "model" reference on 26.x). Resolves every fragment texture and bakes the dynamic model.
public final class MonitorModel implements CustomUnbakedBlockStateModel {
    public static final MonitorModel INSTANCE = new MonitorModel();
    public static final MapCodec<MonitorModel> CODEC = MapCodec.unit(INSTANCE);

    private static final ModelDebugName DEBUG_NAME = () -> "oc2r:monitor";

    private MonitorModel() {}

    @Override
    public MapCodec<MonitorModel> codec() {
        return CODEC;
    }

    @Override
    public void resolveDependencies(final Resolver resolver) {
        // Only sprites from the block atlas are used, there are no model dependencies.
    }

    @Override
    public BlockStateModel bake(final ModelBaker baker) {
        final Map<String, Material.Baked> materials = new HashMap<>(); // NOPMD - Map ownership is confined to one thread or protected by its explicit monitor.
        for (final String name : MonitorModelTypes.TEXTURE_NAMES) {
            materials.put(
                    name,
                    baker.materials().get(new Material(MonitorModelTypes.texture(name)), DEBUG_NAME)); // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
        }
        return new MonitorBakedModel(materials);
    }
}
*///?} else {
package li.cil.oc2.client.model.monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

public final class MonitorModel implements IUnbakedGeometry<MonitorModel> {
    @Override
    @SuppressWarnings("deprecation")
    public BakedModel bake(
            final IGeometryBakingContext context,
            final ModelBaker baker,
            final Function<Material, TextureAtlasSprite> spriteGetter,
            final ModelState modelState,
            final ItemOverrides overrides) {
        final Map<String, TextureAtlasSprite> sprites = new ConcurrentHashMap<>();
        for (final String name : MonitorModelTypes.TEXTURE_NAMES) {
            // NOPMD - each iteration requires a distinct Material for its texture name.
            final Material material =
                    new Material(TextureAtlas.LOCATION_BLOCKS, MonitorModelTypes.texture(name)); // NOPMD allocation depends on loop iteration / per-item state
            sprites.put(name, spriteGetter.apply(material));
        }
        return new MonitorBakedModel(sprites);
    }
}
//?}
