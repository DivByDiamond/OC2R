package li.cil.oc2.common.vm.terminal;

import java.util.concurrent.locks.ReentrantLock;
import li.cil.oc2.common.vm.terminal.render.RendererView;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Lazily-created client render/tick handle for a {@link Terminal}. Extracted from
 * {@link Terminal} (А1). The live renderer set stays on {@link Terminal#renderers} — it is
 * read/written directly by {@link TerminalClient} and by tests as {@code terminal.renderers}.
 */
final class TerminalRenderState {
    private final Terminal terminal;
    private volatile TerminalClient clientInstance;
    private final ReentrantLock clientLock = new ReentrantLock();

    TerminalRenderState(final Terminal terminal) {
        this.terminal = terminal;
    }

    @OnlyIn(Dist.CLIENT)
    RendererView getRenderer() {
        return client().getRenderer();
    }

    @OnlyIn(Dist.CLIENT)
    void setDisplayOnly(final boolean value) {
        client().setDisplayOnly(value);
    }

    @OnlyIn(Dist.CLIENT)
    void releaseRenderer(final RendererView renderer) {
        client().releaseRenderer(renderer);
    }

    @OnlyIn(Dist.CLIENT)
    void clientTick() {
        client().clientTick();
    }

    private TerminalClient client() {
        TerminalClient result = clientInstance;
        if (result == null) {
            clientLock.lock();
            try {
                result = clientInstance;
                if (result == null) {
                    result = new TerminalClient(terminal);
                    clientInstance = result;
                }
            } finally {
                clientLock.unlock();
            }
        }
        return result;
    }
}
