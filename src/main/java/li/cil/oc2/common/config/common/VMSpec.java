package li.cil.oc2.common.config.common;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.config.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

public class VMSpec {
    public final ModConfigSpec.LongValue maxAllocatedMemory;
    public final ModConfigSpec.IntValue vmTimeQuotaMs;
    public final ModConfigSpec.IntValue diskSizeTier1;
    public final ModConfigSpec.IntValue diskSizeTier2;
    public final ModConfigSpec.IntValue diskSizeTier3;
    public final ModConfigSpec.IntValue diskSizeTier4;
    public final ModConfigSpec.IntValue flashMemorySizeTier1;
    public final ModConfigSpec.IntValue flashMemorySizeTier2;
    public final ModConfigSpec.IntValue flashMemorySizeTier3;

    VMSpec(ModConfigSpec.Builder builder) {
        maxAllocatedMemory =
                builder.comment(
                                "Maximum memory that can be allocated across all virtual machines"
                                        + " (computers/robots) at any one time (in bytes)")
                        .defineInRange(
                                "maxAllocatedMemory", 512 * Constants.MEGABYTE, 0, Long.MAX_VALUE);

        vmTimeQuotaMs =
                builder.comment(
                                "The amount of time (in milliseconds) a virtual machine may run"
                                        + " per tick. Lower on weak servers, higher on powerful"
                                        + " ones.")
                        .defineInRange("vmTimeQuotaMs", 25, 1, Integer.MAX_VALUE);

        diskSizeTier1 =
                builder.comment(
                                "Size of the Small hard drive (in megabytes).",
                                "Tier layout:",
                                "Small Disk: diskSizeTier1",
                                "Medium Disk: diskSizeTier2",
                                "Large Disk: diskSizeTier3",
                                "Extra Large Disk: diskSizeTier4",
                                "With the default values this is equivalent to (in the same order)"
                                        + " 8MB, 16MB, 32MB, 128MB.")
                        .defineInRange("diskSizeTier1", 8, 1, Integer.MAX_VALUE);
        diskSizeTier2 =
                builder.comment("Size of the Medium hard drive (in megabytes).")
                        .defineInRange("diskSizeTier2", 16, 1, Integer.MAX_VALUE);
        diskSizeTier3 =
                builder.comment("Size of the Large hard drive (in megabytes).")
                        .defineInRange("diskSizeTier3", 32, 1, Integer.MAX_VALUE);
        diskSizeTier4 =
                builder.comment("Size of the Extra Large hard drive (in megabytes).")
                        .defineInRange("diskSizeTier4", 128, 1, Integer.MAX_VALUE);

        flashMemorySizeTier1 =
                builder.comment(
                                "Size of a small flash memory item (in megabytes). This is the"
                                        + " size of the firmware image that can be stored on a single"
                                        + " flash memory item.")
                        .defineInRange("flashMemorySizeTier1", 4, 1, Integer.MAX_VALUE);
        flashMemorySizeTier2 =
                builder.comment(
                                "Size of a medium flash memory item (in megabytes). This is the"
                                        + " size of the firmware image that can be stored on a single"
                                        + " flash memory item.")
                        .defineInRange("flashMemorySizeTier2", 8, 1, Integer.MAX_VALUE);
        flashMemorySizeTier3 =
                builder.comment(
                                "Size of a standard flash memory item (in megabytes). This is the"
                                        + " size of the firmware image that can be stored on a single"
                                        + " flash memory item.")
                        .defineInRange("flashMemorySizeTier3", 16, 1, Integer.MAX_VALUE);
    }

    public void loadValues() {
        Config.maxAllocatedMemory = maxAllocatedMemory.get();
        Config.vmTimeQuotaMs = vmTimeQuotaMs.get();
        Config.diskSizeTier1 = diskSizeTier1.get() * Constants.MEGABYTE;
        Config.diskSizeTier2 = diskSizeTier2.get() * Constants.MEGABYTE;
        Config.diskSizeTier3 = diskSizeTier3.get() * Constants.MEGABYTE;
        Config.diskSizeTier4 = diskSizeTier4.get() * Constants.MEGABYTE;
        Config.flashMemorySizeTier1 = flashMemorySizeTier1.get() * Constants.MEGABYTE;
        Config.flashMemorySizeTier2 = flashMemorySizeTier2.get() * Constants.MEGABYTE;
        Config.flashMemorySizeTier3 = flashMemorySizeTier3.get() * Constants.MEGABYTE;
    }
}