package li.cil.oc2.common.integration;

import li.cil.oc2.common.integration.projectred.BundledCableHandler;
import net.neoforged.fml.ModList;
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

    public static void initialize() {
        final ModList modList = ModList.get();

        createLoaded = modList.isLoaded(CREATE);
        createAeronauticsLoaded = modList.isLoaded(CREATE_AERONAUTICS);
        valkyrienSkiesLoaded = modList.isLoaded(VALKYRIEN_SKIES);
        sableLoaded = modList.isLoaded(SABLE);

        if (createLoaded) {
            LOGGER.info("Create detected — OC2R will treat contraption-hosted computers defensively (no chunk-tracking assumptions).");
        }
        if (createAeronauticsLoaded) {
            LOGGER.info("Create: Aeronautics detected — OC2R computer blocks on ships will boot with their own devices only when the surrounding level is not a ServerLevel.");
        }
        if (valkyrienSkiesLoaded) {
            LOGGER.info("Valkyrien Skies detected — non-ServerLevel ship worlds will be tolerated by the OC2R bus scan and terminal output paths.");
        }
        if (sableLoaded) {
            LOGGER.info("Sable detected — OC2R will not assume its peripheral blocks expose standard capabilities.");
        }

        if (modList.isLoaded("projectred_transmission")) {
            BundledCableHandler.initialize();
        }
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
