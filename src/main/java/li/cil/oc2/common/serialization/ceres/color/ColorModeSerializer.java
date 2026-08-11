package li.cil.oc2.common.serialization.ceres.color;

import com.google.gson.Gson;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import org.jetbrains.annotations.Nullable;

public class ColorModeSerializer implements Serializer<TerminalColors.ColorMode> {

    @Override
    public void serialize(
            final SerializationVisitor serializationVisitor,
            final Class<TerminalColors.ColorMode> aClass,
            final Object o)
            throws SerializationException {
        final String json = new Gson().toJson(o);
        serializationVisitor.putObject("value", String.class, json);
    }

    @Override
    public TerminalColors.ColorMode deserialize(
            final DeserializationVisitor deserializationVisitor,
            final Class<TerminalColors.ColorMode> aClass,
            @Nullable final Object value)
            throws SerializationException {
        if (!deserializationVisitor.exists("value")) {
            return TerminalColors.ColorMode.SIXTEEN_COLOR;
        }

        final String json = (String) deserializationVisitor.getObject("value", String.class, null);
        if (json == null) {
            return TerminalColors.ColorMode.SIXTEEN_COLOR;
        }

        return new Gson().fromJson(json, TerminalColors.ColorMode.class);
    }
}