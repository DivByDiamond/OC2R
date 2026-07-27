package li.cil.oc2.common.blockentity.network;

import li.cil.oc2.api.capabilities.NetworkInterface;

final class NullNetworkInterface implements NetworkInterface {
    static final NetworkInterface INSTANCE = new NullNetworkInterface();

    @Override
    public byte[] readEthernetFrame() {
        return null;
    }

    @Override
    public void writeEthernetFrame(
            final NetworkInterface source, final byte[] frame, final int timeToLive) {}
}
