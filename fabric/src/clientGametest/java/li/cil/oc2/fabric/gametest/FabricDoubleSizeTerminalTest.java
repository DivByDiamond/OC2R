package li.cil.oc2.fabric.gametest;

import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

/** Repeatable Linux/UART double-size acceptance plus isolated world row-cache invalidation. */
public final class FabricDoubleSizeTerminalTest implements FabricClientGameTest {
    private static final BlockPos COMPUTER = new BlockPos(0, -59, 0);

    @Override
    public void runTest(final ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            client.options.pauseOnLostFocus = false;
            TerminalPixelGoldens.hideHud(client);
            client.options.guiScale().set(2);
            client.getWindow().setWindowed(854, 480);
            client.resizeGui();
            client.options.renderDistance().set(3);
            client.options.fov().set(30);
            client.options.gamma().set(0.0);
            client.options.enableVsync().set(false);
            client.options.framerateLimit().set(30);
        });
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(FabricClientAcceptanceTest::setup);
            world.getServer().runOnServer(server -> {
                final var rules = server.overworld().getGameRules();
                rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, server);
                rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER, false, server);
            });
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 0.5 -60 -1.8 0 0");
            for (int elapsed = 0; elapsed < 600; elapsed += 5) {
                if (world.getServer().computeOnServer(server -> computer(server).energy.getEnergyStored() > 0
                        && computer(server).virtualMachine.getBusState()
                                == li.cil.oc2.common.bus.controller.BusState.READY)) break;
                context.waitTicks(5);
            }
            world.getServer().runOnServer(server -> computer(server).terminalManager.start());
            for (int elapsed = 0; ; elapsed += 20) {
                if (elapsed > 2400) throw new IllegalStateException("guest never reached login");
                if (world.getServer().computeOnServer(server -> screen(computer(server)
                        .terminalManager.getTerminal()).contains("login:"))) break;
                context.waitTicks(20);
            }
            type(world.getServer(), "root\r");
            waitFor(context, world.getServer(), "# ", 1200);
            type(world.getServer(), "printf '\\033[2J\\033[H\\033#3DOUBLE HEIGHT\\n\\033#4DOUBLE HEIGHT\\n"
                    + "\\033#6Double width \\033[1;31mred\\033[0m \\033[4mul\\033[0m \\033[9mstrike\\033[0m\\n"
                    + "\\033#6\\033(0lqqqqqk\\033(B boxes\\n"
                    + "\\033#5Normal 0123456789 0123456789 0123456789 0123456789 END\\n'\r");
            context.waitFor(client -> client.level != null
                    && client.level.getBlockEntity(COMPUTER) instanceof ComputerBlockEntity computer
                    && computer.terminalManager.getTerminal().lineAttrOfScreenRow(0)
                            == li.cil.oc2.common.vm.terminal.Terminal.LINE_ATTR_DOUBLE_HEIGHT_TOP
                    && computer.terminalManager.getTerminal().lineAttrOfScreenRow(2)
                            == li.cil.oc2.common.vm.terminal.Terminal.LINE_ATTR_DOUBLE_WIDTH, 600);
            context.waitTicks(40);
            context.takeScreenshot("oc2r-doublesize-world");
            checkAttributeOnlyRebuild(context);
            TerminalPixelGoldens.check(context.takeScreenshot("oc2r-doublesize-world-attribute-only"),
                    "world", 247, 117, 360, 210);
            context.runOnClient(client -> output(((ComputerBlockEntity) client.level.getBlockEntity(COMPUTER))
                    .terminalManager.getTerminal(), "\033#6"));
            context.waitTicks(10);
            world.getServer().runOnServer(server -> computer(server).terminalManager.openTerminalScreen(
                    server.getPlayerList().getPlayers().getFirst()));
            context.waitForScreen(li.cil.oc2.client.gui.screen.computer.ComputerTerminalScreen.class);
            context.waitTicks(40);
            context.takeScreenshot("oc2r-doublesize-gui");
            context.runOnClient(client -> {
                final var terminal = ((ComputerBlockEntity) client.level.getBlockEntity(COMPUTER))
                        .terminalManager.getTerminal();
                output(terminal, "\033[3;40H\033[2 q");
            });
            context.waitTicks(10);
            TerminalPixelGoldens.check(context.takeScreenshot("oc2r-doublesize-gui-cursor"),
                    "gui", 106, 48, 640, 384);
        }
    }

    /** Exercise the existing world's cached renderer without network apply's full-dirty fallback. */
    private static void checkAttributeOnlyRebuild(final ClientGameTestContext context) {
        final Object[] oldMesh = new Object[1];
        final byte[][] oldVertices = new byte[1][];
        final li.cil.oc2.common.vm.terminal.render.TerminalRenderer[] renderer =
                new li.cil.oc2.common.vm.terminal.render.TerminalRenderer[1];
        final int[][] cells = new int[1][];
        context.runOnClient(client -> {
            final var terminal = ((ComputerBlockEntity) client.level.getBlockEntity(COMPUTER))
                    .terminalManager.getTerminal();
            // Pick the renderer already used by the block, before opening any GUI.
            renderer[0] = terminal.renderers.stream()
                    .filter(li.cil.oc2.common.vm.terminal.render.TerminalRenderer.class::isInstance)
                    .map(li.cil.oc2.common.vm.terminal.render.TerminalRenderer.class::cast)
                    .findFirst().orElseThrow(() -> new AssertionError("world renderer was not created"));
            output(terminal, "\033[3;40H\033[2 q");
        });
        context.waitTicks(10);
        context.runOnClient(client -> {
            final var terminal = renderer[0].terminal;
            oldMesh[0] = rowMesh(renderer[0], 2);
            if (oldMesh[0] == null) throw new AssertionError("world row was not cached");
            oldVertices[0] = meshBytes(oldMesh[0]).clone();
            cells[0] = terminal.buffer.clone();
            if (renderer[0].dirty.get() != 0) throw new AssertionError("cache did not settle");
            // Already double width: switching to DHL changes no cells or cursor coordinates.
            output(terminal, "\033#3");
            if (!java.util.Arrays.equals(cells[0], terminal.buffer)) {
                throw new AssertionError("attribute-only change modified cells");
            }
            if ((renderer[0].dirty.get() & 4L) == 0) throw new AssertionError("row not invalidated");
        });
        context.waitTicks(10);
        context.runOnClient(client -> {
            final Object updated = rowMesh(renderer[0], 2);
            if (updated == oldMesh[0]) throw new AssertionError("stale world row mesh");
            if (java.util.Arrays.equals(oldVertices[0], meshBytes(updated))) {
                throw new AssertionError("attribute change rebuilt identical world vertices");
            }
            if (renderer[0].dirty.get() != 0) throw new AssertionError("world did not rebuild dirty row");
        });
    }

    // Test-only reflection keeps cache internals out of the public production API.
    private static Object rowMesh(final Object renderer, final int row) {
        try {
            final var field = renderer.getClass().getDeclaredField("lines");
            field.setAccessible(true);
            return ((Object[]) field.get(renderer))[row];
        } catch (final ReflectiveOperationException exception) {
            throw new AssertionError("cannot inspect world row cache", exception);
        }
    }

    private static byte[] meshBytes(final Object mesh) {
        try {
            final var accessor = mesh.getClass().getDeclaredMethod("data");
            accessor.setAccessible(true);
            return (byte[]) accessor.invoke(mesh);
        } catch (final ReflectiveOperationException exception) {
            throw new AssertionError("cannot inspect world row vertices", exception);
        }
    }

    private static void output(final li.cil.oc2.common.vm.terminal.Terminal terminal, final String text) {
        terminal.io.putOutput(java.nio.ByteBuffer.wrap(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static void type(final net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server,
            final String text) {
        server.runOnServer(instance -> computer(instance).terminalManager.getTerminal().io.putInput(text));
    }

    private static void waitFor(final ClientGameTestContext context,
            final net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server,
            final String expected, final int timeout) {
        for (int elapsed = 0; elapsed <= timeout; elapsed += 5) {
            if (server.computeOnServer(instance -> screen(computer(instance)
                    .terminalManager.getTerminal()).contains(expected))) return;
            context.waitTicks(5);
        }
        throw new IllegalStateException("never saw " + expected);
    }

    private static String screen(final li.cil.oc2.common.vm.terminal.Terminal terminal) {
        synchronized (terminal) {
            final int start = Math.max(0, terminal.lastRowToDisplayMax - terminal.height) * terminal.width;
            final StringBuilder result = new StringBuilder();
            for (int i = start; i < Math.min(terminal.buffer.length, start + terminal.width * terminal.height); i++) {
                result.appendCodePoint(terminal.buffer[i] > 0 ? terminal.buffer[i] : 32);
            }
            return result.toString();
        }
    }

    private static ComputerBlockEntity computer(final MinecraftServer server) {
        return (ComputerBlockEntity) server.overworld().getBlockEntity(COMPUTER);
    }
}
