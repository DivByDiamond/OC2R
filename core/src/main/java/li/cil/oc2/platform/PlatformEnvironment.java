package li.cil.oc2.platform;

import java.nio.file.Path;

/**
 * Loader-independent facts about the running game: which side it is, which mods are loaded and
 * where the config directory is. A stateless singleton discovered through
 * {@link java.util.ServiceLoader} via {@link Platform#environment()}.
 */
public interface PlatformEnvironment {
    /** Whether this is the physical client (as opposed to a dedicated server). */
    boolean isClient();

    /** Whether a mod with {@code modId} is loaded. */
    boolean isModLoaded(String modId);

    /** The directory mod config files live in. */
    Path configDir();
}
