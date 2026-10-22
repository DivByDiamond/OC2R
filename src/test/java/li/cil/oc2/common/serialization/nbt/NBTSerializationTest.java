package li.cil.oc2.common.serialization.nbt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.serialization.nbt.util.NBTSerialization;
import org.junit.jupiter.api.Test;

class NBTSerializationTest {
    @Test
    void ceresRoundtripRestoresStringsEnumsUuidsAndArrays() {
        final Snapshot source = snapshot();
        assertSnapshot(source, NBTSerialization.deserialize(NBTSerialization.serialize(source), Snapshot.class));
    }

    @Test
    void ceresRoundtripReusesExistingObjectAndReferenceArrays() {
        final Snapshot source = snapshot();
        final Snapshot into = new Snapshot();
        into.label = "stale";
        into.mode = Mode.IDLE;
        into.names = new String[] {"old", "old"};
        into.ids = new UUID[] {new UUID(0, 0), new UUID(0, 0)};
        final String[] previousNames = into.names;
        final UUID[] previousIds = into.ids;

        final Snapshot result = NBTSerialization.deserialize(NBTSerialization.serialize(source), into);
        assertSame(into, result);
        assertSame(previousNames, result.names);
        assertSame(previousIds, result.ids);
        assertSnapshot(source, result);
    }

    @Test
    void explicitNullClearsReusedStringWhileMissingFieldKeepsIt() {
        final Snapshot source = snapshot();
        source.label = null;
        final Snapshot into = snapshot();
        NBTSerialization.deserialize(NBTSerialization.serialize(source), into);
        assertNull(into.label);

        final var tag = NBTSerialization.serialize(snapshot());
        tag.remove("label");
        into.label = "retained";
        NBTSerialization.deserialize(tag, into);
        assertEquals("retained", into.label);
    }

    private static Snapshot snapshot() {
        final Snapshot result = new Snapshot();
        result.label = "console=ttyS0 root=/dev/vda rw";
        result.mode = Mode.RUNNING;
        result.id = UUID.fromString("12345678-1234-5678-9abc-123456789abc");
        result.names = new String[] {"", "framebuffer/монитор"};
        result.ids = new UUID[] {result.id, new UUID(1234, 5678)};
        result.values = new int[] {0, -1, Integer.MAX_VALUE};
        return result;
    }

    private static void assertSnapshot(final Snapshot expected, final Snapshot actual) {
        assertEquals(expected.label, actual.label);
        assertEquals(expected.mode, actual.mode);
        assertEquals(expected.id, actual.id);
        assertArrayEquals(expected.names, actual.names);
        assertArrayEquals(expected.ids, actual.ids);
        assertArrayEquals(expected.values, actual.values);
    }

    public enum Mode { IDLE, RUNNING }

    public static final class Snapshot {
        @Serialized public String label;
        @Serialized public Mode mode;
        @Serialized public UUID id;
        @Serialized public String[] names;
        @Serialized public UUID[] ids;
        @Serialized public int[] values;
    }
}
