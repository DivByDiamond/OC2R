package li.cil.oc2.common.bus.device.data.block;

import java.io.IOException;
import java.io.InputStream;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * OnyxOS root filesystem (OnyxFS v2) loaded from the mod jar. Used as the
 * read-only base for a hard drive, which {@code HardDriveDeviceWithInitialData}
 * copies into a writable drive on first use — so OnyxKernel can mount it and
 * boot to userspace.
 */
public final class OnyxOSBlockDeviceData implements BlockDeviceData {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final ByteBufferBlockDevice INSTANCE;

    static {
        ByteBufferBlockDevice instance;
        try (final InputStream stream =
                OnyxOSBlockDeviceData.class.getResourceAsStream("/onyxos/onyxfs.img")) {
            if (stream == null) {
                throw new IOException("onyxos/onyxfs.img missing from mod jar");
            }
            instance = ByteBufferBlockDevice.createFromStream(stream, true);
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
