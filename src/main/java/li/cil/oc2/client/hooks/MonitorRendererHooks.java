package li.cil.oc2.client.hooks;

import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class MonitorRendererHooks {
    private MonitorRendererHooks() {}

    public static MonitorGUIRenderer createMonitorGUIRenderer() {
        return new MonitorGUIRenderer();
    }

    public static Object getMonitorRenderer(final MonitorBlockEntity monitor) {
        return monitor.stateManager.getMonitor();
    }
}