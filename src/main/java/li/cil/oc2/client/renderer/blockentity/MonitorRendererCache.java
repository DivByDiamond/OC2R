package li.cil.oc2.client.renderer.blockentity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;

final class MonitorRendererCache {
    private static final Cache<MonitorGUIRenderer, MonitorGUIRenderer.RendererView> rendererViews =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(Duration.ofSeconds(5))
                    .removalListener(MonitorRendererCache::handleNoLongerRendering)
                    .build();

    static MonitorGUIRenderer.RendererView getRendererView(
            final MonitorGUIRenderer terminal,
            final MonitorBlockEntity monitor) {
        try {
            return rendererViews.get(terminal, () -> terminal.getRenderer(monitor));
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    static void cleanUp() {
        rendererViews.cleanUp();
    }

    private static void handleNoLongerRendering(
            final RemovalNotification<MonitorGUIRenderer, MonitorGUIRenderer.RendererView>
                    notification) {
        final MonitorGUIRenderer key = notification.getKey();
        final MonitorGUIRenderer.RendererView value = notification.getValue();
        if (key != null && value != null) {
            key.releaseRenderer(value);
        }
    }
}
