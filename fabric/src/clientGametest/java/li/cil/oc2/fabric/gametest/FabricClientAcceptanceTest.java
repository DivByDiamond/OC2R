package li.cil.oc2.fabric.gametest;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.client.RenderCompat;
import li.cil.oc2.client.model.FabricBusCableModel;
import li.cil.oc2.client.model.FabricMonitorModel;
import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.video.VideoCodec;
import li.cil.oc2.common.vm.handler.VMItemStackHandlers;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.lwjgl.glfw.GLFW;

/** Real client/server acceptance: boot bundled Linux, deliver guest pixels, render GUIs and restore the running VM. */
public final class FabricClientAcceptanceTest implements FabricClientGameTest {
    private static final BlockPos COMPUTER = new BlockPos(0, -59, 0);
    private static final BlockPos MONITOR = new BlockPos(2, -58, 1);
    private static final BlockPos PROJECTOR = new BlockPos(4, -59, 2);
    private static final String MARKER = "OC2R_FABRIC26_GUEST_READY";

    @Override
    public void runTest(final ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            client.options.pauseOnLostFocus = false;
            client.options.enableVsync().set(false);
            client.options.renderDistance().set(3);
            client.options.simulationDistance().set(5);
            client.options.framerateLimit().set(30);
        });
        final TestWorldSave save;
        try (var world = context.worldBuilder().create()) {
            save = world.getWorldSave();
            world.getServer().runOnServer(FabricClientAcceptanceTest::setup);
            world.getServer().runCommand("tp @a 2.5 -58 -5 0 10");
            context.waitFor(client -> client.level != null
                    && client.level.getBlockEntity(MONITOR) instanceof MonitorBlockEntity, 600);
            context.waitTicks(40);
            context.runOnClient(FabricClientAcceptanceTest::assertModels);
            context.takeScreenshot("oc2r-fabric26-models");

            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                    .setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.ROBOT.get())));
            context.waitTicks(30);
            context.takeScreenshot("oc2r-fabric26-robot-item");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                    .setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                            new ItemStack(Items.NETWORK_INTERFACE_CARD.get())));
            context.waitTicks(10);
            context.setScreen(() -> new li.cil.oc2.client.gui.screen.network.NetworkInterfaceCardScreen(
                    Minecraft.getInstance().player, net.minecraft.world.InteractionHand.MAIN_HAND));
            context.waitTicks(30);
            context.takeScreenshot("oc2r-fabric26-nic-preview");
            context.setScreen(() -> new li.cil.oc2.manual.client.gui.ManualScreen(
                    li.cil.oc2.client.manual.Manuals.MANUAL.get(),
                    li.cil.oc2.client.manual.ModManualStyle.INSTANCE,
                    li.cil.oc2.client.manual.ModManualScreenStyle.INSTANCE));
            context.waitTicks(30);
            context.takeScreenshot("oc2r-fabric26-manual");
            context.setScreen(() -> null);

            final String preboot = world.getServer().computeOnServer(server ->
                    "state=" + computer(server).virtualMachine.getRunState()
                            + ", bus=" + computer(server).virtualMachine.getBusState()
                            + ", energy=" + computer(server).energy.getEnergyStored()
                            + ", devices=" + computer(server).virtualMachine.busController.getDevices());
            org.apache.logging.log4j.LogManager.getLogger("OC2RClientAcceptance").info(
                    "Models, robot item, NIC preview and manual rendered; preboot " + preboot);
            waitForServer(context, world.getServer(), server -> computer(server).energy.getEnergyStored() > 0
                    && computer(server).virtualMachine.getBusState() == li.cil.oc2.common.bus.controller.BusState.READY, 600);
            world.getServer().runOnServer(server -> computer(server).terminalManager.start());
            org.apache.logging.log4j.LogManager.getLogger("OC2RClientAcceptance").info("Starting bundled Linux guest");
            waitForGuest(context, world.getServer(), "login:", 2400);
            type(world.getServer(), "root\r");
            waitForGuest(context, world.getServer(), "# ", 1200);
            type(world.getServer(), "echo " + MARKER + "\r");
            waitForGuest(context, world.getServer(), MARKER, 1200);
            // The guest writes both its own terminal and the MMIO framebuffers; no host-side image injection.
            type(world.getServer(), "OC2R_RAM_TOKEN=fabric26_live; echo fabric26_disk >/root/oc2r-fabric26-marker; "
                    + "printf '\\033[2J\\033[H\\033[44;97mFABRIC 26 CLIENT ACCEPTANCE\\033[0m\\n'; "
                    + "lua -e 'for n=0,1 do local p=\"/sys/class/graphics/fb\"..n; "
                    + "local s=io.open(p..\"/virtual_size\"); if s then local w,h=s:read(\"*a\"):match(\"(%d+),(%d+)\"); "
                    + "s:close(); local f=assert(io.open(\"/dev/fb\"..n,\"wb\")); "
                    + "w,h=tonumber(w),tonumber(h); for y=0,h-1 do local row={}; "
                    + "for x=0,w-1 do row[#row+1]=((math.floor(x/8)+math.floor(y/8))%2==0) "
                    + "and string.char(0,248) or string.char(255,255) end; f:write(table.concat(row)) end; "
                    + "f:close() end end'; "
                    + "echo OC2R_FRAMEBUFFERS_WRITTEN\r");
            waitForGuest(context, world.getServer(), "OC2R_FRAMEBUFFERS_WRITTEN", 1200);
            waitForServer(context, world.getServer(), server -> {
                final var level = server.overworld();
                return ((MonitorBlockEntity) level.getBlockEntity(MONITOR)).isMounted()
                        && ((MonitorBlockEntity) level.getBlockEntity(MONITOR)).hasPower()
                        && ((ProjectorBlockEntity) level.getBlockEntity(PROJECTOR)).isProjecting();
            }, 600);
            context.waitFor(FabricClientAcceptanceTest::monitorReceivedGuestFrame, 1200);
            context.waitTicks(60);
            context.takeScreenshot("oc2r-fabric26-guest-displays");

            // Hold the guest framebuffer still and compare the same monitor at two exact distances.
            // NORTH-facing monitors display on their north (negative Z) face.
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("tp @a 2.5 -59.125 0 0 0");
            context.waitTicks(8);
            context.takeScreenshot("oc2r-fabric26-monitor-distance-1");
            world.getServer().runCommand("tp @a 2.5 -59.125 -1 0 0");
            context.waitTicks(8);
            context.takeScreenshot("oc2r-fabric26-monitor-distance-2");
            for (int frame = 1; frame <= 3; frame++) {
                context.waitTicks(10);
                context.takeScreenshot("oc2r-fabric26-monitor-distance-2-repeat-" + frame);
            }
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("tp @a 2.5 -58 -5 0 10");
            context.waitTicks(8);

            world.getServer().runOnServer(server -> computer(server).terminalManager.openTerminalScreen(
                    server.getPlayerList().getPlayers().getFirst()));
            context.waitForScreen(li.cil.oc2.client.gui.screen.computer.ComputerTerminalScreen.class);
            context.waitTicks(40);
            context.takeScreenshot("oc2r-fabric26-computer-terminal");
            context.runOnClient(client -> {
                GLFW.glfwSetClipboardString(client.getWindow().handle(), "echo PASTE_CTRL_V\r");
                RenderCompat.currentScreen().keyPressed(
                        new KeyEvent(GLFW.GLFW_KEY_V, 0, GLFW.GLFW_MOD_CONTROL));
            });
            waitForGuest(context, world.getServer(), "PASTE_CTRL_V", 1200);
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().closeContainer());
            context.setScreen(() -> null);
            world.getServer().runOnServer(server -> ((MonitorBlockEntity) server.overworld().getBlockEntity(MONITOR))
                    .openTerminalScreen(server.getPlayerList().getPlayers().getFirst()));
            context.waitTicks(40);
            context.waitForScreen(li.cil.oc2.client.gui.screen.monitor.MonitorDisplayScreen.class);
            context.takeScreenshot("oc2r-fabric26-monitor-gui");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().closeContainer());
            context.setScreen(() -> null);
            world.getServer().runOnServer(server -> server.overworld().getEntitiesOfClass(
                    li.cil.oc2.common.entity.Robot.class, new net.minecraft.world.phys.AABB(-5, -64, -5, 5, -50, 5))
                    .getFirst().openTerminalScreen(server.getPlayerList().getPlayers().getFirst()));
            context.waitForScreen(li.cil.oc2.client.gui.screen.robot.RobotTerminalScreen.class);
            context.waitTicks(30);
            context.takeScreenshot("oc2r-fabric26-robot-terminal");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().closeContainer());
            context.setScreen(() -> null);
            // A real chest contributes animated block-entity geometry to the projector depth pass.
            world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(
                    new BlockPos(4, -59, 5), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState()));
            context.waitTicks(40);
            context.takeScreenshot("oc2r-fabric26-projector-chest-occluder");
            // Every codec must carry a changed guest picture to the client, including switches between codecs.
            final int originalCodec = li.cil.oc2.common.config.Config.videoCodec;
            try {
                final int[][] palettes = {
                        {0x07E0, 0x001F}, {0xFFE0, 0x07FF}, {0xF81F, 0x0000}, {0x07E0, 0xF800}};
                final VideoCodec[] codecs = {VideoCodec.RAW, VideoCodec.H264, VideoCodec.DELTA, VideoCodec.H264};
                for (int i = 0; i < codecs.length; i++) {
                    li.cil.oc2.common.config.Config.videoCodec = codecs[i].id;
                    drawFramebuffers(context, world.getServer(), palettes[i][0], palettes[i][1], i);
                    final int colorA = palettes[i][0];
                    final int colorB = palettes[i][1];
                    try {
                        context.waitFor(client -> monitorShows(client, colorA, colorB), 600);
                    } catch (final AssertionError failure) {
                        final String seen = context.computeOnClient(client -> monitorPixels(client));
                        throw new AssertionError("Codec " + codecs[i] + " did not deliver the guest picture; wanted "
                                + Integer.toHexString(colorA) + "/" + Integer.toHexString(colorB)
                                + ", client sees " + seen, failure);
                    }
                    org.apache.logging.log4j.LogManager.getLogger("OC2RClientAcceptance").info(
                            "Monitor shows the guest picture through codec {}", codecs[i]);
                }
            } finally {
                li.cil.oc2.common.config.Config.videoCodec = originalCodec;
            }
            for (final String fixture : new String[]{"upstream-cursor-80", "upstream-colors-80"}) {
                replayVttest(context, world.getServer(), fixture);
            }
            placeLongWire(world.getServer());
            context.waitTicks(40);
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("tp @a 7 -58 13 0 5");
            context.waitTicks(20);
            context.takeScreenshot("oc2r-fabric26-wire-sag");
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("tp @a 2.5 -58 -5 0 10");
            world.getServer().runOnServer(server -> {
                if (computer(server).virtualMachine.getRunState() != VMRunState.RUNNING) {
                    throw new AssertionError("Guest stopped before save");
                }
                server.saveEverything(false, true, true);
            });
        }
        // Reopen a real saved world and ask the resumed shell for both volatile and disk-backed markers.
        try (var world = save.open()) {
            context.waitTicks(80);
            type(world.getServer(), "printf 'RAM=%s DISK=' \"$OC2R_RAM_TOKEN\"; cat /root/oc2r-fabric26-marker\r");
            waitForGuest(context, world.getServer(), "RAM=fabric26_live DISK=fabric26_disk", 1800);
            // The last codec round left green and red tiles on the guest framebuffers.
            context.waitFor(client -> monitorShows(client, 0x07E0, 0xF800), 1200);
            context.takeScreenshot("oc2r-fabric26-restored-world");
        }
    }

    private static void drawFramebuffers(final ClientGameTestContext context, final TestServerContext server,
                                         final int colorA, final int colorB, final int round) {
        type(server, "lua -e 'local a,b=" + (colorA & 0xFF) + "," + (colorA >>> 8) + ";local c,d=" + (colorB & 0xFF)
                + "," + (colorB >>> 8) + ";for n=0,1 do local p=\"/sys/class/graphics/fb\"..n; "
                + "local s=io.open(p..\"/virtual_size\"); if s then local w,h=s:read(\"*a\"):match(\"(%d+),(%d+)\"); "
                + "s:close(); local f=assert(io.open(\"/dev/fb\"..n,\"wb\")); "
                + "w,h=tonumber(w),tonumber(h); for y=0,h-1 do local row={}; "
                + "for x=0,w-1 do row[#row+1]=((math.floor(x/8)+math.floor(y/8))%2==0) "
                + "and string.char(a,b) or string.char(c,d) end; f:write(table.concat(row)) end; "
                + "f:close() end end'; echo OC2R_FB_$((1+1))_" + round + "\r");
        waitForGuest(context, server, "OC2R_FB_2_" + round, 1200);
    }

    /** True once the client monitor texture shows the two tile colors (RGB565) within lossy-codec tolerance. */
    private static boolean monitorShows(final Minecraft client, final int colorA, final int colorB) {
        if (client.level == null || !(client.level.getBlockEntity(MONITOR) instanceof final MonitorBlockEntity monitor)
                || !monitor.isMounted() || !monitor.hasPower()) return false;
        final MonitorGUIRenderer gui = (MonitorGUIRenderer) monitor.getMonitor();
        final var view = gui.getRenderer(monitor);
        try {
            final var location = view.getTexture();
            if (location == null) return false;
            final var texture = client.getTextureManager().getTexture(location);
            if (!(texture instanceof final DynamicTexture dynamic) || dynamic.getPixels() == null) return false;
            final var pixels = dynamic.getPixels();
            if (pixels.getWidth() != 320 || pixels.getHeight() != 200) return false;
            return closeTo(pixels.getPixel(12, 12), colorA) && closeTo(pixels.getPixel(12, 148), colorB);
        } finally {
            gui.releaseRenderer(view);
        }
    }

    private static String monitorPixels(final Minecraft client) {
        if (client.level == null || !(client.level.getBlockEntity(MONITOR) instanceof final MonitorBlockEntity monitor)) {
            return "no monitor";
        }
        final MonitorGUIRenderer gui = (MonitorGUIRenderer) monitor.getMonitor();
        final var view = gui.getRenderer(monitor);
        try {
            final var location = view.getTexture();
            if (location == null) return "no texture";
            final var texture = client.getTextureManager().getTexture(location);
            if (!(texture instanceof final DynamicTexture dynamic) || dynamic.getPixels() == null) return "no pixels";
            final var pixels = dynamic.getPixels();
            return pixels.getWidth() + "x" + pixels.getHeight() + " a=" + Integer.toHexString(pixels.getPixel(12, 12))
                    + " b=" + Integer.toHexString(pixels.getPixel(12, 148));
        } finally {
            gui.releaseRenderer(view);
        }
    }

    private static boolean closeTo(final int argb, final int rgb565) {
        final int red = ((rgb565 >> 11) & 0x1F) * 255 / 31;
        final int green = ((rgb565 >> 5) & 0x3F) * 255 / 63;
        final int blue = (rgb565 & 0x1F) * 255 / 31;
        return Math.abs(((argb >> 16) & 0xFF) - red) <= 48
                && Math.abs(((argb >> 8) & 0xFF) - green) <= 48
                && Math.abs((argb & 0xFF) - blue) <= 48;
    }

    private static void placeLongWire(final TestServerContext server) {
        server.runOnServer(instance -> {
            final ServerLevel level = instance.overworld();
            final var state = Blocks.NETWORK_CONNECTOR.get().defaultBlockState()
                    .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
            final BlockPos a = new BlockPos(0, -59, 20);
            final BlockPos b = new BlockPos(14, -59, 20);
            for (final BlockPos pos : new BlockPos[]{a, b}) {
                level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(pos, state);
            }
            final var result = NetworkConnectorBlockEntity.connect(
                    (NetworkConnectorBlockEntity) level.getBlockEntity(a),
                    (NetworkConnectorBlockEntity) level.getBlockEntity(b));
            if (result != li.cil.oc2.common.blockentity.network.connector.interfaces.ConnectionResult.SUCCESS) {
                throw new AssertionError("Long wire was not connected: " + result);
            }
        });
    }

    /** Feeds a recorded vttest stream through the real guest tty and compares the host terminal with the golden. */
    private static void replayVttest(final ClientGameTestContext context, final TestServerContext server,
                                     final String fixture) {
        final java.nio.file.Path root = fixtureRoot().resolve(fixture);
        final byte[] stream;
        final java.util.List<String> expected;
        try {
            stream = java.nio.file.Files.readAllBytes(root.resolve("stream.bin"));
            expected = java.nio.file.Files.readAllLines(root.resolve("screen.expected"));
        } catch (final java.io.IOException e) {
            throw new AssertionError("Cannot read vttest fixture " + fixture, e);
        }
        type(server, "stty -echo -opost; PS1=; rm -f /tmp/vt.bin\r");
        context.waitTicks(20);
        final int chunk = 48;
        for (int offset = 0; offset < stream.length; offset += chunk) {
            final StringBuilder line = new StringBuilder("printf '");
            for (int i = offset; i < Math.min(stream.length, offset + chunk); i++) {
                line.append(String.format("\\%03o", stream[i] & 0xFF));
            }
            line.append("' >>/tmp/vt.bin\r");
            type(server, line.toString());
            if ((offset / chunk) % 8 == 7) context.waitTicks(4);
        }
        // Let the guest finish consuming the typed input, then replay the file raw onto the terminal.
        context.waitTicks(100);
        type(server, "clear; cat /tmp/vt.bin\r");
        String previous = "";
        int stable = 0;
        for (int elapsed = 0; elapsed < 1200 && stable < 6; elapsed += 10) {
            context.waitTicks(10);
            final String now = server.computeOnServer(instance -> screen(computer(instance).terminalManager.getTerminal()));
            stable = now.equals(previous) && elapsed > 20 ? stable + 1 : 0;
            previous = now;
        }
        final String[] actual = previous.split("\n", -1);
        final StringBuilder diff = new StringBuilder();
        for (int row = 0; row < expected.size(); row++) {
            final String want = expected.get(row).stripTrailing();
            final String got = row < actual.length ? actual[row].stripTrailing() : "";
            if (!want.equals(got)) diff.append("row ").append(row).append(": want [").append(want)
                    .append("] got [").append(got).append("]\n");
        }
        type(server, "stty echo opost; PS1='# '\r");
        context.waitTicks(20);
        if (!diff.isEmpty()) throw new AssertionError("vttest " + fixture + " differs in the real guest:\n" + diff);
        org.apache.logging.log4j.LogManager.getLogger("OC2RClientAcceptance").info(
                "vttest {} matches the golden screen in the real guest", fixture);
    }

    private static java.nio.file.Path fixtureRoot() {
        java.nio.file.Path dir = java.nio.file.Path.of("").toAbsolutePath();
        while (dir != null) {
            final java.nio.file.Path candidate = dir.resolve("src/test/resources/vttest");
            if (java.nio.file.Files.isDirectory(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new AssertionError("vttest fixtures not found above the working directory");
    }

    private static ComputerBlockEntity computer(final MinecraftServer server) {
        return (ComputerBlockEntity) server.overworld().getBlockEntity(COMPUTER);
    }

    private static void waitForServer(final ClientGameTestContext context, final TestServerContext server,
                                      final java.util.function.Predicate<MinecraftServer> predicate, final int timeout) {
        for (int elapsed = 0; elapsed < timeout; elapsed += 5) {
            if (server.computeOnServer(predicate::test)) return;
            context.waitTicks(5);
        }
        throw new IllegalStateException("Server condition not satisfied after " + timeout + " ticks");
    }

    private static void waitForGuest(final ClientGameTestContext context, final TestServerContext server,
                                     final String expected, final int timeout) {
        try {
            waitForServer(context, server, instance -> {
                final var computer = computer(instance);
                if (computer.virtualMachine.getRunState() == VMRunState.STOPPED) {
                    throw new IllegalStateException("Guest stopped: " + computer.virtualMachine.getError());
                }
                final var error = computer.virtualMachine.getBootError();
                if (error != null && !error.getString().isBlank()) {
                    throw new IllegalStateException("Guest boot error: " + error.getString());
                }
                return screen(computer.terminalManager.getTerminal()).contains(expected);
            }, timeout);
        } catch (final RuntimeException failure) {
            final String diagnostic = server.computeOnServer(instance -> {
                final ComputerBlockEntity computer = computer(instance);
                return "state=" + computer.virtualMachine.getRunState() + ", bus="
                        + computer.virtualMachine.getBusState() + ", energy=" + computer.energy.getEnergyStored()
                        + ", bootError=" + computer.virtualMachine.getBootError() + ", error="
                        + computer.virtualMachine.getError() + ", devices="
                        + computer.virtualMachine.busController.getDevices() + "\n"
                        + screen(computer.terminalManager.getTerminal());
            });
            throw new AssertionError("Guest did not produce [" + expected + "]: " + diagnostic, failure);
        }
    }

    private static void type(final TestServerContext server, final String text) {
        server.runOnServer(instance -> computer(instance).terminalManager.getTerminal().io.putInput(text));
    }

    private static String screen(final Terminal terminal) {
        synchronized (terminal) {
            final int start = Math.max(0, terminal.lastRowToDisplayMax - terminal.height) * terminal.width;
            final StringBuilder result = new StringBuilder();
            for (int i = start; i < Math.min(terminal.buffer.length, start + terminal.width * terminal.height); i++) {
                result.appendCodePoint(terminal.buffer[i] > 0 ? terminal.buffer[i] : 32);
                if ((i + 1) % terminal.width == 0) result.append('\n');
            }
            return result.toString();
        }
    }

    private static boolean monitorReceivedGuestFrame(final Minecraft client) {
        if (client.level == null || !(client.level.getBlockEntity(MONITOR) instanceof final MonitorBlockEntity monitor)
                || !monitor.isMounted() || !monitor.hasPower()) return false;
        final MonitorGUIRenderer gui = (MonitorGUIRenderer) monitor.getMonitor();
        final var view = gui.getRenderer(monitor);
        try {
            final var location = view.getTexture();
            if (location == null) return false;
            final var texture = client.getTextureManager().getTexture(location);
            if (!(texture instanceof final DynamicTexture dynamic) || dynamic.getPixels() == null) return false;
            if (dynamic.getPixels().getWidth() != 320 || dynamic.getPixels().getHeight() != 200) return false;
            final var pixels = dynamic.getPixels();
            final int red = pixels.getPixel(10, 10);
            final int white = pixels.getPixel(10, 150);
            return (red & 0x00FFFFFF) == 0x00FF0000
                    && (white & 0x00FFFFFF) == 0x00FFFFFF;
        } finally {
            gui.releaseRenderer(view);
        }
    }

    private static void assertModels(final Minecraft client) {
        final var models = client.getModelManager().getBlockStateModelSet();
        if (!(models.get(client.level.getBlockState(MONITOR)) instanceof FabricMonitorModel)) {
            throw new AssertionError("Monitor does not use its Fabric dynamic model");
        }
        for (final BlockPos pos : java.util.List.of(new BlockPos(2, -59, 1), new BlockPos(-3, -59, 1))) {
            final var state = client.level.getBlockState(pos);
            final var model = models.get(state);
            if (!(model instanceof FabricBusCableModel)) {
                throw new AssertionError("Bus cable does not use its Fabric dynamic model at " + pos);
            }
            final var parts = new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
            model.collectParts(net.minecraft.util.RandomSource.create(0), parts);
            int expected = state.getValue(BusCableStateProperties.HAS_CABLE) ? 1 : 0;
            for (final Direction side : Direction.values()) {
                if (state.getValue(BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(side)) != ConnectionType.NONE) {
                    expected++;
                }
            }
            if (parts.size() != expected) {
                throw new AssertionError("Missing cable links/plugs at " + pos + ": expected " + expected
                        + " model parts, got " + parts.size());
            }
        }
    }

    static void setup(final MinecraftServer server) {
        final ServerLevel level = server.overworld();
        level.setBlockAndUpdate(COMPUTER, Blocks.COMPUTER.get().defaultBlockState());
        level.setBlockAndUpdate(COMPUTER.below(), Blocks.CREATIVE_ENERGY.get().defaultBlockState());
        level.setBlockAndUpdate(MONITOR, Blocks.MONITOR.get().defaultBlockState());
        level.setBlockAndUpdate(MONITOR.above(), Blocks.CREATIVE_ENERGY.get().defaultBlockState());
        level.setBlockAndUpdate(PROJECTOR, Blocks.PROJECTOR.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(PROJECTOR.below(), Blocks.CREATIVE_ENERGY.get().defaultBlockState());
        for (int x = 0; x <= 4; x++) {
            final BlockPos pos = new BlockPos(x, -59, 1);
            level.setBlockAndUpdate(pos, Blocks.BUS_CABLE.get().defaultBlockState()
                    .setValue(BusCableStateProperties.HAS_CABLE, true));
        }
        for (int x = 0; x <= 4; x++) {
            final BlockPos pos = new BlockPos(x, -59, 1);
            level.setBlockAndUpdate(pos, BusCableStateProperties.recomputeConnections(level, pos, level.getBlockState(pos)));
        }
        interfaceAt(level, new BlockPos(0, -59, 1), Direction.NORTH);
        interfaceAt(level, new BlockPos(2, -59, 1), Direction.UP);
        interfaceAt(level, new BlockPos(4, -59, 1), Direction.SOUTH);
        level.setBlockAndUpdate(new BlockPos(-3, -59, 1), Blocks.BUS_CABLE.get().defaultBlockState()
                .setValue(BusCableStateProperties.HAS_CABLE, false)
                .setValue(BusCableStateProperties.CONNECTION_SOUTH, ConnectionType.INTERFACE));
        // Separate camouflage fixture, so facade geometry does not hide one of the live bus interfaces.
        final BlockPos facadePos = new BlockPos(-2, -59, 1);
        level.setBlockAndUpdate(facadePos, Blocks.BUS_CABLE.get().defaultBlockState());
        ((BusCableBlockEntity) level.getBlockEntity(facadePos))
                .setFacade(new ItemStack(net.minecraft.world.item.Items.STONE));
        for (int x = 0; x <= 8; x++) for (int y = -59; y <= -53; y++) {
            level.setBlockAndUpdate(new BlockPos(x, y, 8), net.minecraft.world.level.block.Blocks.POLISHED_DIORITE.defaultBlockState());
        }
        final var handlers = computer(server).getItemStackHandlers();
        slot(handlers, DeviceTypes.CPU, 0, Items.CPU_TIER_INF.get());
        for (int i = 0; i < 4; i++) slot(handlers, DeviceTypes.MEMORY, i, Items.MEMORY_LARGE.get());
        slot(handlers, DeviceTypes.FLASH_MEMORY, 0, Items.FLASH_MEMORY_CUSTOM.get());
        slot(handlers, DeviceTypes.GPU, 0, Items.GPU_TIER_1.get());
        slot(handlers, DeviceTypes.CARD, 0, Items.NETWORK_INTERFACE_CARD.get());
        final var connectorState = Blocks.NETWORK_CONNECTOR.get().defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
        final BlockPos left = COMPUTER.above();
        final BlockPos right = new BlockPos(6, -59, 0);
        level.setBlockAndUpdate(left, connectorState);
        level.setBlockAndUpdate(right, connectorState);
        NetworkConnectorBlockEntity.connect((NetworkConnectorBlockEntity) level.getBlockEntity(left),
                (NetworkConnectorBlockEntity) level.getBlockEntity(right));
        final var robot = Entities.ROBOT.get().create(level, EntitySpawnReason.COMMAND);
        if (robot == null) throw new AssertionError("Robot could not be created");
        robot.snapTo(-2.5, -59, 0.5, 180, 0);
        level.addFreshEntity(robot);
        ((MonitorBlockEntity) level.getBlockEntity(MONITOR)).start();
    }

    private static void slot(final VMItemStackHandlers handlers, final li.cil.oc2.api.bus.device.DeviceType type,
                             final int index, final Item item) {
        handlers.getItemHandler(type).orElseThrow().setStackInSlot(index, new ItemStack(item));
    }

    private static void interfaceAt(final ServerLevel level, final BlockPos pos, final Direction face) {
        level.setBlockAndUpdate(pos, level.getBlockState(pos)
                .setValue(BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(face), ConnectionType.INTERFACE));
        BusCableStateProperties.onConnectionTypeChanged(level, pos, face, true);
    }
}
