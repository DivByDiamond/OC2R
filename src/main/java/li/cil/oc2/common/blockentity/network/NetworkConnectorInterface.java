package li.cil.oc2.common.blockentity.network;

import li.cil.oc2.api.capabilities.NetworkInterface;

final class NetworkConnectorInterface implements NetworkInterface {
    private static final byte[] NO_FRAME = new byte[0];
    private final NetworkConnectorBlockEntity owner;

    NetworkConnectorInterface(final NetworkConnectorBlockEntity owner) {
        this.owner = owner;
    }

    @Override
    public byte[] readEthernetFrame() {
        return NO_FRAME;
    }

    @Override
    public void writeEthernetFrame(
            final NetworkInterface source, final byte[] frame, final int timeToLive) {
        if (timeToLive <= 0) return;

        final NetworkInterface adjDst = owner.adjacentInterface;
        if (adjDst != null && !adjDst.equals(source)) {
            adjDst.writeEthernetFrame(this, frame, timeToLive - 1);
        }

        for (final NetworkConnectorBlockEntity dst : owner.connectionManager.connectors.values()) {
            if (!dst.isValid() || dst.networkInterface.equals(source)) continue;
            dst.networkInterface.writeEthernetFrame(this, frame, timeToLive - 1);
        }
    }
}