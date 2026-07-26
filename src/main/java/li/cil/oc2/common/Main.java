/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common;

import joptsimple.util.InetAddressConverter;
import li.cil.ceres.Ceres;
import li.cil.oc2.api.API;
import li.cil.oc2.client.manual.Manuals;
import li.cil.oc2.common.block.BlockCodecs;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import li.cil.oc2.common.bus.device.provider.ProviderRegistry;
import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.components.DataComponents;
import li.cil.oc2.common.config.client.ClientSpec;
import li.cil.oc2.common.config.common.CommonSpec;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.inet.DefaultSessionLayer;
import li.cil.oc2.common.item.ItemGroup;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.RecipeSerializers;
import li.cil.oc2.common.serialization.ceres.Serializers;
import li.cil.oc2.common.tags.BlockTags;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.RegistryUtils;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.vm.provider.DeviceTreeProviders;
import li.cil.sedna.Sedna;
import org.apache.logging.log4j.LogManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Random;

@Mod(API.MOD_ID)
public final class Main {
    public static boolean LoadedLibrary = false;

    public Main(IEventBus modBus, ModContainer container) {
        Ceres.initialize();
        Sedna.initialize();
        DeviceTreeProviders.initialize();
        Serializers.initialize();

        container.registerConfig(ModConfig.Type.COMMON, CommonSpec.CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientSpec.CLIENT_CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, AsyncConfig.SERVER_SPEC);

        RegistryUtils.begin();

        ItemTags.initialize();
        BlockTags.initialize();
        DataComponents.initialize(modBus);
        Blocks.initialize(modBus);
        BlockCodecs.initialize(modBus);
        Items.initialize(modBus);
        BlockEntities.initialize(modBus);
        Entities.initialize(modBus);
        Containers.initialize(modBus);
        RecipeSerializers.initialize(modBus);
        SoundEvents.initialize(modBus);

        ProviderRegistry.initialize(modBus);

        BlockDeviceDataRegistry.initialize(modBus);
        FirmwareRegistry.initialize(modBus);

        RegistryUtils.finish(modBus);

        modBus.register(CommonSetup.class);
        if (FMLLoader.getDist() == Dist.CLIENT) {
            Manuals.initialize(modBus);
        }

        ItemGroup.TAB_REGISTER.register(modBus);

        NativeLoader.loadLibrary();
    }

    public static class NativeLoader {
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
            String libName = switch (platform) {
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
                LoadedLibrary = true;
                InetAddress address = new InetAddressConverter().convert("127.0.0.1");
                Random garbageGenerator = new Random();
                byte[] dataToSend = new byte[64];
                for (int i = 0; i < 64; i++)
                {
                    dataToSend[i] = (byte) garbageGenerator.nextInt(0, 255);
                }
                byte[] data = DefaultSessionLayer.sendICMP(address.getAddress(), dataToSend, 64, Config.defaultEchoRequestTimeoutMs);
                if (data != null) {
                    for (int i = 0; i < 64; i++) {
                        if (data[i] != dataToSend[i]) {
                            LogManager.getLogger().error("ICMP data does not match, falling back to JVM UDP implementation");
                            LoadedLibrary = false;
                        }
                    }
                }
                else {
                    LoadedLibrary = false;
                    LogManager.getLogger().error("Loaded native library successfully but ICMP still failed, falling back to JVM UDP implementation");
                }
            } catch(FileNotFoundException fileNotFoundException) {
                if (officiallySupported) {
                    LoadedLibrary = false;
                    LogManager.getLogger().warn("Failed to load native library, jar file is corrupted or build failed, attempted to load from path: {}", resourcePath);
                } else {
                    LoadedLibrary = false;
                    LogManager.getLogger().warn("Unsupported architecture: {}", arch);
                }
            } catch (IOException e) {
                LoadedLibrary = false;
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
                if (System.getProperty("java.vm.vendor").equalsIgnoreCase("the android project")) return Platform.ANDROID;
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
}
