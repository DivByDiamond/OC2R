package li.cil.oc2.common.blockentity.network.connector.interfaces;

import li.cil.oc2.api.capabilities.NetworkInterface;

public final class NullNetworkInterface implements NetworkInterface {
    public static final NetworkInterface INSTANCE = new NullNetworkInterface();

    private static final byte[] NO_FRAME = new byte[0];

    @Override
    public byte[] readEthernetFrame() {
        return NO_FRAME;
    }

    @Override
    public void writeEthernetFrame(
            final NetworkInterface source, final byte[] frame, final int timeToLive) {}
}