package li.cil.oc2.common.serialization.nbt.util;

import com.google.gson.*;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import net.minecraft.nbt.*;

public final class NBTToJsonConverter {
    private static final Map<Integer, Function<Tag, JsonElement>> CONVERTERS =
            Map.ofEntries(
                    Map.entry(NBTTagIds.TAG_BYTE, NBTToJsonConverter::convertByte),
                    Map.entry(NBTTagIds.TAG_SHORT, NBTToJsonConverter::convertShort),
                    Map.entry(NBTTagIds.TAG_INT, NBTToJsonConverter::convertInt),
                    Map.entry(NBTTagIds.TAG_LONG, NBTToJsonConverter::convertLong),
                    Map.entry(NBTTagIds.TAG_FLOAT, NBTToJsonConverter::convertFloat),
                    Map.entry(NBTTagIds.TAG_DOUBLE, NBTToJsonConverter::convertDouble),
                    Map.entry(NBTTagIds.TAG_BYTE_ARRAY, NBTToJsonConverter::convertByteArray),
                    Map.entry(NBTTagIds.TAG_STRING, NBTToJsonConverter::convertString),
                    Map.entry(NBTTagIds.TAG_LIST, NBTToJsonConverter::convertList),
                    Map.entry(NBTTagIds.TAG_COMPOUND, NBTToJsonConverter::convertCompound),
                    Map.entry(NBTTagIds.TAG_INT_ARRAY, NBTToJsonConverter::convertIntArray),
                    Map.entry(NBTTagIds.TAG_LONG_ARRAY, NBTToJsonConverter::convertLongArray));

    public static JsonElement convert(@Nullable final Tag tag) {
        if (tag == null) {
            return JsonNull.INSTANCE;
        }
        final Function<Tag, JsonElement> converter = CONVERTERS.get((int) tag.getId());
        return converter != null ? converter.apply(tag) : JsonNull.INSTANCE;
    }

    private static JsonElement convertByte(final Tag tag) {
        return new JsonPrimitive(((ByteTag) tag).getAsByte());
    }

    private static JsonElement convertShort(final Tag tag) {
        return new JsonPrimitive(((ShortTag) tag).getAsShort());
    }

    private static JsonElement convertInt(final Tag tag) {
        return new JsonPrimitive(((IntTag) tag).getAsInt());
    }

    private static JsonElement convertLong(final Tag tag) {
        return new JsonPrimitive(((LongTag) tag).getAsLong());
    }

    private static JsonElement convertFloat(final Tag tag) {
        return new JsonPrimitive(((FloatTag) tag).getAsFloat());
    }

    private static JsonElement convertDouble(final Tag tag) {
        return new JsonPrimitive(((DoubleTag) tag).getAsDouble());
    }

    private static JsonElement convertString(final Tag tag) {
        return new JsonPrimitive(tag.getAsString());
    }

    private static JsonElement convertByteArray(final Tag tag) {
        final JsonArray json = new JsonArray();
        for (final byte b : ((ByteArrayTag) tag).getAsByteArray()) {
            json.add(b);
        }
        return json;
    }

    private static JsonElement convertList(final Tag tag) {
        final JsonArray json = new JsonArray();
        for (final Tag item : (ListTag) tag) {
            json.add(convert(item));
        }
        return json;
    }

    private static JsonElement convertCompound(final Tag tag) {
        final JsonObject json = new JsonObject();
        final CompoundTag compoundTag = (CompoundTag) tag;
        for (final String key : compoundTag.getAllKeys()) {
            json.add(key, convert(compoundTag.get(key)));
        }
        return json;
    }

    private static JsonElement convertIntArray(final Tag tag) {
        final JsonArray json = new JsonArray();
        for (final int j : ((IntArrayTag) tag).getAsIntArray()) {
            json.add(j);
        }
        return json;
    }

    private static JsonElement convertLongArray(final Tag tag) {
        final JsonArray json = new JsonArray();
        for (final long l : ((LongArrayTag) tag).getAsLongArray()) {
            json.add(l);
        }
        return json;
    }
}