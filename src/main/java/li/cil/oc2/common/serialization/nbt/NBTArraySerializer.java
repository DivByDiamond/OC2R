package li.cil.oc2.common.serialization.nbt;

import net.minecraft.nbt.Tag;

interface NBTArraySerializer {
    Tag serialize(Object value);

    Object deserialize(Tag tag, Class<?> type, Object into);
}