package li.cil.oc2.client;

import li.cil.oc2.client.renderer.blockentity.computer.ComputerRenderer;
import li.cil.oc2.client.renderer.blockentity.monitor.MonitorRenderer;
import li.cil.oc2.client.renderer.cable.NetworkCableRenderer;
import li.cil.oc2.client.renderer.projector.ProjectorDepthRenderer;

/**
 * Single place that subscribes client-side listeners to {@link
 * li.cil.oc2.platform.event.ClientEvents} and the client-visible {@link
 * li.cil.oc2.platform.event.CommonEvents}. Called once from mod construction on the client.
 */
public final class ClientEventListeners {
    private ClientEventListeners() {}

    public static void register() {
        ComputerRenderer.registerEvents();
        MonitorRenderer.registerEvents();
        ProjectorDepthRenderer.registerEvents();
        NetworkCableRenderer.registerEvents();
    }
}
