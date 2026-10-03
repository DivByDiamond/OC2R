package li.cil.oc2.platform;

import java.util.ServiceLoader;

/** Locates the loader-specific implementations of the bridge interfaces in this package. */
public final class Platform {
    private static final Object INIT_LOCK = new Object();

    private static final Object NETWORK_INIT_LOCK = new Object();
    private static final Object ENERGY_INIT_LOCK = new Object();
    private static final Object CAPABILITIES_INIT_LOCK = new Object();
    private static final Object ENVIRONMENT_INIT_LOCK = new Object();
    private static final Object MENU_INIT_LOCK = new Object();

    private static volatile RegistryBridge bridge;
    private static volatile NetworkBridge networkBridge;
    private static volatile EnergyBridge energyBridge;
    private static volatile CapabilityBridge capabilityBridge;
    private static volatile PlatformEnvironment environmentBridge;
    private static volatile MenuBridge menuBridge;

    private Platform() {}

    // The bridge implementation is a stateless singleton handed to all callers; nothing escapes
    // through it that callers could mutate. SpotBugs does not flag MS_EXPOSE_REP on this method
    // (the field is accessed through a static accessor, not exposed directly), so no
    // suppression is needed here.
    @SuppressWarnings("PMD.AvoidSynchronizedStatement") // double-checked init lock, not a hot path
    public static RegistryBridge registries() {
        RegistryBridge result = bridge;
        if (result == null) {
            synchronized (INIT_LOCK) {
                result = bridge;
                if (result == null) {
                    bridge = result = load(RegistryBridge.class);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement") // double-checked init lock, not a hot path
    public static NetworkBridge network() {
        NetworkBridge result = networkBridge;
        if (result == null) {
            synchronized (NETWORK_INIT_LOCK) {
                result = networkBridge;
                if (result == null) {
                    networkBridge = result = load(NetworkBridge.class);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement") // double-checked init lock, not a hot path
    public static EnergyBridge energy() {
        EnergyBridge result = energyBridge;
        if (result == null) {
            synchronized (ENERGY_INIT_LOCK) {
                result = energyBridge;
                if (result == null) {
                    energyBridge = result = load(EnergyBridge.class);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement") // double-checked init lock, not a hot path
    public static CapabilityBridge capabilities() {
        CapabilityBridge result = capabilityBridge;
        if (result == null) {
            synchronized (CAPABILITIES_INIT_LOCK) {
                result = capabilityBridge;
                if (result == null) {
                    capabilityBridge = result = load(CapabilityBridge.class);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement") // double-checked init lock, not a hot path
    public static PlatformEnvironment environment() {
        PlatformEnvironment result = environmentBridge;
        if (result == null) {
            synchronized (ENVIRONMENT_INIT_LOCK) {
                result = environmentBridge;
                if (result == null) {
                    environmentBridge = result = load(PlatformEnvironment.class);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement") // double-checked init lock, not a hot path
    public static MenuBridge menus() {
        MenuBridge result = menuBridge;
        if (result == null) {
            synchronized (MENU_INIT_LOCK) {
                result = menuBridge;
                if (result == null) {
                    menuBridge = result = load(MenuBridge.class);
                }
            }
        }
        return result;
    }

    // The implementation class ships in this very jar, so the interface's own class loader always
    // sees it; the thread-context loader points at the loader module under FML and would miss it.
    @SuppressWarnings("PMD.UseProperClassLoader")
    static <T> T load(final Class<T> type) {
        return ServiceLoader.load(type, type.getClassLoader()).findFirst().orElseThrow(() ->
                new IllegalStateException("No " + type.getName() + " implementation found; "
                        + "the loader module must provide one via META-INF/services"));
    }
}
