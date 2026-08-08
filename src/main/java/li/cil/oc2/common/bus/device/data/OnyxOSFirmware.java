package li.cil.oc2.common.bus.device.data;

import java.io.IOException;
import java.io.InputStream;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.memory.MemoryMaps;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * OnyxOS firmware — OpenSBI ({@code fw_jump.bin}) + OnyxKernel raw binary,
 * loaded with the same "minux" layout as {@link MinuxFirmware}: OpenSBI at the
 * start address, the S-mode kernel at {@code startAddress + 0x200000} where
 * OpenSBI's fw_jump payload jumps (0x80200000).
 *
 * <p>The kernel is the {@code --features smode} build converted to a flat
 * binary with {@code objcopy -O binary} (OpenSBI jumps to raw machine code,
 * not to an ELF entry point).
 */
public final class OnyxOSFirmware implements Firmware {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String RESOURCE_OPEN_SBI = "/onyxos/fw_jump.bin";
    private static final String RESOURCE_KERNEL = "/onyxos/onyx-kernel.bin";

    @Override
    public boolean run(final MemoryMap memory, final long startAddress) {
        try (final InputStream firmware = open(RESOURCE_OPEN_SBI);
             final InputStream kernel = open(RESOURCE_KERNEL)) {
            if (firmware == null || kernel == null) {
                LOGGER.error("OnyxOS resources missing from the mod jar — VM will not boot.");
                return false;
            }
            MemoryMaps.store(memory, startAddress, firmware);
            MemoryMaps.store(memory, startAddress + 0x200000, kernel);
            return true;
        } catch (final IOException | RuntimeException e) {
            LOGGER.error("Failed to load OnyxOS firmware into VM memory", e);
            return false;
        }
    }

    private static InputStream open(final String path) {
        return OnyxOSFirmware.class.getResourceAsStream(path);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("OnyxOS");
    }
}
