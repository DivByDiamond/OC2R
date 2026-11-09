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
                screen.showGraphics(false);
                screen.getMenu().setCaptureInputState(true);
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
            context.runOnClient(client -> ((TabletTerminalScreen) RenderCompat.currentScreen()).showGraphics(true));
            context.waitFor(client -> {
                final var screen = (TabletTerminalScreen) RenderCompat.currentScreen();
                final byte[] pixels = TabletClientView.find(screen.getMenu().tabletId()).orElseThrow().framebuffer();
                final int offset = (102 * 256 + 18) * 2;
                return (pixels[offset] & 255 | (pixels[offset + 1] & 255) << 8) == 0x2457;
            }, 2400);
            // Allow the guest's first row-by-row presentation to finish below the button.
            context.waitTicks(10);
            context.takeScreenshot("oc2r-tablet-widgets");
            context.runOnClient(client -> pointer(true));
            context.waitTicks(2);
            context.runOnClient(client -> pointer(false));
            waitForGuest(context, world.getServer(), "OTABLET_CLICK=1");
            context.waitFor(client -> {
                final var screen = (TabletTerminalScreen) RenderCompat.currentScreen();
                final byte[] pixels = TabletClientView.find(screen.getMenu().tabletId()).orElseThrow().framebuffer();
                final int offset = (70 * 256 + 16) * 2;
                return (pixels[offset] & 255 | (pixels[offset + 1] & 255) << 8) == 0x07e0;
            }, 2400);
            context.runOnClient(client -> ((TabletTerminalScreen) RenderCompat.currentScreen()).showGraphics(false));
            context.takeScreenshot("oc2r-tablet-console-after-click");
            context.runOnClient(client -> ((TabletTerminalScreen) RenderCompat.currentScreen()).showGraphics(true));
            context.waitTicks(10);
            context.takeScreenshot("oc2r-tablet-widgets-after-click");
            world.getServer().runOnServer(server -> {
                final var player = server.getPlayerList().getPlayers().getFirst();
                final var machine = TabletManager.machineOf(server, TabletItem.getId(player.getMainHandItem()).orElseThrow());
                final boolean found = machine.virtualMachine().busController.getDevices().stream()
                        .anyMatch(device -> device instanceof RPCDevice rpc
                                && rpc.getTypeNames().contains("tablet") && rpc.getTypeNames().contains("gps_tower")
                                && rpc.getMethodGroups().stream().anyMatch(group -> group.getName().equals("sendRadioMessage")));
                if (!found) throw new IllegalStateException("Messenger-compatible tablet radio is missing from the guest bus");
            });
            context.runOnClient(client -> ((TabletTerminalScreen) RenderCompat.currentScreen()).getMenu().switchToInventory());
            context.waitFor(client -> RenderCompat.currentScreen() instanceof TabletInventoryScreen, 100);
            context.runOnClient(client -> {
                final var screen = (TabletInventoryScreen) RenderCompat.currentScreen();
                if (!screen.getMenu().getSlot(0).getItem().is(Items.CPU_TIER_1.get())) {
                    throw new IllegalStateException("Tablet CPU slot was not synchronized");
                }
            });
            context.takeScreenshot("oc2r-tablet-inventory");
            context.runOnClient(client -> ((TabletInventoryScreen) RenderCompat.currentScreen()).getMenu().switchToTerminal());
            context.waitFor(client -> RenderCompat.currentScreen() instanceof TabletTerminalScreen, 100);
            context.runOnClient(client -> {
                final var screen = (TabletTerminalScreen) RenderCompat.currentScreen();
                screen.showGraphics(true);
                screen.getMenu().setCaptureInputState(true);
                if (!CaptureKeyboardHook.handle(client.getWindow().handle(), GLFW.GLFW_KEY_Q, 0, GLFW.GLFW_PRESS, 0)
                        || !CaptureKeyboardHook.handle(client.getWindow().handle(), GLFW.GLFW_KEY_Q, 0, GLFW.GLFW_RELEASE, 0)) {
                    throw new AssertionError("Guest Q was not routed through captured VirtIO keyboard input");
                }
            });
            waitForGuest(context, world.getServer(), "OTABLET_EXIT");
            context.runOnClient(client -> ((TabletTerminalScreen) RenderCompat.currentScreen()).showGraphics(false));
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
    private static void pointer(final boolean down) {
        final var screen = (TabletTerminalScreen) RenderCompat.currentScreen();
        final var area = screen.viewport();
        final var event = new MouseButtonEvent(area.x() + 30, area.y() + 98, new MouseButtonInfo(0, 0));
        if (down) {
            // Older batched motion must not replay behind this immediate button edge.
            screen.mouseMoved(area.x() + 220, area.y() + 150);
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
