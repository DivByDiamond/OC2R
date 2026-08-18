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
