package li.cil.oc2.common.config.common;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.config.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

public class VMSpec {
    public final ModConfigSpec.LongValue maxAllocatedMemory;
    public final ModConfigSpec.IntValue diskSizeFactor;
    public final ModConfigSpec.IntValue flashMemorySize;

    VMSpec(ModConfigSpec.Builder builder) {
        maxAllocatedMemory =
                builder.comment(
                                "Maximum memory that can be allocated across all virtual machines"
                                        + " (computers/robots) at any one time (in bytes)")
                        .defineInRange(
                                "maxAllocatedMemory", 512 * Constants.MEGABYTE, 0, Long.MAX_VALUE);

        diskSizeFactor =
                builder.comment(
                                "Determines the size factor of drives, where SF is the size factor"
                                        + " set below the sizes are as follows (this settings is in"
                                        + " bytes):",
                                "Small Disk: SF",
                                "Medium Disk: 2 * SF",
                                "Large Disk: 4 * SF",
                                "Extra Large Disk: 8 * SF",
                                "With the default factor this is equivalent to (in the same order)"
                                        + " 2MB, 4MB, 8MB, 16MB.")
                        .defineInRange(
                                "diskSizeFactor", 2 * Constants.MEGABYTE, 0, Integer.MAX_VALUE);

        flashMemorySize =
                builder.comment(
                                "Size of a flash memory item (in megabytes). This is the size of"
                                        + " the firmware image that can be stored on a single flash"
                                        + " memory item.")
                        .defineInRange("flashMemorySize", 15, 1, Integer.MAX_VALUE);
    }

    public void loadValues() {
        Config.maxAllocatedMemory = maxAllocatedMemory.get();
        Config.diskSizeFactor = diskSizeFactor.get();
        Config.flashMemorySize = flashMemorySize.get() * Constants.MEGABYTE;
    }
}