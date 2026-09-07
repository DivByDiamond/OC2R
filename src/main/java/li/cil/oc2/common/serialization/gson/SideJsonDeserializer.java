package li.cil.oc2.common.serialization.gson;

import com.google.gson.*;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import li.cil.oc2.api.util.Side;

public final class SideJsonDeserializer implements JsonDeserializer<Side> {
    @Override
    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts") // legacy ordinal vs name fallback is inherently nested
    public Side deserialize(
            final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonPrimitive()) {
            final JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
            if (jsonPrimitive.isNumber()) {
                final int ordinal = jsonPrimitive.getAsNumber().intValue();
                final Side[] constants = Side.class.getEnumConstants();
                if (ordinal >= 0 && ordinal < constants.length) {
                    return constants[ordinal];
                }
                throw new JsonParseException("Unknown Side ordinal: " + ordinal);
            }
        }

        return (Side)
                TypeAdapters.ENUM_FACTORY.create(null, TypeToken.get(typeOfT)).fromJsonTree(json);
    }
}