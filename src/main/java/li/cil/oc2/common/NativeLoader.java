package li.cil.oc2.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import joptsimple.util.InetAddressConverter;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.layer.DefaultSessionLayer;
import org.apache.logging.log4j.LogManager;

final class NativeLoader {
    private static final Map<String, String> supportedArch = new HashMap<>();
    private static boolean officiallySupported = true;
    private static final String LOOPBACK_IP = String.format("%d.%d.%d.%d", 127, 0, 0, 1);

    static {
        supportedArch.put("x86_64", "x86_64");
        supportedArch.put("amd64", "x86_64");
        supportedArch.put("aarch64", "arm64");
        supportedArch.put("arm64", "arm64");
    }

    public static void loadLibrary() {
        Platform platform = getPlatformName();
        String arch = getArchString();
        String libName =
                switch (platform) {
                    case MACOS -> "liboc2rnet-" + arch + ".dylib";
                    case WINDOWS -> "oc2rnet-" + arch + ".dll";
                    case LINUX -> "liboc2rnet-linux-" + arch + ".so";
                    case ANDROID -> "liboc2rnet-android-" + arch + ".so";
                    case UNSUPPORTED -> "NONE";
                    default -> throw new AssertionError(platform);
                };

        String resourcePath = "/natives/" + platform + "/" + libName;
        try {
            Path tempFile = extractToTemp(resourcePath);
            System.load(tempFile.toAbsolutePath().toString());
            Main.LoadedLibrary = true;
            InetAddress address = new InetAddressConverter().convert(LOOPBACK_IP);
            Random garbageGenerator = new Random();
            byte[] dataToSend = new byte[64];
            for (int i = 0; i < 64; i++) {
                dataToSend[i] = (byte) garbageGenerator.nextInt(0, 255);
            }
            byte[] data =
                    DefaultSessionLayer.sendICMP(
                            address.getAddress(),
                            dataToSend,
                            64,
                            Config.defaultEchoRequestTimeoutMs);
            if (data != null) {
                for (int i = 0; i < 64; i++) {
                    if (data[i] != dataToSend[i]) {
                        LogManager.getLogger()
                                .error(
                                        "ICMP data does not match, falling back to JVM UDP"
                                                + " implementation");
                        Main.LoadedLibrary = false;
                    }
                }
            } else {
                Main.LoadedLibrary = false;
                LogManager.getLogger()
                        .error(
                                "Loaded native library successfully but ICMP still failed, falling"
                                        + " back to JVM UDP implementation");
            }
        } catch (FileNotFoundException fileNotFoundException) {
            if (officiallySupported) {
                Main.LoadedLibrary = false;
                LogManager.getLogger()
                        .warn(
                                "Failed to load native library, jar file is corrupted or build"
                                        + " failed, attempted to load from path: {}",
                                resourcePath);
            } else {
                Main.LoadedLibrary = false;
                LogManager.getLogger().warn("Unsupported architecture: {}", arch);
            }
        } catch (IOException e) {
            Main.LoadedLibrary = false;
            LogManager.getLogger().warn("Failed to load native library: {}", resourcePath, e);
        } catch (UnsatisfiedLinkError linkError) {
            // System.load() succeeded, but the native library does not export
            // the JNI symbol(s) that the Java side declares as `native`. This
            // typically happens when the bundled prebuilt binaries are out of
            // sync with the current Java package layout (e.g. a class was
            // moved and the corresponding JNIEXPORT symbol name changed).
            // Falling back to the JVM UDP implementation is the intended
            // behaviour here; rethrowing would crash mod construction.
            Main.LoadedLibrary = false;
            LogManager.getLogger()
                    .warn(
                            "Native library was loaded but does not expose the expected JNI"
                                    + " symbols ({}); falling back to JVM UDP implementation."
                                    + " This usually means the bundled natives are out of sync"
                                    + " with the current Java class layout.",
                            linkError.getMessage());
        } catch (NoClassDefFoundError classError) {
            // Defensive: any class-resolution issue triggered by the probe
            // call should not take down the whole mod.
            Main.LoadedLibrary = false;
            LogManager.getLogger()
                    .warn(
                            "Native library probe failed due to a missing class ({}); falling"
                                    + " back to JVM UDP implementation.",
                            classError.getMessage());
        }
    }

    private static String getArchString() {
        String arch = System.getProperty("os.arch").toLowerCase(java.util.Locale.ROOT);
        String result = supportedArch.get(arch);
        if (result == null) {
            officiallySupported = false;
            return arch;
        }
        return result;
    }

    private static Platform getPlatformName() {
        String os = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);

        if (os.contains("mac")) {
            return Platform.MACOS;
        } else if (os.contains("win")) {
            return Platform.WINDOWS;
        } else if (os.contains("nux") || os.contains("nix")) {
            return Platform.LINUX;
        } else {
            if ("the android project".equalsIgnoreCase(System.getProperty("java.vm.vendor")))
                return Platform.ANDROID;
            officiallySupported = false;
            return Platform.UNSUPPORTED;
        }
    }

    private static Path extractToTemp(String resourcePath) throws IOException {
        try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
            Path local = Path.of(System.getProperty("user.dir"), System.mapLibraryName("oc2rnet"));
            Files.copy(in, local, StandardCopyOption.REPLACE_EXISTING);
            return local;
        }
    }

    private enum Platform {
        LINUX,
        MACOS,
        WINDOWS,
        ANDROID,
        UNSUPPORTED;

        @Override
        public String toString() {
            return super.toString().toLowerCase(java.util.Locale.ROOT);
        }
    }
}