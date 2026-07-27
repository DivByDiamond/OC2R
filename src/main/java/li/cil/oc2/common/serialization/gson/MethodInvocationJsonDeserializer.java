package li.cil.oc2.common.serialization.gson;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.UUID;
import li.cil.oc2.common.bus.adapter.MethodInvocation;

public final class MethodInvocationJsonDeserializer implements JsonDeserializer<MethodInvocation> {
    @Override
    public MethodInvocation deserialize(
            final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        final UUID deviceId = context.deserialize(jsonObject.get("deviceId"), UUID.class);
        final String methodName = jsonObject.get("name").getAsString();
        final JsonElement parameters = jsonObject.get("parameters");
        return new MethodInvocation(
                deviceId,
                methodName,
                parameters != null && parameters.isJsonArray()
                        ? parameters.getAsJsonArray()
                        : new JsonArray());
    }
}