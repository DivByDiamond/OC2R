package li.cil.oc2.client.input;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import li.cil.oc2.client.gui.screen.misc.KeyCodeMapping;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.blockentity.keyboard.link.KeyboardTargets;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.blockentity.projector.misc.ProjectorContraptionHelper;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.monitor.input.KeyboardInputMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** World-visible capture; pointer deltas are batched so render FPS cannot flood the server. */
public final class KeyboardCapture {
    private final KeyboardBlockEntity keyboard;
    private BlockPos focus;
    private boolean firstMove = true;
    private double lastX;
    private double lastY;
    private double pendingX;
    private double pendingY;
    private double pendingWheel;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Capture owns references to live entities; copied state cannot track their lifecycle.")
    public KeyboardCapture(final KeyboardBlockEntity keyboard) {
        this.keyboard = keyboard;
        focus = keyboard.getTargetPos();
    }

    /** Hides and confines the host cursor without closing the transparent capture screen. */
    public void start() {
        firstMove = true;
        cursorMode(GLFW.GLFW_CURSOR_DISABLED);
    }

    /** Releases the server lease and restores normal host cursor handling. */
    public void close() {
        send(KeyboardInputMessage.RELEASE, 0);
        cursorMode(GLFW.GLFW_CURSOR_NORMAL);
    }

    /** Updates the viewed input endpoint and keeps an idle capture lease alive. */
    public void tick() {
        final Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        final BlockPos next = viewedFocus(client);
        if (!next.equals(focus)) {
            // Discard old-target pointer deltas; the server releases held keys before switching.
            pendingX = 0;
            pendingY = 0;
            pendingWheel = 0;
            focus = next;
        }
        send(KeyboardInputMessage.HEARTBEAT, 0);
        pendingX = flushAxis(0, pendingX);
        pendingY = flushAxis(1, pendingY);
        pendingWheel = flushAxis(8, pendingWheel);
    }

    private BlockPos viewedFocus(final Minecraft client) {
        final HitResult hit = client.player.pick(32, 0, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
            return keyboard.getTargetPos();
        }
        final BlockEntity endpoint = KeyboardTargets.resolve(client.level, block.getBlockPos());
        return endpoint != null ? endpoint.getBlockPos() : projectedFocus(client, hit.getLocation());
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals") // Projectors must belong to this exact live level.
    private BlockPos projectedFocus(final Minecraft client, final Vec3 hit) {
        BlockPos next = keyboard.getTargetPos();
        double nearest = Double.POSITIVE_INFINITY;
        for (final ProjectorBlockEntity projector : ProjectorContraptionHelper.loadedProjectors()) {
            if (projector.getLevel() != client.level || projector.isRemoved() || !projector.isProjecting()
                    || !KeyboardTargets.projectsOnto(projector, hit)) continue;
            final double distance = client.player.distanceToSqr(Vec3.atCenterOf(projector.getBlockPos()));
            if (distance < nearest && distance <= 32 * 32) {
                nearest = distance;
                next = projector.getBlockPos();
            }
        }
        return next;
    }

    public void key(final int key, final boolean down) {
        if (KeyCodeMapping.MAPPING.containsKey(key)) send(KeyCodeMapping.MAPPING.get(key), down ? 1 : 0);
    }

    public void button(final int button, final boolean down) {
        if (button >= 0 && button <= 7) send(0x110 + button, down ? 1 : 0);
    }

    public void scroll(final double amount) { pendingWheel += amount; }

    /** Keeps normal world aiming available while also moving the guest's relative pointer. */
    public void move(final double x, final double y) {
        if (firstMove) {
            firstMove = false;
            lastX = x;
            lastY = y;
            return;
        }
        final Minecraft client = Minecraft.getInstance();
        final double dx = (x - lastX) * client.getWindow().getGuiScale();
        final double dy = (y - lastY) * client.getWindow().getGuiScale();
        lastX = x;
        lastY = y;
        pendingX += dx;
        pendingY += dy;
        final double sensitivity = client.options.sensitivity().get() * 0.6 + 0.2;
        final double scale = sensitivity * sensitivity * sensitivity * 8;
        if (client.player != null) {
            //? if >=26.1 {
            /*client.player.turn(dx * scale * (client.options.invertMouseX().get() ? -1 : 1),
                    dy * scale * (client.options.invertMouseY().get() ? -1 : 1));
            *///?} else {
            client.player.turn(dx * scale, dy * scale * (client.options.invertYMouse().get() ? -1 : 1));
            //?}
        }
    }

    private double flushAxis(final int axis, final double pending) {
        final int value = (int) Math.clamp(pending, -1024, 1024);
        if (value != 0) send(KeyboardInputMessage.RELATIVE + axis, value);
        return pending - value;
    }

    private void send(final int code, final int value) {
        if (Minecraft.getInstance().getConnection() != null) {
            NetworkMessages.sendToServer(new KeyboardInputMessage(keyboard, focus, code, value));
        }
    }

    private static void cursorMode(final int mode) {
        final var window = Minecraft.getInstance().getWindow();
        //? if >=26.1 {
        /*GLFW.glfwSetInputMode(window.handle(), GLFW.GLFW_CURSOR, mode);
        *///?} else {
        GLFW.glfwSetInputMode(window.getWindow(), GLFW.GLFW_CURSOR, mode);
        //?}
    }
}
