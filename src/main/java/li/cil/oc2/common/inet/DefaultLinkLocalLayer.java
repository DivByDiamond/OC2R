package li.cil.oc2.common.inet;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;
import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.layer.LinkLocalLayer;
import li.cil.oc2.api.inet.layer.NetworkLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DefaultLinkLocalLayer implements LinkLocalLayer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random random = new Random();

    private static final short MAC_PREFIX = 0x5ed1;

    private static final int IP_VER4 = 4;
    private static final int IP_VER6 = 6;

    private static final String MAC_ADDRESS_TAG = "MACAddress";
    private static final String IPv4_ADDRESS_TAG = "IPv4Address";

    private final NetworkLayer networkLayer;

    private MacAddress myMacAddress = new MacAddress(MAC_PREFIX, random.nextInt());
    private int myIpV4Address = -1;

    private short cardMacPrefix = -1;
    private int cardMacAddress = -1;
    private int cardIpAddress = -1;

    private boolean needArpResponse = false;

    public DefaultLinkLocalLayer(
            final LayerParameters layerParameters, final NetworkLayer networkLayer) {
        layerParameters
                .getSavedState()
                .ifPresent(
                        tag -> {
                            if (tag instanceof CompoundTag layerState) {
                                final String ipAddressString =
                                        layerState.getString(IPv4_ADDRESS_TAG);
                                if (!ipAddressString.isEmpty()) {
                                    try {
                                        myIpV4Address = InetUtils.parseIpv4Address(ipAddressString);
                                    } catch (final AddressParseException exception) {
                                        LOGGER.error(
                                                "Failed to parse internet adapter IPv4 address",
                                                exception);
                                    }
                                }
                                final String macAddressString =
                                        layerState.getString(MAC_ADDRESS_TAG);
                                if (!macAddressString.isEmpty()) {
                                    try {
                                        myMacAddress =
                                                MacAddressUtils.parseMacAddress(macAddressString);
                                    } catch (final AddressParseException exception) {
                                        LOGGER.error(
                                                "Failed to parse internet adapter MAC address from"
                                                        + " NBT",
                                                exception);
                                    }
                                }
                            }
                        });
        this.networkLayer = networkLayer;
    }

    private void prepareEthernetHeader(final ByteBuffer frame, final short protocol) {
        frame.putShort(cardMacPrefix);
        frame.putInt(cardMacAddress);
        frame.putShort(myMacAddress.prefix());
        frame.putInt(myMacAddress.address());
        frame.putShort(protocol);
    }

    @Override
    public Optional<Tag> onSave() {
        final CompoundTag layerState = new CompoundTag();
        if (myIpV4Address != -1) {
            final String ipAddressString = InetUtils.ipv4AddressToString(myIpV4Address);
            layerState.putString(IPv4_ADDRESS_TAG, ipAddressString);
        }
        layerState.putString(MAC_ADDRESS_TAG, MacAddressUtils.macAddressToString(myMacAddress));
        networkLayer
                .onSave()
                .ifPresent(
                        networkLayerState ->
                                layerState.put(NetworkLayer.LAYER_NAME, networkLayerState));
        return Optional.of(layerState);
    }

    @Override
    public void onStop() {
        networkLayer.onStop();
    }

    @Override
    public boolean receiveEthernetFrame(final ByteBuffer frame) {
        if (needArpResponse) {
            needArpResponse = false;
            prepareEthernetHeader(frame, ArpProtocol.PROTOCOL_ARP);
            ArpProtocol.writeResponse(
                    frame,
                    myMacAddress,
                    myIpV4Address,
                    cardMacPrefix,
                    cardMacAddress,
                    cardIpAddress,
                    FRAME_HEADER_SIZE);
        } else {
            frame.position(frame.position() + FRAME_HEADER_SIZE);
            short protocol = networkLayer.receivePacket(frame);
            if (protocol == NetworkLayer.PROTOCOL_NONE) {
                return false;
            }
            if (protocol == NetworkLayer.PROTOCOL_IP) {
                final int version = Byte.toUnsignedInt(frame.get(frame.position())) >>> 4;
                if (version == IP_VER6) {
                    protocol = NetworkLayer.PROTOCOL_IPv6;
                }
            }
            frame.position(frame.position() - FRAME_HEADER_SIZE);
            prepareEthernetHeader(frame, protocol);
            frame.position(frame.position() - FRAME_HEADER_SIZE);
            LOGGER.trace("IP message sent");
        }
        return true;
    }

    @Override
    public void sendEthernetFrame(final ByteBuffer frame) {
        if (frame.remaining() < FRAME_HEADER_SIZE) {
            LOGGER.trace("Ethernet header too low");
            return;
        }
        final short dstMacPrefix = frame.getShort();
        final int dstMacAddress = frame.getInt();
        final short srcMacPrefix = frame.getShort();
        final int srcMacAddress = frame.getInt();
        final short protocol = frame.getShort();

        if (protocol == ArpProtocol.PROTOCOL_ARP) {
            LOGGER.trace("ARP message received");
            final var arpData = ArpProtocol.readRequest(frame, srcMacPrefix, srcMacAddress);
            if (arpData != null) {
                cardIpAddress = arpData.senderIpAddress();
                myIpV4Address = arpData.targetIpAddress();
                cardMacPrefix = arpData.senderMacPrefix();
                cardMacAddress = arpData.senderMacAddress();
                needArpResponse = true;
            }
        } else {
            LOGGER.trace("Network message received");
            networkLayer.sendPacket(protocol, frame);
        }
    }
}