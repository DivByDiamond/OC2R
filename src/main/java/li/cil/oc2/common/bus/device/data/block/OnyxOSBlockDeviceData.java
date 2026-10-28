package li.cil.oc2.common.bus.device.data.block;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.vm.misc.VersionedRootImages;
import li.cil.oc2.platform.Platform;
import li.cil.sedna.api.device.BlockDevice;
import net.minecraft.network.chat.Component;
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

    private static final VersionedRootImages IMAGES = loadImages();

    private static VersionedRootImages loadImages() {
        final Path config = Platform.environment().configDir().resolve("oc2r");
        final Path override = config.resolve("onyxfs.img");
        final boolean custom = Files.isRegularFile(override);
        try (InputStream current = custom ? Files.newInputStream(override)
                        : OnyxOSBlockDeviceData.class.getResourceAsStream("/onyxos/onyxfs.img");
                InputStream legacy = OnyxOSBlockDeviceData.class.getResourceAsStream("/onyxos/onyxfs-20261016.img");
                InputStream previous = OnyxOSBlockDeviceData.class.getResourceAsStream("/onyxos/onyxfs-20261018.img")) {
            if (current == null || legacy == null || previous == null) {
                throw new IllegalStateException("Bundled OnyxOS root image is missing");
            }
            if (custom) LOGGER.info("OnyxOS: using override {}", override);
            return new VersionedRootImages(current.readAllBytes(), legacy.readAllBytes(),
                    config.resolve("root-images"), custom, previous.readAllBytes());
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot load and preserve OnyxOS root images", e);
        }
    }

    public static VersionedRootImages.Image currentRootImage() throws IOException {
        return IMAGES.current();
    }

    public static VersionedRootImages.Image savedRootImage(final String id) throws IOException {
        return IMAGES.resolve(id);
    }

    @Override
    public BlockDevice getBlockDevice() {
        return IMAGES.standaloneDisk().device();
    }

    @Override
    public long getCapacity() {
        return IMAGES.standaloneDisk().device().getCapacity();
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("OnyxOS");
    }
}
