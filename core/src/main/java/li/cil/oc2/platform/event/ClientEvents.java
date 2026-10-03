package li.cil.oc2.platform.event;

/**
 * Client-only game events, independent of the loader. Mirrors {@link CommonEvents}: client code
 * registers listeners here, each loader module fires them from its own hooks.
 *
 * <p>Only lifecycle events live here so far; the render-pipeline hooks (level render stages, GUI
 * layers, shader/model/color registration) are still subscribed through the loader directly.
 */
public final class ClientEvents {
    /** Start of every client tick. */
    public static final Event<Runnable> CLIENT_TICK_START = new Event<>();

    private ClientEvents() {}
}
