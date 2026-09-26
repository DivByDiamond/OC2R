package li.cil.oc2.platform;

import java.util.ServiceLoader;

/** Locates the loader-specific implementations of the bridge interfaces in this package. */
public final class Platform {
    private static RegistryBridge registries;

    private Platform() {}

    public static synchronized RegistryBridge registries() {
        if (registries == null) {
            registries = load(RegistryBridge.class);
        }
        return registries;
    }

    static <T> T load(final Class<T> type) {
        return ServiceLoader.load(type, type.getClassLoader()).findFirst().orElseThrow(() ->
                new IllegalStateException("No " + type.getName() + " implementation found; "
                        + "the loader module must provide one via META-INF/services"));
    }
}
