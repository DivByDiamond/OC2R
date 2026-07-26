package li.cil.oc2.common.serialization.nbt;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import li.cil.ceres.Ceres;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record NBTSerializerImpl(CompoundTag tag) implements SerializationVisitor {
    private static final String IS_NULL_KEY = "<is_null>";

    @Override
    public void putBoolean(final String name, final boolean value) {
        tag.putBoolean(name, value);
    }

    @Override
    public void putByte(final String name, final byte value) {
        tag.putByte(name, value);
    }

    @Override
    public void putChar(final String name, final char value) {
        tag.putInt(name, value);
    }

    @Override
    public void putShort(final String name, final short value) {
        tag.putShort(name, value);
    }

    @Override
    public void putInt(final String name, final int value) {
        tag.putInt(name, value);
    }

    @Override
    public void putLong(final String name, final long value) {
        tag.putLong(name, value);
    }

    @Override
    public void putFloat(final String name, final float value) {
        tag.putFloat(name, value);
    }

    @Override
    public void putDouble(final String name, final double value) {
        tag.putDouble(name, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void putObject(final String name, final Class<?> type, @Nullable final Object value) throws SerializationException {
        if (putIsNull(name, value)) {
            return;
        }

        if (type.isArray()) {
            tag.put(name, putArray(name, type, value));
        } else if (type.isEnum()) {
            tag.putString(name, ((Enum) value).name());
        } else if (type == String.class) {
            tag.putString(name, (String) value);
        } else if (type == UUID.class) {
            final CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID(name, (UUID) value);
            tag.put(name, uuidTag);
        } else {
            final CompoundTag valueTag = new CompoundTag();
            Ceres.getSerializer(type).serialize(new NBTSerializerImpl(valueTag), (Class) type, value);
            if (!valueTag.isEmpty()) {
                tag.put(name, valueTag);
            }
        }
    }

    @FunctionalInterface
    private interface ArrayComponentSerializer {
        Tag serialize(Class<?> type, Object value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Tag putArray(final String name, final Class<?> type, final Object value) {
        final Class<?> componentType = type.getComponentType();

        final NBTArraySerializer arraySerializer = NBTDeserializerImpl.ARRAY_SERIALIZERS.get(componentType);
        if (arraySerializer != null) {
            return arraySerializer.serialize(value);
        } else {
            final ArrayComponentSerializer componentSerializer;
            if (componentType.isArray()) {
                componentSerializer = (t, v) -> putArray(name, t, v);
            } else {
                final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
                componentSerializer = (t, v) -> {
                    final CompoundTag innerTag = new CompoundTag();
                    serializer.serialize(new NBTSerializerImpl(innerTag), (Class) t, v);
                    return innerTag;
                };
            }

            final ListTag listTag = new ListTag();
            final IntArrayList nullIndices = new IntArrayList();

            final Object[] data = (Object[]) value;
            for (int i = 0; i < data.length; i++) {
                final Object datum = data[i];
                if (datum == null) {
                    nullIndices.add(i);
                } else {
                    if (datum.getClass() != componentType) {
                        throw new SerializationException(String.format("Polymorphism detected in generic array [%s]. This is not supported.", name));
                    }
                    listTag.add(componentSerializer.serialize(componentType, datum));
                }
            }

            if (nullIndices.isEmpty()) {
                return listTag;
            } else {
                final CompoundTag arrayTag = new CompoundTag();
                arrayTag.put("value", listTag);
                arrayTag.putIntArray("nulls", nullIndices);
                return arrayTag;
            }
        }
    }

    @Contract(value = "_, null -> true")
    private boolean putIsNull(final String name, @Nullable final Object value) {
        final boolean isNull = value == null;
        if (isNull) {
            final CompoundTag nullTag = new CompoundTag();
            nullTag.putBoolean(IS_NULL_KEY, true);
            tag.put(name, nullTag);
        }
        return isNull;
    }
}
