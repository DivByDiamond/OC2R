package li.cil.oc2.common.blockentity.network;

import static java.util.Collections.emptyList;

import net.minecraft.nbt.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

final class PortSettings {
    public short untagged;
    public final List<Short> tagged;
    public final boolean hairpin;
    public final boolean trunkAll;

    PortSettings(
            final short untagged,
            final List<Short> tagged,
            final boolean hairpin,
            final boolean trunkAll) {
        this.untagged = untagged;
        this.tagged = tagged;
        this.hairpin = hairpin;
        this.trunkAll = trunkAll;
    }

    PortSettings() {
        this((short) 0, emptyList(), false, true);
    }

    void save(final CompoundTag tag) {
        tag.put("untagged", ShortTag.valueOf(untagged));
        tag.put(
                "tagged",
                new IntArrayTag(tagged.stream().map(s -> (int) s).collect(Collectors.toList())));
        tag.put("hairpin", ByteTag.valueOf(hairpin));
        tag.put("trunkAll", ByteTag.valueOf(trunkAll));
    }

    static PortSettings load(final CompoundTag tag) {
        short untagged = tag.getShort("untagged");
        List<Short> tagged =
                Arrays.stream(tag.getIntArray("tagged"))
                        .mapToObj(i -> (short) i)
                        .collect(Collectors.toList());
        boolean hairpin = tag.getBoolean("hairpin");
        boolean trunkAll = tag.getBoolean("trunkAll");
        return new PortSettings(untagged, tagged, hairpin, trunkAll);
    }
}
