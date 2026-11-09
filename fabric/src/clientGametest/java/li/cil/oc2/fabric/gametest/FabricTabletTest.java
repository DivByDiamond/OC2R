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
            context.waitTicks(30);
            context.runOnClient(client -> {
                final var screen = (TabletTerminalScreen) RenderCompat.currentScreen();
                screen.getMenu().setCaptureInputState(true);
                if (!screen.getMenu().getCaptureInputState()) {
                    throw new IllegalStateException("Tablet keyboard capture is unavailable");
                }
            });
            context.takeScreenshot("oc2r-tablet-terminal");
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
                final boolean captured = CaptureKeyboardHook.handle(client.getWindow().handle(),
                        CaptureKeyBinding.RELEASE.getDefaultKey().getValue(), 0, GLFW.GLFW_PRESS, 0);
                if (!captured || RenderCompat.currentScreen() != null) {
                    throw new IllegalStateException("The shared release binding did not leave tablet capture");
                }
            });
            context.waitTicks(5);
        }
    }
}
