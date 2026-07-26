package li.cil.oc2.common.blockentity.network;

import com.mojang.datafixers.util.Pair;

final class PacketProcessor {
    static long macToLong(final byte[] mac, int offset) {
        long ret = 0;
        for (int i = 0; i < 6; i++) {
            ret |= ((((long) mac[i + offset]) & 0xff) << (i * 8));
        }
        return ret;
    }

    static String macLongToString(long mac) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i != 0) {
                ret.append(":");
            }
            ret.append(String.format("%02x", (mac >> (i * 8)) & 0xff));
        }
        return ret.toString();
    }

    static short getVLAN(byte[] packet) {
        if (packet[12] == ((byte) 0x81) && packet[13] == 0x00) {
            return (short) (packet[15] | ((((short) packet[14]) & 0x0f) << 8));
        } else {
            return (short) 0;
        }
    }

    static byte[] addVLANTag(byte[] packet, short tag) {
        if (tag != 0) {
            byte[] ret = new byte[packet.length + 4];
            copyBytes(packet, ret, 0, 0, 12);
            copyBytes(packet, ret, 12, 16, packet.length - 12);
            ret[12] = (byte) 0x81;
            ret[13] = (byte) 0x00;
            ret[14] = (byte) ((tag >> 8) & 0x0f);
            ret[15] = (byte) (tag & 0xff);
            return ret;
        } else {
            return packet;
        }
    }

    static Pair<Short, byte[]> removeVLANTag(byte[] packet) {
        if (packet[12] == ((byte) 0x81) && packet[13] == 0x00) {
            byte[] ret = new byte[packet.length - 4];
            copyBytes(packet, ret, 0, 0, 12);
            copyBytes(packet, ret, 16, 12, packet.length - 16);
            short tag = (short) (packet[15] | ((((short) packet[14]) & 0x0f) << 8));
            return new Pair<>(tag, ret);
        } else {
            return new Pair<>((short) 0, packet);
        }
    }

    static void copyBytes(byte[] input, byte[] output, int inputOffset, int outputOffset, int length) {
        if (length >= 0) System.arraycopy(input, inputOffset, output, outputOffset, length);
    }
}
