/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import java.nio.charset.StandardCharsets;
import li.cil.oc2.common.vm.terminal.Terminal;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;

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

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void stoppingComputerMakesItsOwnerImmediatelySaveable(final GameTestHelper helper) {
        final var computer = ComputerFixture.place(helper).blockEntity();
        final var vm = computer.virtualMachine;
        final var chunk = helper.getLevel().getChunkAt(computer.getBlockPos());
        vm.setRunState(li.cil.oc2.common.vm.VMRunState.RUNNING);
        // A previous save consumed the real flag; this tick only queued a lazy VM update.
        //? if >=26.1 {
        /*chunk.tryMarkSaved();
        *///?} else {
        chunk.setUnsaved(false);
        //?}
        li.cil.oc2.common.util.world.chunk.ChunkUtils.setLazyUnsaved(chunk);
        vm.stop();
        TestSupport.assertTrue(helper, "STOPPED transition must eagerly dirty the real owner before unload discards lazy flags",
                chunk.isUnsaved());
        // Repeating stop on a cold machine changes nothing and must not create a new save.
        //? if >=26.1 {
        /*chunk.tryMarkSaved();
        *///?} else {
        chunk.setUnsaved(false);
        //?}
        vm.stop();
        TestSupport.assertTrue(helper, "an already stopped machine must not become dirty again", !chunk.isUnsaved());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void stoppedComputerRetainsItsPrivateRootAcrossContainerReload(final GameTestHelper helper) {
        final var fixture = ComputerFixture.place(helper);
        final var source = fixture.blockEntity();
        final var restored = new li.cil.oc2.common.blockentity.computer.ComputerBlockEntity(
                source.getBlockPos(), source.getBlockState());
        restored.setLevel(helper.getLevel());
        final long offset = 64 * 1024L - 7; // Cross two sparse COW pages without starting a guest OS.
        final byte[] expected = "oc2r-stopped-computer-root".getBytes(StandardCharsets.UTF_8);
        try {
            TestSupport.assertTrue(helper, "fresh computer root must not contain the private marker",
                    !java.util.Arrays.equals(expected, readRootMarker(restored.virtualMachine.state.builtinDevices.rootFilesystem, offset, expected.length)));
            try (var output = source.virtualMachine.state.builtinDevices.rootFilesystem.getOutputStream(offset)) {
                output.write(expected);
            }
            source.virtualMachine.start();
            source.virtualMachine.stop();
            TestSupport.assertTrue(helper, "stop itself must retain the source disk bytes",
                    java.util.Arrays.equals(expected, readRootMarker(source.virtualMachine.state.builtinDevices.rootFilesystem, offset, expected.length)));
            final var saved = fixture.save();
            li.cil.oc2.common.blockentity.computer.persistence.ComputerBlockEntityPersistence.loadAdditional(
                    restored, saved, helper.getLevel().registryAccess());
            TestSupport.assertTrue(helper, "restored computer must remain powered off without a worker",
                    restored.virtualMachine.getRunState() == li.cil.oc2.common.vm.VMRunState.STOPPED
                            && restored.virtualMachine.runner == null && !restored.virtualMachine.state.board.isRunning());
            TestSupport.assertTrue(helper, "stopped computer private disk bytes must survive container save/load",
                    java.util.Arrays.equals(expected, readRootMarker(restored.virtualMachine.state.builtinDevices.rootFilesystem, offset, expected.length)));
            helper.succeed();
        } catch (final java.io.IOException failure) {
            throw TestSupport.failure(helper, "computer root roundtrip failed: " + failure);
        } finally {
            restored.virtualMachine.dispose();
        }
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void shutdownMakesTheRealComputerChunkImmediatelySaveable(final GameTestHelper helper) {
        final var computer = ComputerFixture.place(helper).blockEntity();
        final var vm = computer.virtualMachine;
        final var chunk = helper.getLevel().getChunkAt(computer.getBlockPos());
        vm.runner = vm.createRunner();
        vm.runState = li.cil.oc2.common.vm.VMRunState.RUNNING;
        // The previous save consumed the chunk's real vanilla dirty flag.
        //? if >=26.1 {
        /*chunk.tryMarkSaved();
        *///?} else {
        chunk.setUnsaved(false);
        //?}
        TestSupport.assertTrue(helper, "chunk must start clean", !chunk.isUnsaved());
        vm.prepareForServerShutdown(helper.getLevel().getServer());
        TestSupport.assertTrue(helper, "final CPU save must not depend on a deferred flag surviving unload",
            chunk.isUnsaved());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void shutdownCapturesWorkerProgressAfterAnEarlierSave(final GameTestHelper helper) {
        final ShutdownProbe vm = new ShutdownProbe(helper.getLevel(), false);
        final ShutdownProbe restored = new ShutdownProbe(helper.getLevel(), false);
        try {
            vm.runState = li.cil.oc2.common.vm.VMRunState.RUNNING;
            vm.state.board.setRunning(true);
            vm.serialize(); // The last normal chunk save precedes the final worker completion.
            final long savedTime = vm.state.board.getCpu().getTime();
            vm.pendingWork = true;
            vm.prepareForServerShutdown(helper.getLevel().getServer());
            final long finalTime = vm.state.board.getCpu().getTime();
            TestSupport.assertTrue(helper, "shutdown must join the pending worker before dirtying", vm.dirtyAfterJoin);
            TestSupport.assertTrue(helper, "worker must advance after the earlier save", finalTime > savedTime);
            vm.tick();
            TestSupport.assertEquals(helper, "shutdown must prevent another scheduled tick", 0, vm.scheduledTicks);
            TestSupport.assertTrue(helper, "shutdown must retain guest RUNNING state",
                vm.runState == li.cil.oc2.common.vm.VMRunState.RUNNING && vm.state.board.isRunning());
            restored.deserialize(vm.serialize());
            TestSupport.assertEquals(helper, "final CPU snapshot must resume at the joined state",
                finalTime, restored.state.board.getCpu().getTime());
            helper.succeed();
        } finally {
            vm.dispose();
            restored.dispose();
        }
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void shutdownSkipsInactiveDetachedAndOtherServerMachines(final GameTestHelper helper) {
        final ShutdownProbe vm = new ShutdownProbe(helper.getLevel(), false);
        final ShutdownProbe detached = new ShutdownProbe(null, false);
        final ShutdownProbe cold = new ShutdownProbe(helper.getLevel(), false);
        try {
            cold.prepareForServerShutdown(helper.getLevel().getServer()); // STOPPED.
            cold.start();
            cold.tick();
            TestSupport.assertEquals(helper, "a late tick must not scan/start a cold machine after shutdown",
                0, ((ShutdownProbeController) cold.busController).scans);
            TestSupport.assertTrue(helper, "stopped machines must not dirty a final snapshot", !cold.dirtyAfterJoin);
            vm.runState = li.cil.oc2.common.vm.VMRunState.RUNNING;
            vm.prepareForServerShutdown(null); // A different server identity.
            TestSupport.assertTrue(helper, "another server must not quiesce this machine", !vm.dirtyAfterJoin);
            detached.runState = li.cil.oc2.common.vm.VMRunState.RUNNING;
            detached.prepareForServerShutdown(helper.getLevel().getServer());
            TestSupport.assertTrue(helper, "detached machines must not be quiesced", !detached.dirtyAfterJoin);
            vm.runner = null;
            vm.prepareForServerShutdown(helper.getLevel().getServer());
            TestSupport.assertTrue(helper, "machines without a worker must not dirty a final snapshot", !vm.dirtyAfterJoin);
            helper.succeed();
        } finally {
            vm.dispose();
            detached.dispose();
            cold.dispose();
        }
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void shutdownDoesNotPublishPartiallyConstructedMachines(final GameTestHelper helper) {
        final ShutdownProbe vm = new ShutdownProbe(null, true);
        try {
            TestSupport.assertEquals(helper, "shutdown must not visit a partially initialized owner", 0, vm.ownerReads);
            helper.succeed();
        } finally {
            vm.dispose();
        }
    }

    private static final class ShutdownProbeController extends li.cil.oc2.common.bus.controller.CommonDeviceBusController {
        private int scans;
        ShutdownProbeController() {
            super(new li.cil.oc2.common.bus.device.BlockDeviceBusElement(), 100);
        }
        @Override public void scan() { scans++; }
    }

    /** Isolated VM with a deterministic pending worker, without freezing other parallel tests. */
    private static final class ShutdownProbe extends li.cil.oc2.common.vm.runner.AbstractVirtualMachine {
        private net.minecraft.world.level.Level level;
        private boolean ownerInitialized;
        private boolean pendingWork;
        private boolean dirtyAfterJoin;
        private int scheduledTicks;
        private int ownerReads;

        ShutdownProbe(final net.minecraft.world.level.Level level, final boolean checkPublication) {
            super(new ShutdownProbeController());
            runner = createRunner();
            if (checkPublication) {
                runState = li.cil.oc2.common.vm.VMRunState.RUNNING;
                // Reentrant shutdown emulates another thread observing the constructor boundary.
                // Existing real test VMs belong to a non-null server and are left untouched.
                li.cil.oc2.common.vm.VirtualMachineShutdown.prepare(null);
            }
            this.level = level;
            ownerInitialized = true;
            li.cil.oc2.common.vm.VirtualMachineShutdown.track(this);
        }

        @Override
        protected net.minecraft.world.level.Level getLevelForShutdown() {
            ownerReads++;
            if (!ownerInitialized) throw new IllegalStateException("owner is not initialized");
            return level;
        }

        @Override
        protected void markUnsavedForShutdown() {
            dirtyAfterJoin = !pendingWork;
        }

        @Override
        public boolean consumeEnergy(final int amount, final boolean simulate) { return true; }

        @Override
        public li.cil.oc2.common.vm.runner.AbstractTerminalVMRunner createRunner() {
            return new li.cil.oc2.common.vm.runner.AbstractTerminalVMRunner(this, new Terminal()) {
                @Override protected void sendTerminalDiffToClient(
                    final li.cil.oc2.common.vm.terminal.TerminalDiff.Snapshot snapshot) {}
                @Override public void tick() { scheduledTicks++; }
                @Override public void join() {
                    if (pendingWork) {
                        state.board.step(100);
                        pendingWork = false;
                    }
                }
            };
        }
    }

    // --------------------------------------------------------------------- //

    private static byte[] readRootMarker(final li.cil.sedna.api.device.BlockDevice root,
                                         final long offset, final int length) throws java.io.IOException {
        try (var input = root.getInputStream(offset)) {
            return input.readNBytes(length);
        }
    }

    private TerminalTests() {
    }
}
