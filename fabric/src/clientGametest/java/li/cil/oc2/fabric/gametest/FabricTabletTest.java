package li.cil.oc2.fabric.gametest;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.client.RenderCompat;
import li.cil.oc2.client.gui.screen.tablet.TabletInventoryScreen;
import li.cil.oc2.client.gui.screen.tablet.TabletTerminalScreen;
import li.cil.oc2.client.input.CaptureKeyBinding;
import li.cil.oc2.client.input.CaptureKeyboardHook;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.tablet.TabletItem;
import li.cil.oc2.common.tablet.TabletManager;
import li.cil.oc2.common.tablet.TabletClientView;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Opens both tablet menus on a real client and releases captured native keyboard input. */
public final class FabricTabletTest implements FabricClientGameTest {
    @Override
    public void runTest(final ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            client.options.pauseOnLostFocus = false;
            client.options.renderDistance().set(3);
            client.options.framerateLimit().set(30);
            client.options.enableVsync().set(false);
        });
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                final var player = server.getPlayerList().getPlayers().getFirst();
                final ItemStack stack = new ItemStack(Items.TABLET.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                final var id = TabletItem.getOrCreateId(stack);
                final var machine = TabletManager.machineOf(server, id);
                if (machine == null) throw new IllegalStateException("Tablet machine was not created");
                machine.inventory().getItemHandler(DeviceTypes.CPU).orElseThrow()
                        .setStackInSlot(0, new ItemStack(Items.CPU_TIER_1.get()));
                machine.virtualMachine().start();
                TabletManager.openTerminal(player, id);
            });
            context.waitFor(client -> RenderCompat.currentScreen() instanceof TabletTerminalScreen, 200);
            context.runOnClient(client -> GLFW.glfwFocusWindow(client.getWindow().handle()));
            context.waitFor(client -> client.isWindowActive(), 200);
            context.waitTicks(30);
            context.runOnClient(client -> {
                final var screen = (TabletTerminalScreen) RenderCompat.currentScreen();
                hostTab(false);
                screen.getMenu().setCaptureInputState(true);
                hostClick(SIDEBAR_X, CAPTURE_Y);
                if (screen.getMenu().getCaptureInputState()) throw new AssertionError("Custom capture key did not release input");
                hostClick(SIDEBAR_X, CAPTURE_Y);
                if (!screen.getMenu().getCaptureInputState()) {
                    throw new IllegalStateException("Tablet keyboard capture is unavailable");
                }
            });
            waitForGuest(context, world.getServer(), "login: ");
            type(world.getServer(), "root\r");
            waitForGuest(context, world.getServer(), "osh$ ");
            type(world.getServer(), "otablet --device\r");
            waitForGuest(context, world.getServer(), "OTABLET_READY 256x160");
            context.takeScreenshot("oc2r-tablet-console");
            context.runOnClient(client -> hostTab(true));
            waitForGuest(context, world.getServer(), "OTABLET_DESKTOP_READY");
            context.waitFor(client -> {
                final byte[] pixels = framebuffer();
                // The guest paints its mouse cursor at (0,0) until the first
                // pointer event, so sample the header at its far edge.
                return pixel(pixels, 0, 159) == 0x08e4 && pixel(pixels, 255, 0) == 0x2a09;
            }, 2400);
            context.waitTicks(10);
            context.takeScreenshot("oc2r-tablet-desktop");
            guestClick(context, world.getServer(), 108, 30, "OTABLET_ACTION=101");
            guestClick(context, world.getServer(), 32, 148, "OTABLET_ACTION=1");
            guestClick(context, world.getServer(), 80, 148, "OTABLET_ACTION=2");
            for (final int key : new int[] {GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_L,
                    GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_O}) {
                context.runOnClient(client -> guestKey(key));
                context.waitTicks(2);
            }
            guestClick(context, world.getServer(), 216, 119, "OTABLET_NOTES_SAVED=5");
            context.waitTicks(30);
            context.takeScreenshot("oc2r-tablet-notes");
            guestClick(context, world.getServer(), 236, 28, "OTABLET_ACTION=102");
            guestClick(context, world.getServer(), 80, 148, "OTABLET_ACTION=2");
            context.waitFor(client -> notesContainHello(), 2400);
            context.waitTicks(10);
            context.takeScreenshot("oc2r-tablet-notes-reopened");
            guestClick(context, world.getServer(), 236, 28, "OTABLET_ACTION=102");
            guestClick(context, world.getServer(), 176, 148, "OTABLET_APP_STARTED=osnake");
            context.waitTicks(40);
            context.takeScreenshot("oc2r-tablet-snake");
            context.runOnClient(client -> guestKey(GLFW.GLFW_KEY_Q));
            waitForGuest(context, world.getServer(), "OTABLET_APP_RETURN=osnake STATUS=0");
            context.waitTicks(30);
            context.runOnClient(client -> hostTab(false));
            context.takeScreenshot("oc2r-tablet-console-after-app");
            context.runOnClient(client -> hostTab(true));
            context.waitTicks(10);
            context.takeScreenshot("oc2r-tablet-desktop-after-app");
            guestClick(context, world.getServer(), 80, 148, "OTABLET_ACTION=2");
            context.runOnClient(client -> {
                GLFW.glfwSetClipboardString(client.getWindow().handle(), "paste_in_screen");
                ((TabletTerminalScreen) RenderCompat.currentScreen()).keyPressed(
                        new KeyEvent(GLFW.GLFW_KEY_V, 0, GLFW.GLFW_MOD_CONTROL));
            });
            context.waitTicks(20);
            world.getServer().runOnServer(server -> {
                final var player = server.getPlayerList().getPlayers().getFirst();
                final var machine = TabletManager.machineOf(server, TabletItem.getId(player.getMainHandItem()).orElseThrow());
                final boolean found = machine.virtualMachine().busController.getDevices().stream()
                        .anyMatch(device -> device instanceof RPCDevice rpc
                                && rpc.getTypeNames().contains("tablet") && rpc.getTypeNames().contains("gps_tower")
                                && rpc.getMethodGroups().stream().anyMatch(group -> group.getName().equals("sendRadioMessage")));
                if (!found) throw new IllegalStateException("Messenger-compatible tablet radio is missing from the guest bus");
            });
            context.runOnClient(client -> hostClick(SIDEBAR_X, INVENTORY_Y));
            // The slot contents arrive in their own packet after the screen opens.
            context.waitFor(client -> RenderCompat.currentScreen() instanceof final TabletInventoryScreen screen
                    && screen.getMenu().getSlot(0).getItem().is(Items.CPU_TIER_1.get()), 100);
            context.takeScreenshot("oc2r-tablet-inventory");
            context.runOnClient(client -> ((TabletInventoryScreen) RenderCompat.currentScreen()).getMenu().switchToTerminal());
            context.waitFor(client -> RenderCompat.currentScreen() instanceof TabletTerminalScreen, 100);
            context.runOnClient(client -> hostTab(true));
            guestClick(context, world.getServer(), 224, 148, "OTABLET_EXIT");
            type(world.getServer(), "cat /users/tablet-notes.txt\r");
            waitForGuest(context, world.getServer(), "hello");
            waitForGuest(context, world.getServer(), "paste_in_screen");
            context.runOnClient(client -> hostTab(false));
            context.runOnClient(client -> {
                GLFW.glfwSetClipboardString(client.getWindow().handle(), "echo PASTE_TABLET\r");
                ((TabletTerminalScreen) RenderCompat.currentScreen()).keyPressed(
                        new KeyEvent(GLFW.GLFW_KEY_V, 0, GLFW.GLFW_MOD_CONTROL));
            });
            waitForGuest(context, world.getServer(), "PASTE_TABLET");
            context.takeScreenshot("oc2r-tablet-console-after-guest-key");
            context.runOnClient(client -> {
                final boolean captured = CaptureKeyboardHook.handle(client.getWindow().handle(),
                        CaptureKeyBinding.RELEASE.getDefaultKey().getValue(), 0, GLFW.GLFW_PRESS, 0);
                if (!captured || RenderCompat.currentScreen() != null) {
                    throw new IllegalStateException("The shared release binding did not leave tablet capture");
                }
            });
            context.waitTicks(5);
        }
    }
    // Centres of the sidebar buttons relative to the viewport: power, capture, inventory, display.
    private static final int SIDEBAR_X = -17;
    private static final int CAPTURE_Y = 24;
    private static final int INVENTORY_Y = 38;
    private static final int DISPLAY_Y = 52;

    private static void hostTab(final boolean graphical) {
        final var screen = (TabletTerminalScreen) RenderCompat.currentScreen();
        if (screen.isGraphical() != graphical) hostClick(SIDEBAR_X, DISPLAY_Y);
        if (screen.isGraphical() != graphical) {
            throw new AssertionError("The tablet display button did not change views");
        }
    }

    private static void hostClick(final int x, final int y) {
        final var screen = (TabletTerminalScreen) RenderCompat.currentScreen();
        final var area = screen.viewport();
        final var event = new MouseButtonEvent(area.x() + x, area.y() + y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static byte[] framebuffer() {
        final var screen = (TabletTerminalScreen) RenderCompat.currentScreen();
        return TabletClientView.find(screen.getMenu().tabletId()).orElseThrow().framebuffer();
    }

    private static int pixel(final byte[] pixels, final int x, final int y) {
        final int offset = (y * 256 + x) * 2;
        return pixels[offset] & 255 | (pixels[offset + 1] & 255) << 8;
    }

    private static boolean notesContainHello() {
        final byte[] pixels = framebuffer();
        final int[] glyph = {17, 17, 17, 31, 17, 17, 17};
        for (int row = 0; row < glyph.length; row++) {
            for (int column = 0; column < 5; column++) {
                final boolean foreground = pixel(pixels, 20 + column, 45 + row) == 0x1987;
                if (foreground != ((glyph[row] & (1 << (4 - column))) != 0)) return false;
            }
        }
        return true;
    }

    private static void guestClick(final ClientGameTestContext context, final TestServerContext server,
                                   final int x, final int y, final String marker) {
        final long previous = markerCount(guestText(server), marker);
        context.runOnClient(client -> pointer(x, y, true));
        context.waitTicks(2);
        context.runOnClient(client -> pointer(x, y, false));
        for (int elapsed = 0; elapsed < 4800; elapsed += 5) {
            if (markerCount(guestText(server), marker) > previous) return;
            context.waitTicks(5);
        }
        throw new AssertionError("Guest action did not produce a new exact marker [" + marker + "]:\n" + guestText(server));
    }

    private static long markerCount(final String output, final String marker) {
        return output.lines().map(String::strip).filter(marker::equals).count();
    }

    private static String guestText(final TestServerContext server) {
        return server.computeOnServer(instance -> {
            final var player = instance.getPlayerList().getPlayers().getFirst();
            final var machine = TabletManager.machineOf(instance,
                    TabletItem.getId(player.getMainHandItem()).orElseThrow());
            return text(machine.terminal());
        });
    }

    private static void guestKey(final int key) {
        final var client = net.minecraft.client.Minecraft.getInstance();
        if (!CaptureKeyboardHook.handle(client.getWindow().handle(), key, 0, GLFW.GLFW_PRESS, 0)
                || !CaptureKeyboardHook.handle(client.getWindow().handle(), key, 0, GLFW.GLFW_RELEASE, 0)) {
            throw new AssertionError("Native key was not delivered to the guest: " + key);
        }
    }

    private static void pointer(final int x, final int y, final boolean down) {
        final var screen = (TabletTerminalScreen) RenderCompat.currentScreen();
        final var area = screen.viewport();
        final var event = new MouseButtonEvent(area.x() + x, area.y() + y, new MouseButtonInfo(0, 0));
        if (down) {
            // Older batched motion must not replay behind this immediate button edge.
            screen.mouseMoved(area.x() + 250, area.y() + 2);
            screen.mouseClicked(event, false);
        }
        else screen.mouseReleased(event);
    }

    private static void type(final TestServerContext server, final String text) {
        server.runOnServer(instance -> {
            final var player = instance.getPlayerList().getPlayers().getFirst();
            TabletManager.machineOf(instance, TabletItem.getId(player.getMainHandItem()).orElseThrow())
                    .terminal().io.putInput(text);
        });
    }

    private static void waitForGuest(final ClientGameTestContext context, final TestServerContext server,
                                     final String expected) {
        String actual = "";
        for (int elapsed = 0; elapsed < 4800; elapsed += 5) {
            actual = server.computeOnServer(instance -> {
                final var player = instance.getPlayerList().getPlayers().getFirst();
                final var machine = TabletManager.machineOf(instance,
                        TabletItem.getId(player.getMainHandItem()).orElseThrow());
                return text(machine.terminal());
            });
            if (actual.contains(expected)) return;
            context.waitTicks(5);
        }
        throw new AssertionError("Tablet guest did not produce [" + expected + "]:\n" + actual);
    }

    private static String text(final Terminal terminal) {
        synchronized (terminal) {
            final int start = Math.max(0, terminal.lastRowToDisplayMax - terminal.height) * terminal.width;
            final StringBuilder text = new StringBuilder();
            for (int index = start; index < Math.min(terminal.buffer.length, start + terminal.width * terminal.height); index++) {
                text.appendCodePoint(terminal.buffer[index] > 0 ? terminal.buffer[index] : 32);
                if ((index + 1) % terminal.width == 0) text.append('\n');
            }
            return text.toString();
        }
    }

}
