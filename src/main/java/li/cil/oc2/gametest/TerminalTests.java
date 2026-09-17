/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import java.nio.charset.StandardCharsets;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TerminalTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void terminalExistsAfterComputerPlaced(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.place(helper);
        final Terminal terminal = computer.blockEntity().terminalManager.getTerminal();
        TestSupport.assertNotNull(helper, terminal, "terminal after computer placed");
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void terminalBufferWritesHello(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.place(helper);
        final Terminal terminal = computer.blockEntity().terminalManager.getTerminal();

        // Write directly through the buffer writer so the test does not depend on VM tick / guest OS.
        for (final char ch : "hello".toCharArray()) {
            terminal.bufferWriter.putChar(ch);
        }

        computer.assertScreenContains("hello", "terminal buffer should contain hello");
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void terminalSurvivesComputerStart(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.placePower(helper, player);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        computer.start();

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                final Terminal terminal = computer.blockEntity().terminalManager.getTerminal();
                TestSupport.assertNotNull(helper, terminal, "terminal after computer start");
                // Write a marker and verify it appears — proves the terminal object is still writable after start().
                for (final char ch : "hello".toCharArray()) {
                    terminal.bufferWriter.putChar(ch);
                }
                computer.assertScreenContains("hello", "terminal after start should still buffer hello");
            })
            .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void terminalDiffRecordsShiftOpsAtCapacity(final GameTestHelper helper) {
        // The server-integration pin for the shift-op wire protocol: driving a REAL block
        // entity's terminal to absolute capacity must accumulate resolved shift geometry in
        // the network sink (the client replays it to keep its scrolled-back scrollback exact).
        final ComputerFixture computer = ComputerFixture.place(helper);
        final Terminal terminal = computer.blockEntity().terminalManager.getTerminal();

        helper.startSequence()
            .thenExecute(() -> {
                final byte[] line = "\n".getBytes(StandardCharsets.UTF_8);
                final int capacity = Terminal.HEIGHT * Terminal.SCROLL_BACK_COUNT;
                // Saturate the scrollback, then one more line: exactly one whole-buffer shift.
                for (int i = 0; i <= capacity; i++) {
                    terminal.io.putOutput(java.nio.ByteBuffer.wrap(line));
                }
            })
            .thenExecute(() -> {
                final Terminal.NetworkDirty dirty = terminal.consumeNetworkDirty();
                TestSupport.assertTrue(helper, "at-capacity linefeed must record a shift op for the wire",
                        dirty.shiftOps().length >= 5);
                TestSupport.assertTrue(helper, "the recorded op must shift a nonzero number of rows",
                        dirty.shiftOps()[2] > 0);
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private TerminalTests() {
    }
}
