package li.cil.oc2.common.serialization.ceres.text;

import javax.annotation.Nullable;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;

public final class TextComponentSerializer implements Serializer<Component> {
    @Override
    public void serialize(
            final SerializationVisitor visitor, final Class<Component> type, final Object value)
            throws SerializationException {
        //? if >=26.1 {
        /*final String json = li.cil.oc2.common.util.text.ComponentJson.toJson((Component) value, RegistryAccess.EMPTY);
        *///?} else {
        final String json = Component.Serializer.toJson((Component) value, RegistryAccess.EMPTY);
        //?}
        visitor.putObject("value", String.class, json);
    }

    @Nullable
    @Override
    public Component deserialize(
            final DeserializationVisitor visitor,
            final Class<Component> type,
            @Nullable final Object value)
            throws SerializationException {
        if (!visitor.exists("value")) {
            return (Component) value;
        }

        final String json = (String) visitor.getObject("value", String.class, null);
        if (json == null) {
            return (Component) value;
        }

        //? if >=26.1 {
        /*return li.cil.oc2.common.util.text.ComponentJson.fromJson(json, RegistryAccess.EMPTY);
        *///?} else {
        return Component.Serializer.fromJson(json, RegistryAccess.EMPTY);
        //?}
    }
}