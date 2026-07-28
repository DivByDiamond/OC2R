package li.cil.oc2.common.inet.protocol;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.layer.NetworkLayer;
import li.cil.oc2.common.inet.util.MacAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ArpProtocol {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final short PROTOCOL_ARP = 0x0806;

    private static final short HW_TYPE_ETHERNET = 0x0001;
    static final int ARP_MESSAGE_SIZE = 28;
    private static final int ARP_ADDRESS_TYPE =
            (HW_TYPE_ETHERNET << 16) | NetworkLayer.PROTOCOL_IPv4;
    private static final short ARP_ADDRESSES_SIZES = (6 << 8) | 4;
    private static final short ARP_REQUEST = 0x0001;
    private static final short ARP_RESPONSE = 0x0002;

    public record ArpRequestData(
            short senderMacPrefix,
            int senderMacAddress,
            int senderIpAddress,
            int targetIpAddress) {}

    public static void writeResponse(
            final ByteBuffer frame,
            final MacAddress myMacAddress,
            final int myIpV4Address,
            final short cardMacPrefix,
            final int cardMacAddress,
            final int cardIpAddress,
            final int frameHeaderSize) {
        frame.putInt(ARP_ADDRESS_TYPE);
        frame.putShort(ARP_ADDRESSES_SIZES);
        frame.putShort(ARP_RESPONSE);
        frame.putShort(myMacAddress.prefix());
        frame.putInt(myMacAddress.address());
        frame.putInt(myIpV4Address);
        frame.putShort(cardMacPrefix);
        frame.putInt(cardMacAddress);
        frame.putInt(cardIpAddress);
        frame.position(frame.position() - frameHeaderSize - ARP_MESSAGE_SIZE);
        LOGGER.trace("ARP message sent");
    }

    public static ArpRequestData readRequest(
            final ByteBuffer frame, final short srcMacPrefix, final int srcMacAddress) {
        if (frame.remaining() < ARP_MESSAGE_SIZE) {
            return null;
        }
        final int hwAndProtocolAddressesTypes = frame.getInt();
        if (hwAndProtocolAddressesTypes != ARP_ADDRESS_TYPE) {
            LOGGER.trace("Wrong ARP address type, drop");
            return null;
        }
        final short addressesSizes = frame.getShort();
        if (addressesSizes != ARP_ADDRESSES_SIZES) {
            LOGGER.trace("Wrong ARP address size, drop");
            return null;
        }
        final short messageType = frame.getShort();
        if (messageType != ARP_REQUEST) {
            LOGGER.trace("Not an ARP request, drop");
            return null;
        }
        final short senderMacPrefix = frame.getShort();
        final int senderMacAddress = frame.getInt();
        if (senderMacPrefix != srcMacPrefix || senderMacAddress != srcMacAddress) {
            LOGGER.trace("Wrong sender, drop");
            return null;
        }
        final int senderIpAddress = frame.getInt();
        frame.getShort();
        frame.getInt();
        final int targetIpAddress = frame.getInt();
        return new ArpRequestData(
                senderMacPrefix, senderMacAddress, senderIpAddress, targetIpAddress);
    }
}