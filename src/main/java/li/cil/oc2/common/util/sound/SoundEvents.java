package li.cil.oc2.common.util.sound;

import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class SoundEvents {
    public static final Supplier<SoundEvent> COMPUTER_RUNNING =
            register("computer_running");
    public static final Supplier<SoundEvent> POST_BEEP_FIRMWARE =
            register("post_beep_firmware");
    public static final Supplier<SoundEvent> POST_BEEP_ENERGY =
            register("post_beep_energy");
    public static final Supplier<SoundEvent> POST_BEEP_CPU =
            register("post_beep_cpu");
    public static final Supplier<SoundEvent> POST_BEEP_MEMORY =
            register("post_beep_memory");
    public static final Supplier<SoundEvent> POST_BEEP_UNKNOWN =
            register("post_beep_unknown");
    public static final Supplier<SoundEvent> FLOPPY_ACCESS =
            register("floppy_access");
    public static final Supplier<SoundEvent> FLOPPY_EJECT =
            register("floppy_eject");
    public static final Supplier<SoundEvent> FLOPPY_INSERT =
            register("floppy_insert");
    public static final Supplier<SoundEvent> HDD_ACCESS = register("hdd_access");
    public static final Supplier<SoundEvent> SOUND_CARD_BEEP =
            register("sound_card_beep");

    public static void initialize() {
        // Calling this loads the class, which queues its registrations on the bridge.
    }

    private static Supplier<SoundEvent> register(final String name) {
        return Platform.registries().register("minecraft:sound_event", API.MOD_ID, 
                name,
                () ->
                        SoundEvent.createVariableRangeEvent(
                                ResourceLocation.fromNamespaceAndPath(API.MOD_ID, name)));
    }
}