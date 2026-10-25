package li.cil.oc2.common.serialization.nbt;

//? if >=26.1 {
/*import static org.junit.jupiter.api.Assertions.assertEquals;

import li.cil.oc2.common.util.nbt.NBTUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.junit.jupiter.api.Test;

// The vanilla ValueInput bridge preserves the raw NBT used by existing VM snapshots.
class NBTValueIoTest {
    @Test
    void wholeMapBridgePreservesEveryNbtTypeAndNestedData() {
        final CompoundTag source = new CompoundTag();
        source.putByte("byte", (byte) -12);
        source.putShort("short", (short) 456);
        source.putInt("int", Integer.MIN_VALUE);
        source.putLong("long", Long.MAX_VALUE);
        source.putFloat("float", 0.125f);
        source.putDouble("double", Math.PI);
        source.putString("string", "VM snapshot / память");
        source.putByteArray("bytes", new byte[] {-1, 0, 1});
        source.putIntArray("ints", new int[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE});
        source.putLongArray("longs", new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE});
        final ListTag list = new ListTag();
        list.add(StringTag.valueOf("first"));
        list.add(StringTag.valueOf("second"));
        source.put("list", list);
        final CompoundTag child = new CompoundTag();
        child.put("list", list.copy());
        source.put("nested", child);
        source.put("empty", new CompoundTag());

        final var input = TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, source);
        final CompoundTag decoded = NBTUtils.toTag(input);
        assertEquals(source, decoded);
        final TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        output.store(NBTUtils.TAG_MAP_CODEC, decoded);
        assertEquals(source, output.buildResult());
    }
}
*///?}
