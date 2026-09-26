package li.cil.oc2.common.blockentity.network.switches.port;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketProcessorTest {
    private static byte[] untagged() {
        final byte[] packet = new byte[20];
        for (int i = 0; i < packet.length; i++) {
            packet[i] = (byte) (i + 1);
        }
        return packet;
    }

    @Test
    void vlanIdWithLowByteAbove0x7fRoundtrips() {
        final short[] ids = {1, 0x7f, 0x80, 0xff, 0x100, 0x1ff, 0xfff};
        for (final short id : ids) {
            final byte[] tagged = PacketProcessor.addVLANTag(untagged(), id);
            assertEquals(id, PacketProcessor.getVLAN(tagged), "getVLAN for " + id);
            final var stripped = PacketProcessor.removeVLANTag(tagged);
            assertEquals(id, stripped.getFirst(), "removeVLANTag id for " + id);
            assertArrayEquals(untagged(), stripped.getSecond());
        }
    }

    @Test
    void untaggedPacketHasVlanZero() {
        assertEquals(0, PacketProcessor.getVLAN(untagged()));
    }
}
