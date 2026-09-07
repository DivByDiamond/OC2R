package li.cil.oc2.common.serialization.nbt.reference;

import java.lang.reflect.Array;
import java.util.List;
import java.util.UUID;
import li.cil.oc2.common.serialization.ceres.color.ColorDataSerializer;
import li.cil.oc2.common.serialization.nbt.NBTArraySerializer;
import li.cil.oc2.common.serialization.nbt.NBTDeserializerImpl;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import net.minecraft.nbt.*;

public final class NBTReferenceArraySerializers {
    public static final class EnumArraySerializer implements NBTArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            final Enum<?>[] data = (Enum<?>[]) value;
            final ListTag list = new ListTag();
            for (final Enum<?> e : data) {
                if (e != null) {
                    list.add(StringTag.valueOf(e.name()));
                } else {
                    list.add(StringTag.valueOf(""));
                }
            }
            return list;
        }

        @SuppressWarnings({"unchecked", "PMD.CognitiveComplexity", "PMD.CyclomaticComplexity", "PMD.EmptyCatchBlock"}) // legacy ordinal + name fallback is inherently branching
        @Override
        public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
            final Class<?> componentType = type.getComponentType();
            final Object[] enumConstants = componentType.getEnumConstants();

            Enum<?>[] data = (Enum<?>[]) into;
            if (tag instanceof final IntArrayTag intArrayTag) {
                // legacy ordinal storage
                final int[] serializedData = intArrayTag.getAsIntArray();
                if (data == null || data.length != serializedData.length) {
                    data = (Enum<?>[]) Array.newInstance(componentType, serializedData.length);
                }
                for (int i = 0; i < serializedData.length; i++) {
                    final int ordinal = serializedData[i];
                    if (ordinal >= 0 && ordinal < enumConstants.length) {
                        data[i] = (Enum<?>) enumConstants[ordinal];
                    }
                }
            } else if (tag instanceof final ListTag listTag) {
                if (!listTag.isEmpty() && listTag.getElementType() != NBTTagIds.TAG_STRING) {
                    return data;
                }
                if (data == null || data.length != listTag.size()) {
                    data = (Enum<?>[]) Array.newInstance(componentType, listTag.size());
                }
                for (int i = 0; i < listTag.size(); i++) {
                    final String name = listTag.getString(i);
                    if (name.isEmpty()) {
                        data[i] = null;
                        continue;
                    }
                    try {
                        data[i] = Enum.valueOf((Class<Enum>) componentType, name);
                    } catch (final IllegalArgumentException ignored) {
                        // fallback: try legacy ordinal stored as string
                        try {
                            final int ordinal = Integer.parseInt(name);
                            if (ordinal >= 0 && ordinal < enumConstants.length) {
                                data[i] = (Enum<?>) enumConstants[ordinal];
                            }
                        } catch (final NumberFormatException ignored2) {
                            // leave as null
                        }
                    }
                }
            }
            return data;
        }
    }

    public static final class StringArraySerializer implements NBTArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            final String[] data = (String[]) value;
            final List<Tag> list = new ListTag();
            for (final String datum : data) {
                list.add(StringTag.valueOf(datum));
            }
            return (ListTag) list;
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
            String[] data = (String[]) into;
            if (!(tag instanceof ListTag)) {
                return data;
            }
            final ListTag serializedData = (ListTag) tag;
            if (!serializedData.isEmpty() && serializedData.getElementType() != NBTTagIds.TAG_STRING) {
                return data;
            }
            if (data == null || data.length != serializedData.size()) {
                data = new String[serializedData.size()];
            }
            for (int i = 0; i < serializedData.size(); i++) {
                data[i] = serializedData.getString(i);
            }
            return data;
        }
    }

    public static final class UUIDArraySerializer implements NBTArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            final UUID[] data = (UUID[]) value;
            final List<Tag> list = new ListTag();
            for (final UUID datum : data) {
                list.add(StringTag.valueOf(datum.toString()));
            }
            return (ListTag) list;
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
            UUID[] data = (UUID[]) into;
            if (!(tag instanceof ListTag)) {
                return data;
            }
            final ListTag serializedData = (ListTag) tag;
            if (!serializedData.isEmpty() && serializedData.getElementType() != NBTTagIds.TAG_STRING) {
                return data;
            }
            if (data == null || data.length != serializedData.size()) {
                data = new UUID[serializedData.size()];
            }
            for (int i = 0; i < serializedData.size(); i++) {
                data[i] = UUID.fromString(serializedData.getString(i));
            }
            return data;
        }
    }

    public static final class ColorDataArraySerializer implements NBTArraySerializer {
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
            if (tag instanceof IntArrayTag) {
                final int[] serializedData = ((IntArrayTag) tag).getAsIntArray();
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
}
