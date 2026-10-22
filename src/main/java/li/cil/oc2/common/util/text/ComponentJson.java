package li.cil.oc2.common.util.text;

//? if >=26.1 {
/*import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

// Minecraft 26.x removed Component.Serializer; components are (de)serialized through their codec.
public final class ComponentJson {
    public static String toJson(final Component component, final HolderLookup.Provider registries) {
        final JsonElement json =
                ComponentSerialization.CODEC
                        .encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), component)
                        .getOrThrow();
        return json.toString();
    }

    @Nullable
    public static Component fromJson(final String json, final HolderLookup.Provider registries) {
        return ComponentSerialization.CODEC
                .parse(registries.createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
                .result()
                .orElse(null);
    }

    private ComponentJson() {}
}
*///?}
