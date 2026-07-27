package li.cil.oc2.common.serialization;

import javax.annotation.Nullable;
import li.cil.ceres.Ceres;
import li.cil.ceres.api.SerializationException;
import li.cil.oc2.common.serialization.nbt.NBTDeserializerImpl;
import li.cil.oc2.common.serialization.nbt.NBTSerializerImpl;
import net.minecraft.nbt.CompoundTag;

/**
 * Utility for serializing/deserializing arbitrary objects to/from NBT using Ceres serialization
 * framework.
 *
 * <p>Supports primitive types, enums, arrays, UUIDs, and complex objects registered with Ceres
 * serializers.
 */
public final class NBTSerialization {
    /**
     * Serialize a value into an existing CompoundTag under Ceres conventions.
     *
     * @param tag the target NBT compound
     * @param value the value to serialize
     * @param type the exact type to serialize as
     */
    public static <T> void serialize(final CompoundTag tag, final T value, final Class<T> type)
            throws SerializationException {
        Ceres.getSerializer(type).serialize(new NBTSerializerImpl(tag), type, value);
    }

    /** Serialize a value, inferring its type from the runtime class. */
    public static <T> void serialize(final CompoundTag tag, final T value)
            throws SerializationException {
        @SuppressWarnings("unchecked")
        final Class<T> type = (Class<T>) value.getClass();
        serialize(tag, value, type);
    }

    /**
     * Serialize a value to a new CompoundTag.
     *
     * @return a new CompoundTag containing the serialized value
     */
    public static <T> CompoundTag serialize(final T value, final Class<T> type)
            throws SerializationException {
        final CompoundTag tag = new CompoundTag();
        serialize(tag, value, type);
        return tag;
    }

    /** Serialize a value to a new CompoundTag, inferring its type. */
    public static <T> CompoundTag serialize(final T value) throws SerializationException {
        final CompoundTag tag = new CompoundTag();
        serialize(tag, value);
        return tag;
    }

    /**
     * Deserialize an object from an existing CompoundTag.
     *
     * @param tag the source NBT compound
     * @param type the target type
     * @param into an optional existing instance to populate (may be null)
     * @return the deserialized object
     */
    public static <T> T deserialize(
            final CompoundTag tag, final Class<T> type, @Nullable final T into)
            throws SerializationException {
        return Ceres.getSerializer(type).deserialize(new NBTDeserializerImpl(tag), type, into);
    }

    /** Deserialize an object, creating a new instance. */
    public static <T> T deserialize(final CompoundTag tag, final Class<T> type)
            throws SerializationException {
        return deserialize(tag, type, null);
    }

    /** Deserialize into an existing object, inferring type from it. */
    public static <T> T deserialize(final CompoundTag tag, final T into)
            throws SerializationException {
        @SuppressWarnings("unchecked")
        final Class<T> type = (Class<T>) into.getClass();
        return deserialize(tag, type, into);
    }
}