package li.cil.oc2.client.hooks;

import li.cil.oc2.client.renderer.cable.NetworkCableRenderer;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class NetworkCableRendererHooks {
    private NetworkCableRendererHooks() {}

    public static void addNetworkConnector(final NetworkConnectorBlockEntity entity) {
        NetworkCableRenderer.addNetworkConnector(entity);
    }

    public static void invalidateConnections() {
        NetworkCableRenderer.invalidateConnections();
    }
}