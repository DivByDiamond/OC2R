package li.cil.oc2.platform.event;

import java.util.function.Consumer;

/**
 * Client-only game events, independent of the loader. Mirrors {@link CommonEvents}: client code
 * registers listeners here, each loader module fires them from its own hooks.
 *
 * <p>Level render stages and the client tick live here; GUI layers and shader/model/color
 * registration are still subscribed through the loader directly.
 */
public final class ClientEvents {
    /** Start of every client tick. */
    public static final Event<Runnable> CLIENT_TICK_START = new Event<>();

    /** Level rendering reached one of the {@link LevelRenderContext.Stage stages}; fired once per stage. */
    public static final Event<Consumer<LevelRenderContext>> RENDER_LEVEL = new Event<>();

    private ClientEvents() {}
}
