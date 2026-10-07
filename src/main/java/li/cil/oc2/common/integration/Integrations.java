package li.cil.oc2.common.integration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.platform.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Integrations {
    private static final Logger LOGGER = LogManager.getLogger();

    // Mod IDs we soft-depend on but never link against at compile time.
    // Presence is checked at runtime via ModList so we don't pull their
    // classes onto the classpath. This lets OC2R computers coexist with
    // Create: Aeronautics airships and Sable peripherals without ever
    // requiring those mods to be installed.
    public static final String CREATE = "create";
    public static final String CREATE_AERONAUTICS = "create_aeronautics";
    public static final String VALKYRIEN_SKIES = "valkyrienskies";
    public static final String SABLE = "sable";

    private static boolean createLoaded;
    private static boolean createAeronauticsLoaded;
    private static boolean valkyrienSkiesLoaded;
    private static boolean sableLoaded;

    // Loader specific integrations that link against the other mod; each runs only if that mod is present.
    private static final Map<String, Runnable> MOD_INTEGRATIONS = new ConcurrentHashMap<>();

    /**
     * Registers an integration to run during {@link #initialize()} if {@code modId} is loaded. The runnable
     * must reference the integration classes lazily (inside a lambda body), they may be missing otherwise.
     */
    public static void registerModIntegration(final String modId, final Runnable integration) {
        MOD_INTEGRATIONS.put(modId, integration);
    }

    public static void initialize() {
        createLoaded = Platform.environment().isModLoaded(CREATE);
        createAeronauticsLoaded = Platform.environment().isModLoaded(CREATE_AERONAUTICS);
        valkyrienSkiesLoaded = Platform.environment().isModLoaded(VALKYRIEN_SKIES);
        sableLoaded = Platform.environment().isModLoaded(SABLE);

        if (createLoaded) {
            LOGGER.info(
                    "Create detected — OC2R will treat contraption-hosted computers defensively (no"
                            + " chunk-tracking assumptions).");
        }
        if (createAeronauticsLoaded) {
            LOGGER.info(
                    "Create: Aeronautics detected — OC2R computer blocks on ships will boot with"
                            + " their own devices only when the surrounding level is not a"
                            + " ServerLevel.");
        }
        if (valkyrienSkiesLoaded) {
            LOGGER.info(
                    "Valkyrien Skies detected — non-ServerLevel ship worlds will be tolerated by"
                            + " the OC2R bus scan and terminal output paths.");
        }
        if (sableLoaded) {
            LOGGER.info(
                    "Sable detected — OC2R will not assume its peripheral blocks expose standard"
                            + " capabilities.");
        }

        MOD_INTEGRATIONS.forEach((modId, integration) -> {
            if (Platform.environment().isModLoaded(modId)) {
                integration.run();
            }
        });
    }

    public static boolean isCreateLoaded() {
        return createLoaded;
    }

    public static boolean isCreateAeronauticsLoaded() {
        return createAeronauticsLoaded;
    }

    public static boolean isValkyrienSkiesLoaded() {
        return valkyrienSkiesLoaded;
    }

    public static boolean isSableLoaded() {
        return sableLoaded;
    }
}