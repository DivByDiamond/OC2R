/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SoundEvents {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final DeferredHolder<SoundEvent, SoundEvent> COMPUTER_RUNNING = register("computer_running");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLOPPY_ACCESS = register("floppy_access");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLOPPY_EJECT = register("floppy_eject");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLOPPY_INSERT = register("floppy_insert");
    public static final DeferredHolder<SoundEvent, SoundEvent> HDD_ACCESS = register("hdd_access");

    ///////////////////////////////////////////////////////////////////

    public static void initialize(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    ///////////////////////////////////////////////////////////////////

    private static DeferredHolder<SoundEvent, SoundEvent> register(final String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, name)));
    }
}
