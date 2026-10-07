package li.cil.oc2.platform.event;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Client-only game events, independent of the loader. Mirrors {@link CommonEvents}: client code
 * registers listeners here, each loader module fires them from its own hooks.
 *
 * <p>Level render stages, the client tick and the hotbar query live here; geometry loaders,
 * shaders and menu screens are still registered through the loader directly.
 */
public final class ClientEvents {
    /** Start of every client tick. */
    public static final Event<Runnable> CLIENT_TICK_START = new Event<>();

    /** Level rendering reached one of the {@link LevelRenderContext.Stage stages}; fired once per stage. */
    public static final Event<Consumer<LevelRenderContext>> RENDER_LEVEL = new Event<>();

    /** Asked before the hotbar is drawn; if any listener returns {@code true} the hotbar is hidden. */
    public static final Event<BooleanSupplier> HIDE_HOTBAR = new Event<>();

    private ClientEvents() {}
}
