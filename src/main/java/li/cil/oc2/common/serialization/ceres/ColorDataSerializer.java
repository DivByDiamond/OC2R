package li.cil.oc2.common.serialization.ceres;

import com.google.gson.Gson;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;
import li.cil.oc2.common.vm.terminal.TerminalColors;
import li.cil.oc2.common.vm.terminal.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.TerminalColors.ColorMode;
import org.jetbrains.annotations.Nullable;

public class ColorDataSerializer implements Serializer<ColorData> {

    public static int toInt(ColorData colorData) {
        var mode = ColorMode.SIXTEEN_COLOR;
        if (colorData.Mode != null)
            mode = colorData.Mode;
        return (mode.ordinal() << 24) |
            (colorData.R << 16) |
            (colorData.G << 8) |
            colorData.B;
    }
    public static ColorData toColorData(int value) {
        final int mode = (value >> 24) & 0xFF;
        final int red = (value >> 16) & 0xFF;
        final int green = (value >> 8) & 0xFF;
        final int blue = value & 0xFF;

        return new ColorData(red, green, blue, ColorMode.values()[mode]);
    }

    @Override
    public void serialize(final SerializationVisitor serializationVisitor, final Class<ColorData> aClass, final Object o) throws SerializationException {
        ColorData colorData = (ColorData) o;
        serializationVisitor.putInt("value", toInt(colorData));
    }

    @Override
    public ColorData deserialize(final DeserializationVisitor deserializationVisitor, final Class<ColorData> aClass, @Nullable final Object o) throws SerializationException {
        if (!deserializationVisitor.exists("value")) {
            return new ColorData();
        }

        final int combined = deserializationVisitor.getInt("value");
        return toColorData(combined);
    }
}
