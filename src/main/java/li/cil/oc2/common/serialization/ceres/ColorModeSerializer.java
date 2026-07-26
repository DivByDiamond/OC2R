package li.cil.oc2.common.serialization.ceres;

import com.google.gson.Gson;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;
import li.cil.oc2.common.vm.terminal.Terminal;
import org.jetbrains.annotations.Nullable;

public class ColorModeSerializer implements Serializer<Terminal.ColorMode> {

    @Override
    public void serialize(final SerializationVisitor serializationVisitor, final Class<Terminal.ColorMode> aClass, final Object o) throws SerializationException {
        final String json = new Gson().toJson(o);
        serializationVisitor.putObject("value", String.class, json);
    }

    @Override
    public Terminal.ColorMode deserialize(final DeserializationVisitor deserializationVisitor, final Class<Terminal.ColorMode> aClass, @Nullable final Object value) throws SerializationException {
        if (!deserializationVisitor.exists("value")) {
            return Terminal.ColorMode.SIXTEEN_COLOR;
        }

        final String json = (String) deserializationVisitor.getObject("value", String.class, null);
        if (json == null) {
            return Terminal.ColorMode.SIXTEEN_COLOR;
        }

        return new Gson().fromJson(json, Terminal.ColorMode.class);
    }
}
