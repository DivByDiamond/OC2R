package li.cil.oc2.fabric.mixin.client;

import li.cil.oc2.client.input.CaptureKeyboardHook;
import net.minecraft.client.KeyboardHandler;
//? if >=26.1 {
/*import net.minecraft.client.input.KeyEvent;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets captured guests use keys otherwise reserved for vanilla host shortcuts. */
@SuppressWarnings("PMD.AvoidDollarSigns") // Mixin handler names are namespaced to avoid collisions.
@Mixin(KeyboardHandler.class)
public abstract class CaptureKeyboardMixin {
    //? if >=26.1 {
/*    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void oc2r$capture(final long window, final int action, final KeyEvent event, final CallbackInfo ci) {
        if (CaptureKeyboardHook.handle(window, event.key(), event.scancode(), action)) ci.cancel();
    }
*///?} else {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void oc2r$capture(final long window, final int key, final int scan, final int action,
            final int unusedModifiers, final CallbackInfo ci) {
        if (CaptureKeyboardHook.handle(window, key, scan, action)) ci.cancel();
    }
    //?}
}
