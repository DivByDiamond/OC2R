package li.cil.oc2.client.model.monitor;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public final class MonitorModelLoader implements IGeometryLoader<MonitorModel> {
    @Override
    public MonitorModel read(
            final JsonObject modelContents, final JsonDeserializationContext context) {
        return new MonitorModel();
    }
}
