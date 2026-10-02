package li.cil.oc2.platform;

import net.neoforged.neoforge.items.IItemHandler;

/** Adapts NeoForge item handlers owned by loader-bound code (menus, robots) to the core type. */
public final class NeoForgeItemHandlers {
    private NeoForgeItemHandlers() {}

    /** A core {@link ItemHandler} view over {@code handler}, equal to other views of it. */
    public static ItemHandler adapt(final IItemHandler handler) {
        if (handler instanceof final NeoForgeCapabilities.ItemHandlerWrapper wrapper) {
            return wrapper.delegate();
        }
        return new NeoForgeCapabilities.ItemHandlerView(handler);
    }
}
