package li.cil.oc2.common.serialization.nbt;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.ceres.Ceres;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.oc2.common.serialization.nbt.reference.NBTReferenceArraySerializers.ColorDataArraySerializer;
import li.cil.oc2.common.serialization.nbt.reference.NBTReferenceArraySerializers.EnumArraySerializer;
import li.cil.oc2.common.serialization.nbt.reference.NBTReferenceArraySerializers.StringArraySerializer;
import li.cil.oc2.common.serialization.nbt.reference.NBTReferenceArraySerializers.UUIDArraySerializer;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;

public record NBTDeserializerImpl(CompoundTag tag) implements DeserializationVisitor {
    private static final String IS_NULL_KEY = "<is_null>";

    public static final Map<Class<?>, NBTArraySerializer> ARRAY_SERIALIZERS = new ConcurrentHashMap<>();

    static {
        ARRAY_SERIALIZERS.put(boolean.class, new BooleanArraySerializer());
        ARRAY_SERIALIZERS.put(byte.class, new ByteArraySerializer());
        ARRAY_SERIALIZERS.put(char.class, new CharArraySerializer());
        ARRAY_SERIALIZERS.put(short.class, new ShortArraySerializer());
        ARRAY_SERIALIZERS.put(int.class, new IntArraySerializer());
        ARRAY_SERIALIZERS.put(long.class, new LongArraySerializer());
        ARRAY_SERIALIZERS.put(float.class, new FloatArraySerializer());
        ARRAY_SERIALIZERS.put(double.class, new DoubleArraySerializer());
        ARRAY_SERIALIZERS.put(Enum.class, new EnumArraySerializer());
        ARRAY_SERIALIZERS.put(String.class, new StringArraySerializer());
        ARRAY_SERIALIZERS.put(UUID.class, new UUIDArraySerializer());
        ARRAY_SERIALIZERS.put(TerminalColors.ColorData.class, new ColorDataArraySerializer());
    }

    @Override
    public boolean getBoolean(final String name) {
        //? if >=26.1 {
        /*return tag.getBooleanOr(name, false);
        *///?} else {
        return tag.getBoolean(name);
        //?}
    }

    @Override
    public byte getByte(final String name) {
        //? if >=26.1 {
        /*return tag.getByteOr(name, (byte) 0);
        *///?} else {
        return tag.getByte(name);
        //?}
    }

    @Override
    public char getChar(final String name) {
        //? if >=26.1 {
        /*return (char) tag.getIntOr(name, 0);
        *///?} else {
        return (char) tag.getInt(name);
        //?}
    }

    @Override
    public short getShort(final String name) {
        //? if >=26.1 {
        /*return tag.getShortOr(name, (short) 0);
        *///?} else {
        return tag.getShort(name);
        //?}
    }

    @Override
    public int getInt(final String name) {
        //? if >=26.1 {
        /*return tag.getIntOr(name, 0);
        *///?} else {
        return tag.getInt(name);
        //?}
    }

    @Override
    public long getLong(final String name) {
        //? if >=26.1 {
        /*return tag.getLongOr(name, 0L);
        *///?} else {
        return tag.getLong(name);
        //?}
    }

    @Override
    public float getFloat(final String name) {
        //? if >=26.1 {
        /*return tag.getFloatOr(name, 0.0F);
        *///?} else {
        return tag.getFloat(name);
        //?}
    }

    @Override
    public double getDouble(final String name) {
        //? if >=26.1 {
        /*return tag.getDoubleOr(name, 0.0);
        *///?} else {
        return tag.getDouble(name);
        //?}
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    @Override
    public Object getObject(final String name, final Class<?> type, @Nullable final Object into)
            throws SerializationException {
        if (isNull(name)) {
            return null;
        }

        if (!tag.contains(name)) {
            return into;
        }

        if (type.isArray()) {
            final Tag arrayTag = tag.get(name);
            assert arrayTag != null;
            return getArray(arrayTag, type, into);
        } else if (type.isEnum()) {
            //? if >=26.1 {
            /*return Enum.valueOf((Class) type, tag.getStringOr(name, ""));
            *///?} else {
            return Enum.valueOf((Class) type, tag.getString(name));
            //?}
        } else if (type == String.class) {
            return tag.getString(name);
        } else if (type == UUID.class) {
            //? if >=26.1 {
            /*return tag.getCompoundOrEmpty(name).read(name, net.minecraft.core.UUIDUtil.CODEC).orElseThrow();
            *///?} else {
            return tag.getCompound(name).getUUID(name);
            //?}
        } else {
            //? if >=26.1 {
            /*final CompoundTag valueTag = tag.getCompoundOrEmpty(name);
            *///?} else {
            final CompoundTag valueTag = tag.getCompound(name);
            //?}
            return Ceres.getSerializer(type)
                    .deserialize(new NBTDeserializerImpl(valueTag), (Class) type, into);
        }
    }

    @Override
    public boolean exists(final String name) {
        return tag.contains(name);
    }

    private boolean isNull(final String name) {
        //? if >=26.1 {
        /*return tag.getCompoundOrEmpty(name).getBooleanOr(IS_NULL_KEY, false);
        *///?} else {
        return tag.getCompound(name).getBoolean(IS_NULL_KEY);
        //?}
    }

    @Nullable
    public static Object getArray(final Tag tag, final Class<?> type, final @Nullable Object into) {
        final Class<?> componentType = type.getComponentType();
        final NBTArraySerializer arraySerializer = ARRAY_SERIALIZERS.get(componentType);
        if (arraySerializer != null) {
            return arraySerializer.deserialize(tag, type, into);
        } else {
            return getGenericArray(tag, componentType, into);
        }
    }

    @Nullable
    public static Object getGenericArray(
            final Tag tag, final Class<?> componentType, final @Nullable Object into) {
        final ArrayComponentDeserializer componentDeserializer =
                createComponentDeserializer((Class) componentType);

        final List<Tag> listTag;
        final int[] nulls;
        if (tag instanceof ListTag) {
            listTag = (ListTag) tag;
            nulls = new int[0];
        } else if (tag instanceof CompoundTag) {
            final CompoundTag compoundTag = (CompoundTag) tag;
            listTag = (ListTag) compoundTag.get("value");
            //? if >=26.1 {
            /*nulls = compoundTag.getIntArray("nulls").orElse(new int[0]);
            *///?} else {
            nulls = compoundTag.getIntArray("nulls");
            //?}
        } else {
            return (Object[]) into;
        }

        return deserializeIntoArray(listTag, nulls, componentType, into, componentDeserializer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // generic array deserialization is inherently untype-safe
    private static ArrayComponentDeserializer createComponentDeserializer(final Class<?> componentType) {
        if (componentType.isArray()) {
            return NBTDeserializerImpl::getArray;
        }
        final li.cil.ceres.api.Serializer serializer = Ceres.getSerializer(componentType);
        return (n, t, i) ->
                serializer.deserialize(new NBTDeserializerImpl((CompoundTag) n), (Class) t, i);
    }

    @Nullable
    private static Object deserializeIntoArray(
            final List<Tag> listTag,
            final int[] nulls,
            final Class<?> componentType,
            final @Nullable Object into,
            final ArrayComponentDeserializer componentDeserializer) {
        if (listTag == null) {
            return into;
        }

        Object[] data = (Object[]) into;
        final int length = listTag.size() + nulls.length;
        if (data == null || data.length != length) {
            data = (Object[]) Array.newInstance(componentType, length);
        }

        int nullsIndex = 0;
        for (int i = 0; i < length; i++) {
            if (nullsIndex < nulls.length && i == nulls[nullsIndex]) {
                nullsIndex++;
                continue;
            }

            final Tag itemTag = listTag.get(i - nullsIndex);
            if (itemTag == null) {
                continue;
            }

            data[i] = componentDeserializer.deserialize(itemTag, componentType, data[i]);
        }

        return data;
    }

    @FunctionalInterface
    private interface ArrayComponentDeserializer {
        @Nullable
        Object deserialize(Tag tag, Class<?> type, @Nullable Object into);
    }
}
