package li.cil.oc2.gametest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

class GameTestStructureTest {
    @Test
    void sharedStructureHasRealBoundsAndAirBlocks() throws Exception {
        try (var input = getClass().getResourceAsStream("/data/oc2r/structure/empty.nbt")) {
            assertNotNull(input);
            final var structure = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            final var size = new ListTag();
            for (int axis = 0; axis < 3; axis++) {
                size.add(IntTag.valueOf(9));
            }
            // A malformed NBT string length made Minecraft read an empty root compound.
            // Its zero-size bounds can reach an unforced neighbour chunk, so GameTests
            // wait forever before their test body or tick timeout can start.
            assertEquals(size, structure.get("size"));
            final var palette = assertInstanceOf(ListTag.class, structure.get("palette"));
            assertEquals(1, palette.size());
            assertEquals(StringTag.valueOf("minecraft:air"),
                    assertInstanceOf(CompoundTag.class, palette.get(0)).get("Name"));
            final var blocks = assertInstanceOf(ListTag.class, structure.get("blocks"));
            assertEquals(9 * 9 * 9, blocks.size());
            final var coordinates = new HashSet<IntTag>();
            for (int coordinate = 0; coordinate < 9; coordinate++) {
                coordinates.add(IntTag.valueOf(coordinate));
            }
            final var positions = new HashSet<ListTag>();
            for (final var entry : blocks) {
                final var block = assertInstanceOf(CompoundTag.class, entry);
                assertEquals(IntTag.valueOf(0), block.get("state"));
                final var pos = assertInstanceOf(ListTag.class, block.get("pos"));
                assertEquals(3, pos.size());
                assertTrue(pos.stream().allMatch(coordinates::contains));
                assertTrue(positions.add(pos), "duplicate air block position: " + pos);
            }
        }
    }
}
