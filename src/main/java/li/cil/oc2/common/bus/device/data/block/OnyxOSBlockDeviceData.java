package li.cil.oc2.common.bus.device.data.block;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * OnyxOS root filesystem (OnyxFS v2) loaded from the mod jar. Used as the
 * read-only base for a hard drive, which {@code HardDriveDeviceWithInitialData}
 * copies into a writable drive on first use — so OnyxKernel can mount it and
 * boot to userspace.
 *
 * <p>Overridable: {@code config/oc2r/onyxfs.img} takes precedence over the
 * built-in resource, so a pack can ship a custom root filesystem without
 * rebuilding the mod.
 */
public final class OnyxOSBlockDeviceData implements BlockDeviceData {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final ByteBufferBlockDevice INSTANCE;

    static {
        ByteBufferBlockDevice instance;
        final Path override = FMLPaths.CONFIGDIR.get().resolve("oc2r").resolve("onyxfs.img");
        try {
            if (Files.isRegularFile(override)) {
                LOGGER.info("OnyxOS: using override {}", override);
                instance = ByteBufferBlockDevice.createFromStream(Files.newInputStream(override), true);
            } else {
                try (InputStream stream =
                        OnyxOSBlockDeviceData.class.getResourceAsStream("/onyxos/onyxfs.img")) {
                    if (stream == null) {
                        LOGGER.error("onyxos/onyxfs.img missing from mod jar");
                        instance = ByteBufferBlockDevice.create(0, true);
                    } else {
                        instance = ByteBufferBlockDevice.createFromStream(stream, true);
                    }
                }
            }
        } catch (final IOException e) {
            LOGGER.error("Failed to load OnyxOS root filesystem", e);
            instance = ByteBufferBlockDevice.create(0, true);
        }
        INSTANCE = instance;
    }

    @Override
    public BlockDevice getBlockDevice() {
        return INSTANCE;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("OnyxOS");
    }
}
