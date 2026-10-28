package li.cil.oc2.common.blockentity.keyboard.link;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.KeyboardDevice;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned capture lease, including deterministic held-key cleanup. */
@SuppressWarnings("PMD.CompareObjectsWithEquals") // Leases bind exact live player, world and device instances.
public final class KeyboardInputSession {
    private final KeyboardBlockEntity keyboard;
    private final HeldInputState held = new HeldInputState();
    @Nullable private ServerPlayer owner;
    @Nullable private KeyboardDevice<?> receiver;
    private long lastInput;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Capture owns references to live entities; copied state cannot track their lifecycle.")
    public KeyboardInputSession(final KeyboardBlockEntity keyboard) {
        this.keyboard = keyboard;
    }

    /** Number of accepted held keys/buttons, useful for capture diagnostics. */
    public int heldInputCount() { return held.size(); }

    /** Starts capture only after a real block interaction. */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Capture owns references to live entities; copied state cannot track their lifecycle.")
    public void begin(final ServerPlayer player) {
        release();
        owner = player;
        lastInput = player.level().getGameTime();
    }

    /** Tests capture ownership without extending its timeout. */
    public boolean owns(final ServerPlayer player) {
        return owner == player && valid();
    }

    /** Keeps capture alive and releases the previous endpoint when focus changes. */
    public void focus(final KeyboardDevice<?> target) {
        if (receiver != target) {
            releaseHeld();
            receiver = target;
        }
        lastInput = owner.level().getGameTime();
    }

    /** Emits only state transitions; duplicate packets cannot leave keys stuck. */
    public void key(final int code, final boolean down) {
        if (receiver != null) held.update(code, down, receiver::sendKeyEvent);
    }

    /** Emits a bounded relative pointer movement. */
    public void relative(final int axis, final int amount) {
        if (receiver != null && (axis == 0 || axis == 1 || axis == 8)) {
            receiver.sendRelativeEvent(axis, Math.clamp(amount, -1024, 1024));
        }
    }

    /** Ends capture and releases every key and pointer button on the old endpoint. */
    public void release() {
        releaseHeld();
        receiver = null;
        owner = null;
    }

    /** Expires disconnected, distant or abandoned captures even if no release packet arrives. */
    public void tick() {
        if (owner != null && !valid()) release();
    }

    private boolean valid() {
        return owner != null && !owner.isRemoved() && owner.connection != null
                && owner.connection.isAcceptingMessages()
                && owner.level() == keyboard.getLevel()
                && keyboard.getBlockPos().closerToCenterThan(owner.position(), 8)
                && owner.level().getGameTime() - lastInput <= 40;
    }

    private void releaseHeld() {
        if (receiver != null) held.release(receiver::sendKeyEvent);
    }
}
