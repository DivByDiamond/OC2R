package li.cil.oc2.common.blockentity.network.switches.port;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.nbt.*;

public final class PortSettings {
    public short untagged;
    public final List<Short> tagged;
    public final boolean hairpin;
    public final boolean trunkAll;

    public PortSettings(
            final short untagged,
            final List<Short> tagged,
            final boolean hairpin,
            final boolean trunkAll) {
        this.untagged = untagged;
        this.tagged = tagged;
        this.hairpin = hairpin;
        this.trunkAll = trunkAll;
    }

    public PortSettings() {
        this((short) 0, emptyList(), false, true);
    }

    public void save(final CompoundTag tag) {
        tag.put("untagged", ShortTag.valueOf(untagged));
        tag.put(
                "tagged",
                //? if >=26.1 {
                /*new IntArrayTag(tagged.stream().mapToInt(s -> (int) s).toArray()));
                *///?} else {
                new IntArrayTag(tagged.stream().map(s -> (int) s).collect(Collectors.toList())));
                //?}
        tag.put("hairpin", ByteTag.valueOf(hairpin));
        tag.put("trunkAll", ByteTag.valueOf(trunkAll));
    }

    public static PortSettings load(final CompoundTag tag) {
        //? if >=26.1 {
        /*short untagged = tag.getShortOr("untagged", (short) 0);
        *///?} else {
        short untagged = tag.getShort("untagged");
        //?}
        List<Short> tagged =
                //? if >=26.1 {
                /*Arrays.stream(tag.getIntArray("tagged").orElse(new int[0]))
                *///?} else {
                Arrays.stream(tag.getIntArray("tagged"))
                //?}
                        .mapToObj(i -> (short) i)
                        .collect(Collectors.toList());
        //? if >=26.1 {
        /*boolean hairpin = tag.getBooleanOr("hairpin", false);
        boolean trunkAll = tag.getBooleanOr("trunkAll", false);
        *///?} else {
        boolean hairpin = tag.getBoolean("hairpin");
        boolean trunkAll = tag.getBoolean("trunkAll");
        //?}
        return new PortSettings(untagged, tagged, hairpin, trunkAll);
    }
}