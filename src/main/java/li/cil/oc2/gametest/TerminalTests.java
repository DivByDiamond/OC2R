/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;

public final class TerminalTests {
    @GameTest(template = TestSupport.TEMPLATE)
    public static void terminalExistsAfterComputerPlaced(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.place(helper);
        final Terminal terminal = computer.blockEntity().terminalManager.getTerminal();
        TestSupport.assertNotNull(helper, terminal, "terminal after computer placed");
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE)
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

    @GameTest(template = TestSupport.TEMPLATE)
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

    // --------------------------------------------------------------------- //

    private TerminalTests() {
    }
}
