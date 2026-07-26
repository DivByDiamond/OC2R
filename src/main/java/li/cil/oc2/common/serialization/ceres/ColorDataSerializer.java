package li.cil.oc2.common.serialization.ceres;

import com.google.gson.Gson;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;
import li.cil.oc2.common.vm.terminal.Terminal;
import org.jetbrains.annotations.Nullable;

public class ColorDataSerializer implements Serializer<Terminal.ColorData> {

    public static int toInt(Terminal.ColorData colorData) {
        var mode = Terminal.ColorMode.SIXTEEN_COLOR;
        if (colorData.Mode != null)
            mode = colorData.Mode;
        return (mode.ordinal() << 24) |
            (colorData.R << 16) |
            (colorData.G << 8) |
            colorData.B;
    }
    public static Terminal.ColorData toColorData(int value) {
        final int mode = (value >> 24) & 0xFF;
        final int red = (value >> 16) & 0xFF;
        final int green = (value >> 8) & 0xFF;
        final int blue = value & 0xFF;

        return new Terminal.ColorData(red, green, blue, Terminal.ColorMode.values()[mode]);
    }

    @Override
    public void serialize(final SerializationVisitor serializationVisitor, final Class<Terminal.ColorData> aClass, final Object o) throws SerializationException {
        Terminal.ColorData colorData = (Terminal.ColorData) o;
        serializationVisitor.putInt("value", toInt(colorData));
    }

    @Override
    public Terminal.ColorData deserialize(final DeserializationVisitor deserializationVisitor, final Class<Terminal.ColorData> aClass, @Nullable final Object o) throws SerializationException {
        if (!deserializationVisitor.exists("value")) {
            return new Terminal.ColorData();
        }

        final int combined = deserializationVisitor.getInt("value");
        return toColorData(combined);
    }
}
