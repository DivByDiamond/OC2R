package li.cil.oc2.common.bus.device.data.firmware;

import java.io.IOException;
import java.io.InputStream;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.memory.MemoryMaps;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class MinuxFirmware implements Firmware {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean run(final MemoryMap memory, final long startAddress) {
        // Since sedna-buildroot 0.2.x the firmware is a minimal boot ROM that
        // loads the kernel from disk (rootfs.ext2 on a virtio-blk device); no
        // kernel image is stored into flash anymore.
        try (InputStream firmware = Buildroot.getSednaFirmware()) {
            if (firmware == null) {
                LOGGER.error(
                        "Minux firmware resource (generated/firmware.bin) is missing from"
                                + " sedna-buildroot.jar — VM will not boot.");
                return false;
            }
            MemoryMaps.store(memory, startAddress, firmware);
            return true;
        } catch (final IOException e) {
            LOGGER.error("Failed to load Minux firmware into VM memory", e);
            return false;
        } catch (final Exception t) {
            LOGGER.error("Unexpected error while loading Minux firmware into VM memory", t);
            return false;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Minux");
    }
}