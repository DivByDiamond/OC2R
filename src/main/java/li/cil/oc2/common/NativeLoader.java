package li.cil.oc2.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Random;
import joptsimple.util.InetAddressConverter;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.DefaultSessionLayer;
import org.apache.logging.log4j.LogManager;

final class NativeLoader {
    private static final HashMap<String, String> supportedArch = new HashMap<>();
    private static boolean officiallySupported = true;

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
                };

        String resourcePath = "/natives/" + platform + "/" + libName;
        try {
            Path tempFile = extractToTemp(resourcePath);
            System.load(tempFile.toAbsolutePath().toString());
            Main.LoadedLibrary = true;
            InetAddress address = new InetAddressConverter().convert("127.0.0.1");
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
        }
    }

    private static String getArchString() {
        String arch = System.getProperty("os.arch").toLowerCase();
        String result = supportedArch.get(arch);
        if (result == null) {
            officiallySupported = false;
            return arch;
        }
        return result;
    }

    private static Platform getPlatformName() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac")) {
            return Platform.MACOS;
        } else if (os.contains("win")) {
            return Platform.WINDOWS;
        } else if (os.contains("nux") || os.contains("nix")) {
            return Platform.LINUX;
        } else {
            if (System.getProperty("java.vm.vendor").equalsIgnoreCase("the android project"))
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
            return super.toString().toLowerCase();
        }
    }
}