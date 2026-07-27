package li.cil.oc2.common.serialization.nbt;

import java.lang.reflect.Array;
import java.util.UUID;
import li.cil.oc2.common.serialization.ceres.ColorDataSerializer;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.vm.terminal.TerminalColors;
import net.minecraft.nbt.*;

final class EnumArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object value) {
        final Enum<?>[] data = (Enum<?>[]) value;
        final int[] convertedData = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            convertedData[i] = data[i].ordinal();
        }
        return new IntArrayTag(convertedData);
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        final Class<?> componentType = type.getComponentType();
        final Object[] enumConstants = componentType.getEnumConstants();

        Enum<?>[] data = (Enum<?>[]) into;
        if (tag instanceof final IntArrayTag intArrayTag) {
            final int[] serializedData = intArrayTag.getAsIntArray();
            if (data == null || data.length != serializedData.length) {
                data = (Enum<?>[]) Array.newInstance(componentType, serializedData.length);
            }
            for (int i = 0; i < serializedData.length; i++) {
                data[i] = (Enum<?>) enumConstants[serializedData[i]];
            }
        }
        return data;
    }
}

final class StringArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object value) {
        final String[] data = (String[]) value;
        final ListTag list = new ListTag();
        for (final String datum : data) {
            list.add(StringTag.valueOf(datum));
        }
        return list;
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        String[] data = (String[]) into;
        if (tag instanceof final ListTag serializedData) {
            if (serializedData.isEmpty()
                    || serializedData.getElementType() == NBTTagIds.TAG_STRING) {
                if (data == null || data.length != serializedData.size()) {
                    data = new String[serializedData.size()];
                }
                for (int i = 0; i < serializedData.size(); i++) {
                    data[i] = serializedData.getString(i);
                }
            }
        }
        return data;
    }
}

final class UUIDArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object value) {
        final UUID[] data = (UUID[]) value;
        final ListTag list = new ListTag();
        for (final UUID datum : data) {
            list.add(StringTag.valueOf(datum.toString()));
        }
        return list;
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        UUID[] data = (UUID[]) into;
        if (tag instanceof final ListTag serializedData) {
            if (serializedData.isEmpty()
                    || serializedData.getElementType() == NBTTagIds.TAG_STRING) {
                if (data == null || data.length != serializedData.size()) {
                    data = new UUID[serializedData.size()];
                }
                for (int i = 0; i < serializedData.size(); i++) {
                    data[i] = UUID.fromString(serializedData.getString(i));
                }
            }
        }
        return data;
    }
}

final class ColorDataArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object obj) {
        final var input = (TerminalColors.ColorData[]) obj;
        final var values = new it.unimi.dsi.fastutil.ints.IntArrayList();
        for (var x : input) {
            values.add(ColorDataSerializer.toInt(x));
        }
        return new IntArrayTag(values);
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        TerminalColors.ColorData[] data = (TerminalColors.ColorData[]) into;
        if (tag instanceof final IntArrayTag intArrayTag) {
            final int[] serializedData = intArrayTag.getAsIntArray();
            if (data == null || data.length != serializedData.length) {
                data = new TerminalColors.ColorData[serializedData.length];
            }
            for (int i = 0; i < data.length; i++) {
                data[i] = ColorDataSerializer.toColorData(serializedData[i]);
            }
            return data;
        } else {
            return NBTDeserializerImpl.getGenericArray(tag, TerminalColors.ColorData.class, into);
        }
    }
}