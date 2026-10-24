package li.cil.oc2.fabric.mixin.client;

import java.util.concurrent.atomic.AtomicBoolean;
import li.cil.oc2.platform.event.ClientEvents;
import net.minecraft.client.DeltaTracker;
//? if <26.2 {
import net.minecraft.client.gui.Gui;
//?}
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets {@link ClientEvents#HIDE_HOTBAR} listeners hide the hotbar, like NeoForge's GUI layer event. */
//? if >=26.2 {
/*@Mixin(net.minecraft.client.gui.Hud.class)
*///?} else {
@Mixin(Gui.class)
//?}
public abstract class GuiMixin {
    //? if >=26.1 {
/*    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
*///?} else {
    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    //?}
    private void oc2r$hideHotbar(
            final GuiGraphics ignoredGraphics,
            final DeltaTracker ignoredDeltaTracker,
            final CallbackInfo ci) {
        final AtomicBoolean hide = new AtomicBoolean();
        ClientEvents.HIDE_HOTBAR.fire(listener -> {
            if (listener.getAsBoolean()) {
                hide.set(true);
            }
        });
        if (hide.get()) {
            ci.cancel();
        }
    }
}
