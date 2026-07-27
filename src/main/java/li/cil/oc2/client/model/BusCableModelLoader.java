package li.cil.oc2.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.neoforged.neoforge.client.model.ElementsModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public final class BusCableModelLoader implements IGeometryLoader<BusCableModel> {

    @Override
    public BusCableModel read(
            final JsonObject modelContents, final JsonDeserializationContext context) {
        return new BusCableModel(ElementsModel.Loader.INSTANCE.read(modelContents, context));
    }
}
