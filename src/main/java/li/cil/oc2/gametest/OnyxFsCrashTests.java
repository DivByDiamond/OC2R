/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.misc.BuiltinDevices;
import li.cil.oc2.common.vm.misc.BuiltinRootFilesystem;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * Power loss against the OnyxFS undo journal on the real bundled OnyxOS guest. The guest runs a
 * workload of metadata operations (mkdir, create and fill a multi-block file, cross-directory and
 * directory rename, overwrite, unlink of a file that needs an indirect block). The built-in root
 * disk is wrapped in a {@link PowerCutDisk} that discards every sector after a budget, the machine
 * is stopped, the disk is saved and loaded into a cold computer, the guest boots again and the
 * result is judged twice: by an OnyxFS reader on the host and by the guest itself.
 *
 * <p>Tunable with system properties. The defaults are one cut point from one workload step, which
 * keeps a routine game test run short; the full sweep used to verify the journal takes
 * {@code -Doc2.onyxfs.cuts=4 -Doc2.onyxfs.torn=0 -Doc2.onyxfs.slice=N/3} for the sector-aligned
 * points and {@code -Doc2.onyxfs.cuts=1 -Doc2.onyxfs.torn=1 -Doc2.onyxfs.slice=1/2} for the torn
 * ones, split across runs because one process cannot hold every boot inside the task timeout.
 * {@code oc2.onyxfs.cuts} is cut points per workload step, {@code oc2.onyxfs.torn} torn-sector
 * points per step, {@code oc2.onyxfs.slice} runs every count-th trial, {@code oc2.onyxfs.only}
 * replays one {@code step:sector:torn} point, and {@code oc2.onyxfs.report} names the result file.
 */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OnyxFsCrashTests {
    private static final Path REPORT = Path.of(System.getProperty("oc2.onyxfs.report", "onyxfs-crash-report.txt"));
    private static final int CUTS_PER_STEP = Integer.getInteger("oc2.onyxfs.cuts", 1);
    private static final int TORN_PER_STEP = Integer.getInteger("oc2.onyxfs.torn", 0);
    private static final String SLICE = System.getProperty("oc2.onyxfs.slice", "0/12");
    /** {@code step:sector:torn} to replay one exact point; empty runs the whole plan. */
    private static final String ONLY = System.getProperty("oc2.onyxfs.only", "");
    private static final int[] TORN_PREFIXES = {1, 256, 508, 511};
    private static final long BOOT_MS = 45_000;
    private static final long COMMAND_MS = 120_000;
    private static final int BIG_FILE_MINIMUM = 48 * 1024;

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE,
        timeoutTicks = 200_000_000)
    public static void onyxFsSurvivesPowerLossDuringTheWorkload(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.placePower(helper, player);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        computer.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get()));
        computer.install(DeviceTypes.CPU, new ItemStack(Items.CPU_TIER_1.get()));
        computer.install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
        final Run run = new Run(helper, computer);
        // A sequence poll, not onEachTick: that would register a callback for every tick of the huge timeout.
        helper.startSequence()
            .thenWaitUntil(() -> TestSupport.assertTrue(helper, "power-loss trials still running", run.tick()))
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //
    // Guest workload and its expected namespace.

    /** What a path may hold: a directory, exact bytes, or any prefix of some bytes. */
    private record Allowed(boolean directory, byte[] data, boolean prefix) {
        static final Allowed DIR = new Allowed(true, new byte[0], false);

        static Allowed exact(final byte[] data) {
            return new Allowed(false, data, false);
        }

        static Allowed prefixOf(final byte[] data) {
            return new Allowed(false, data, true);
        }

        boolean accepts(final Object observed) {
            if (directory) {
                return observed == DIR;
            }
            if (!(observed instanceof byte[] bytes)) {
                return false;
            }
            return prefix ? bytes.length <= data.length && Arrays.equals(bytes, 0, bytes.length, data, 0, bytes.length)
                : Arrays.equals(bytes, data);
        }

        String describe() {
            return directory ? "dir" : (prefix ? "prefix" : "exact") + "(" + data.length + ")";
        }
    }

    private static final Object OBSERVED_DIR = Allowed.DIR;

    private interface Namespace {
        Map<String, Allowed> apply(Map<String, Allowed> before);
    }

    /** One guest command and the namespaces a power cut during it may leave behind. */
    private record Step(String command, Namespace after, Namespace interim) {
    }

    private static Map<String, Allowed> with(final Map<String, Allowed> base, final String path, final Allowed value) {
        final Map<String, Allowed> copy = new TreeMap<>(base);
        copy.put(path, value);
        return copy;
    }

    private static Map<String, Allowed> without(final Map<String, Allowed> base, final String... paths) {
        final Map<String, Allowed> copy = new TreeMap<>(base);
        for (final String path : paths) {
            copy.remove(path);
        }
        return copy;
    }

    private static Map<String, Allowed> renamed(final Map<String, Allowed> base, final String from, final String to) {
        final Map<String, Allowed> copy = new TreeMap<>();
        for (final Map.Entry<String, Allowed> entry : base.entrySet()) {
            final String key = entry.getKey();
            if (key.equals(from) || key.startsWith(from + "/")) {
                copy.put(to + key.substring(from.length()), entry.getValue());
            } else {
                copy.put(key, entry.getValue());
            }
        }
        return copy;
    }

    private static List<Step> workload(final String bigPath, final byte[] big, final String otherPath, final byte[] other) {
        final byte[] first = "first\n".getBytes(StandardCharsets.US_ASCII);
        final byte[] second = "second\n".getBytes(StandardCharsets.US_ASCII);
        final Namespace same = before -> before;
        return List.of(
            new Step("mkdir /w", b -> with(b, "/w", Allowed.DIR), same),
            new Step("mkdir /w/a", b -> with(b, "/w/a", Allowed.DIR), same),
            new Step("mkdir /w/b", b -> with(b, "/w/b", Allowed.DIR), same),
            new Step("cp " + bigPath + " /w/a/big", b -> with(b, "/w/a/big", Allowed.exact(big)),
                b -> with(b, "/w/a/big", Allowed.prefixOf(big))),
            new Step("echo first > /w/a/note", b -> with(b, "/w/a/note", Allowed.exact(first)),
                b -> with(b, "/w/a/note", Allowed.prefixOf(first))),
            new Step("mv /w/a/big /w/b/big", b -> renamed(b, "/w/a/big", "/w/b/big"), same),
            new Step("echo second > /w/a/note", b -> with(b, "/w/a/note", Allowed.exact(second)),
                b -> with(b, "/w/a/note", Allowed.prefixOf(second))),
            new Step("mv /w/b /w/c", b -> renamed(b, "/w/b", "/w/c"), same),
            new Step("rm /w/c/big", b -> without(b, "/w/c/big"), same),
            new Step("cp " + otherPath + " /w/a/big2", b -> with(b, "/w/a/big2", Allowed.exact(other)),
                b -> with(b, "/w/a/big2", Allowed.prefixOf(other))),
            new Step("rm /w/a/note", b -> without(b, "/w/a/note"), same),
            new Step("rm /w/a/big2", b -> without(b, "/w/a/big2"), same));
    }

    // Independent of the workload namespace: the paths the workload touches may legitimately be
    // absent after a cut that landed before the step created them.
    private static final List<String> RECOVERY_CHECKS = List.of("ls /", "ls /etc", "cat /etc/passwd");
    private static final List<String> POST_CRASH_WORK = List.of(
        "mkdir /pc", "echo post > /pc/f", "cat /pc/f", "mkdir /pc/d", "mv /pc/f /pc/d/f");

    // --------------------------------------------------------------------- //
    // Tick-driven scripting.

    private static final class Timeout extends RuntimeException {
        Timeout(final String what) {
            super(what);
        }
    }

    /** Sequential actions and waits, advanced one tick at a time; waits have a wall-clock limit. */
    private static final class Script {
        private record Item(String what, Runnable action, BooleanSupplier condition, long wallMs) {
        }

        private final ArrayDeque<Item> items = new ArrayDeque<>();
        private Item current;
        private long deadline;

        Script then(final Runnable action) {
            items.add(new Item("", action, null, 0));
            return this;
        }

        Script until(final String what, final long wallMs, final BooleanSupplier condition) {
            items.add(new Item(what, null, condition, wallMs));
            return this;
        }

        boolean tick() {
            while (true) {
                if (current == null) {
                    current = items.poll();
                    if (current == null) {
                        return true;
                    }
                    if (current.action() != null) {
                        final Runnable action = current.action();
                        current = null;
                        action.run();
                        continue;
                    }
                    deadline = System.nanoTime() + current.wallMs() * 1_000_000L;
                }
                if (current.condition().getAsBoolean()) {
                    current = null;
                    continue;
                }
                if (System.nanoTime() > deadline) {
                    throw new Timeout(current.what());
                }
                return false;
            }
        }
    }

    // --------------------------------------------------------------------- //

    private static final class Guest {
        private final ComputerFixture computer;
        private int sequence;
        private String sentinel = "";
        private String command = "";

        Guest(final ComputerFixture computer) {
            this.computer = computer;
        }

        /**
         * Clears the screen and writes a unique line into the terminal, so the output of a new boot
         * can be told from the old one and the scrollback cannot drop the marker.
         */
        void mark() {
            sentinel = "@@BOOT" + ++sequence + "@@";
            computer.blockEntity().terminalManager.getTerminal().io.putOutput(ByteBuffer.wrap(
                ("\u001b[2J\u001b[H" + sentinel + "\r\n").getBytes(StandardCharsets.US_ASCII)));
        }

        String text() {
            final String full = fullScreen(computer);
            final int at = full.lastIndexOf(sentinel);
            // A missing sentinel means the scrollback dropped it: report nothing rather than the
            // whole buffer, which would let a stale prompt from the previous boot match.
            return at < 0 || sentinel.isEmpty() ? (sentinel.isEmpty() ? full : "") : full.substring(at + sentinel.length());
        }

        boolean has(final String needle) {
            return text().contains(needle);
        }

        /** Types one command; the shell drops input typed while it is busy, so nothing else is typed. */
        void send(final String line) {
            command = line;
            computer.type(line + "\r");
        }

        private List<String> lines() {
            return Arrays.asList(text().split("\n"));
        }

        /** The command's echoed line is on screen and a bare prompt line has followed it. */
        boolean done() {
            final List<String> lines = lines();
            final int echo = lastEcho(lines);
            if (echo < 0) {
                return false;
            }
            for (int i = echo + 1; i < lines.size(); i++) {
                if (lines.get(i).strip().equals("osh$")) {
                    return true;
                }
            }
            return false;
        }

        private int lastEcho(final List<String> lines) {
            for (int i = lines.size() - 1; i >= 0; i--) {
                if (lines.get(i).strip().equals("osh$ " + command)) {
                    return i;
                }
            }
            return -1;
        }

        /** Everything the last command printed, without the echoed input line and the next prompt. */
        String output() {
            final List<String> lines = lines();
            final StringBuilder out = new StringBuilder();
            for (int i = lastEcho(lines) + 1; i > 0 && i < lines.size(); i++) {
                if (lines.get(i).strip().equals("osh$")) {
                    break;
                }
                out.append(lines.get(i)).append('\n');
            }
            return out.toString();
        }
    }

    // --------------------------------------------------------------------- //

    /** One power cut and what came of it. */
    private static final class Trial {
        final int step;
        final long sector;
        final int torn;
        final int block;
        final String label;
        PowerCutDisk disk;
        byte[] cutImage;
        long pending = -2;
        String failure;
        String screen = "";
        boolean cutHit;
        boolean aborted;
        final long started = System.nanoTime();

        Trial(final int step, final long sector, final int torn, final int block) {
            this.step = step;
            this.sector = sector;
            this.torn = torn;
            this.block = block;
            this.label = "step " + step + " sector " + sector + " (block " + block + ")"
                + (torn > 0 ? " torn@" + torn : "");
        }
    }

    private static final class Run {
        private final GameTestHelper helper;
        private final ComputerFixture computer;
        private final Guest guest;
        private final ArrayDeque<Script> queue = new ArrayDeque<>();
        private Script current;
        private Trial trial;
        private boolean finished;

        private List<Step> steps;
        private final List<byte[]> stepImages = new ArrayList<>();
        private final List<Long> stepStart = new ArrayList<>();
        private final List<Long> stepEnd = new ArrayList<>();
        private int[] trace = new int[0];
        private int dataStart;
        private int journalStart;
        private final List<Map<String, Allowed>> namespaces = new ArrayList<>();
        private PowerCutDisk baselineDisk;
        private long baselineStarted;
        private final java.util.Set<String> knownAnomalies = new java.util.HashSet<>();
        private final List<String> failures = new ArrayList<>();
        private int passed;
        private int sliceCounter;
        private int executed;

        private static IllegalStateException fail(final String message) {
            return new IllegalStateException(message);
        }

        Run(final GameTestHelper helper, final ComputerFixture computer) {
            this.helper = helper;
            this.computer = computer;
            this.guest = new Guest(computer);
            queue.add(baseline());
        }

        boolean tick() {
            if (finished) {
                return true;
            }
            if (current == null) {
                current = queue.poll();
                if (current == null) {
                    finish();
                    return true;
                }
            }
            try {
                if (current.tick()) {
                    current = null;
                }
            } catch (final Timeout timeout) {
                current = null;
                if (trial == null) {
                    throw fail("baseline run stalled waiting for " + timeout.getMessage()
                        + "\n" + fullScreen(computer));
                }
                trial.failure = "stalled waiting for " + timeout.getMessage() + "; " + cpuState(computer)
                    + (trial.disk == null ? "" : ", sectors=" + trial.disk.sectorsWritten()
                        + " requests=" + trial.disk.writeRequests())
                    + diskState();
                trial.screen = fullScreen(computer);
                concludeTrial();
            }
            return false;
        }

        // ----------------------------------------------------------------- //

        private BuiltinDevices devices() {
            return computer.blockEntity().virtualMachine.state.builtinDevices;
        }

        /**
         * Logs in as root until a shell answers. The guest can swallow the name if it is typed before
         * the login prompt starts reading (it is echoed, then ignored), so the name is re-sent while
         * the shell is still missing instead of being typed exactly once.
         */
        private void awaitLogin(final Script script, final String what) {
            final long[] nextAttempt = {0};
            script.until(what + " reaches a root shell", BOOT_MS, () -> {
                if (trial != null && trial.aborted) {
                    return true;
                }
                if (guest.has("osh$")) {
                    return true;
                }
                final long now = System.nanoTime();
                if (now >= nextAttempt[0]) {
                    computer.type("root\r");
                    nextAttempt[0] = now + 2_000_000_000L;
                }
                return false;
            });
        }

        /**
         * Puts a fresh copy-on-write overlay over the packaged base and copies {@code image} into it,
         * then attaches it through a cutting wrapper. Working through the overlay keeps the disk the
         * thing OC2R serializes, so a save/load round trip is the real one.
         */
        private PowerCutDisk resetTo(final byte[] image, final long budget) {
            try {
                final BuiltinDevices devices = devices();
                devices.prepareSavedOnyxRoot(baseImageId());
                final BuiltinRootFilesystem root = rootFilesystem();
                final byte[] current = OnyxFsImage.read(root).raw();
                for (int block = 0; block < image.length / OnyxFsImage.BLOCK; block++) {
                    final int at = block * OnyxFsImage.BLOCK;
                    if (Arrays.equals(current, at, at + OnyxFsImage.BLOCK, image, at, at + OnyxFsImage.BLOCK)) {
                        continue;
                    }
                    try (OutputStream out = root.getOutputStream(at)) {
                        out.write(image, at, OnyxFsImage.BLOCK);
                    }
                }
                final PowerCutDisk disk = new PowerCutDisk(root);
                disk.setBudgetBytes(budget);
                devices.rfs.setBlock(disk, false);
                return disk;
            } catch (final IOException | ReflectiveOperationException failure) {
                throw fail("cannot reset the root disk: " + failure);
            }
        }

        private BuiltinRootFilesystem rootFilesystem() throws ReflectiveOperationException {
            final Field field = BuiltinDevices.class.getDeclaredField("onyxRootFilesystem");
            field.setAccessible(true);
            return (BuiltinRootFilesystem) field.get(devices());
        }

        /** Identity of the immutable image the live overlay sits on; needed to build a fresh one. */
        private String baseImageId() {
            try {
                final BuiltinDevices devices = devices();
                devices.selectRootFilesystem(true);
                final String id = rootFilesystem().getBaseImageId();
                if (id == null) {
                    throw fail("the root disk has no packaged base identity");
                }
                return id;
            } catch (final IOException | ReflectiveOperationException failure) {
                throw fail("cannot identify the packaged root image: " + failure);
            }
        }

        /** The bytes the guest currently sees on its root disk. */
        private byte[] imageBytes(final PowerCutDisk disk) {
            try {
                return OnyxFsImage.read(disk).raw();
            } catch (final IOException failure) {
                throw fail("cannot read the root disk: " + failure);
            }
        }

        /** The bytes of the live, persisted root filesystem (after a load, not the pre-boot copy). */
        private byte[] imageBytes() {
            return image().raw();
        }

        private List<String> newAnomalies(final OnyxFsImage image) {
            final List<String> found = new ArrayList<>(image.anomalies());
            found.removeAll(knownAnomalies);
            return found;
        }

        private OnyxFsImage image() {
            try {
                final BuiltinDevices devices = devices();
                // Creates the OnyxOS overlay on first use; a no-op once a disk is attached.
                devices.selectRootFilesystem(true);
                final Field field = BuiltinDevices.class.getDeclaredField("onyxRootFilesystem");
                field.setAccessible(true);
                return OnyxFsImage.read((BuiltinRootFilesystem) field.get(devices));
            } catch (final IOException | ReflectiveOperationException failure) {
                throw fail("cannot read the root disk: " + failure);
            }
        }

        private Script baseline() {
            final Script script = new Script();
            final long[] startedAt = new long[1];
            script.then(() -> {
                // Boot from a private copy of the shipped image, so every run starts from the same
                // bytes and the test never paints the packaged base.
                final byte[] pristine = imageBytes();
                baselineDisk = resetTo(pristine, Long.MAX_VALUE);
                baselineDisk.startTrace(4_000_000);
                guest.mark();
                startedAt[0] = System.nanoTime();
                baselineStarted = startedAt[0];
                computer.start();
            });
            script.until("the first boot reaches login", BOOT_MS, () -> guest.has("login:"));
            script.then(() -> {
                report("first boot (seeds root account) to login: " + ms(startedAt[0]) + " ms, "
                    + baselineDisk.sectorsWritten() + " sectors written, flushes=" + baselineDisk.flushes());
            });
            awaitLogin(script, "the first boot");
            script.then(() -> {
                report("first boot to shell: " + ms(startedAt[0]) + " ms, " + baselineDisk.sectorsWritten() + " sectors");
                chooseWorkload();
            });
            // The workload itself is appended once the payload files are known.
            script.then(() -> {
                final Script workload = new Script();
                for (int index = 0; index < steps.size(); index++) {
                    final int step = index;
                    workload.then(() -> beginStep(step));
                    workload.until("baseline step " + step + " " + steps.get(step).command(), COMMAND_MS, guest::done);
                    workload.then(() -> endStep(step));
                }
                workload.then(this::finishBaseline);
                queue.addFirst(workload);
            });
            return script;
        }

        private void chooseWorkload() {
            final OnyxFsImage image = new OnyxFsImage(imageBytes(baselineDisk));
            final List<String> anomalies = image.anomalies();
            report("pristine-plus-seed image: blocks=" + image.totalBlocks() + " dataStart=" + image.dataStartBlock()
                + " journal=" + image.journalStartBlock() + "+" + image.journalBlocks()
                + " journalCount=" + image.journalCount() + " anomalies=" + anomalies);
            // Leaked inodes already present in the shipped image are tolerated; anything new is not.
            knownAnomalies.addAll(anomalies);
            dataStart = image.dataStartBlock();
            journalStart = image.journalStartBlock();
            OnyxFsImage.Node bigNode = null;
            OnyxFsImage.Node otherNode = null;
            for (final OnyxFsImage.Node node : image.tree().values()) {
                if (node.directory() || !node.path().startsWith("/bin/") || node.size() < BIG_FILE_MINIMUM) {
                    continue;
                }
                if (bigNode == null || node.size() < bigNode.size()) {
                    bigNode = node;
                }
            }
            if (bigNode == null) {
                throw fail("no /bin file of at least " + BIG_FILE_MINIMUM + " bytes: "
                    + image.tree().keySet());
            }
            final byte[] big = image.content(bigNode);
            for (final OnyxFsImage.Node node : image.tree().values()) {
                if (node.directory() || !node.path().startsWith("/bin/") || node.size() < BIG_FILE_MINIMUM
                    || node.path().equals(bigNode.path())) {
                    continue;
                }
                if (otherNode == null || node.size() < otherNode.size()) {
                    otherNode = node;
                }
            }
            final OnyxFsImage.Node other = otherNode == null ? bigNode : otherNode;
            steps = workload(bigNode.path(), big, other.path(), image.content(other));
            report("payloads: " + bigNode.path() + " " + bigNode.size() + " bytes, " + other.path() + " " + other.size()
                + " bytes; /bin has " + image.tree().keySet().stream().filter(p -> p.startsWith("/bin/")).toList());
            Map<String, Allowed> state = new TreeMap<>();
            namespaces.add(state);
            for (final Step step : steps) {
                state = new TreeMap<>(step.after().apply(state));
                namespaces.add(state);
            }
        }

        private void beginStep(final int index) {
            final long snapshotStarted = System.nanoTime();
            stepImages.add(imageBytes(baselineDisk));
            stepStart.add(baselineDisk.sectorsWritten());
            report("step " + index + " [" + steps.get(index).command() + "] image saved " + ms(snapshotStarted)
                + " ms, heap " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1 << 20) + " MiB");
            guest.send(steps.get(index).command());
        }

        private void endStep(final int index) {
            stepEnd.add(baselineDisk.sectorsWritten());
            final String output = guest.output();
            report("step " + index + " [" + steps.get(index).command() + "] wrote "
                + (stepEnd.get(index) - stepStart.get(index)) + " sectors; output: " + output.strip());
            if (output.contains("osh:")) {
                throw fail("baseline step " + steps.get(index).command() + " failed: " + output);
            }
        }

        private void finishBaseline() {
            trace = baselineDisk.traceBlocks();
            report("baseline workload done: " + trace.length + " sectors in total, wall " + ms(baselineStarted)
                + " ms, flushes=" + baselineDisk.flushes());
            final OnyxFsImage image = new OnyxFsImage(imageBytes(baselineDisk));
            final List<String> problems = new ArrayList<>(newAnomalies(image));
            problems.addAll(compare(image, namespaces.get(steps.size())));
            report("baseline final image: anomalies+mismatches=" + problems + " tree=" + describe(image));
            if (!problems.isEmpty()) {
                throw fail("the uninterrupted baseline is already wrong: " + problems);
            }
            computer.stop();
            queue.addAll(planTrials());
        }

        // ----------------------------------------------------------------- //

        private boolean metadata(final long sector) {
            final int block = trace[(int) sector];
            return block < dataStart;
        }

        private List<Script> planTrials() {
            final List<Trial> plan = new ArrayList<>();
            int tornIndex = 0;
            for (int step = 0; step < steps.size(); step++) {
                final long from = stepStart.get(step);
                final long to = stepEnd.get(step);
                final List<Long> cuts = new ArrayList<>();
                final List<Long> metadataCuts = new ArrayList<>();
                final List<Long> dataCuts = new ArrayList<>();
                final List<Long> headerCuts = new ArrayList<>();
                for (long sector = from; sector < to; sector++) {
                    if (metadata(sector)) {
                        metadataCuts.add(sector - from);
                    } else {
                        dataCuts.add(sector - from);
                    }
                    if (trace[(int) sector] == journalStart && (sector == from || trace[(int) sector - 1] != journalStart)) {
                        headerCuts.add(sector - from);
                    }
                }
                final int length = (int) (to - from);
                if (length <= CUTS_PER_STEP) {
                    for (long n = 0; n < length; n++) {
                        cuts.add(n);
                    }
                } else {
                    final int metadataShare = Math.min(metadataCuts.size(), Math.max(1, CUTS_PER_STEP - CUTS_PER_STEP / 4));
                    spread(metadataCuts, metadataShare, cuts);
                    spread(dataCuts, Math.min(dataCuts.size(), CUTS_PER_STEP - cuts.size()), cuts);
                }
                for (final long n : cuts.stream().distinct().sorted().toList()) {
                    plan.add(new Trial(step, n, 0, trace[(int) (from + n)]));
                }
                final List<Long> tornCandidates = new ArrayList<>(headerCuts);
                tornCandidates.addAll(metadataCuts);
                final List<Long> tornPicks = new ArrayList<>();
                if (!headerCuts.isEmpty()) {
                    spread(headerCuts, Math.min(headerCuts.size(), (TORN_PER_STEP + 1) / 2), tornPicks);
                }
                spread(metadataCuts, Math.min(metadataCuts.size(), TORN_PER_STEP - tornPicks.size()), tornPicks);
                for (final long n : tornPicks.stream().distinct().sorted().toList()) {
                    plan.add(new Trial(step, n, TORN_PREFIXES[tornIndex++ % TORN_PREFIXES.length],
                        trace[(int) (from + n)]));
                }
            }
            final String[] parts = SLICE.split("/");
            final int index = Integer.parseInt(parts[0]);
            final int count = Integer.parseInt(parts[1]);
            final List<Script> scripts = new ArrayList<>();
            int counter = 0;
            for (final Trial candidate : plan) {
                if (!ONLY.isEmpty() && !matchesOnly(candidate)) {
                    continue;
                }
                if (counter++ % count == index) {
                    scripts.add(trialScript(candidate));
                }
            }
            report("trial plan: " + plan.size() + " points, this slice runs " + scripts.size() + " ("
                + plan.stream().filter(t -> t.torn == 0).count() + " sector cuts, "
                + plan.stream().filter(t -> t.torn > 0).count() + " torn)");
            return scripts;
        }

        private static boolean matchesOnly(final Trial trial) {
            final String[] wanted = ONLY.split(":");
            return wanted.length == 3
                && trial.step == Integer.parseInt(wanted[0])
                && trial.sector == Long.parseLong(wanted[1])
                && trial.torn == Integer.parseInt(wanted[2]);
        }

        private static void spread(final List<Long> from, final int count, final List<Long> into) {
            if (count <= 0 || from.isEmpty()) {
                return;
            }
            for (int i = 0; i < count; i++) {
                into.add(from.get((int) ((long) i * from.size() / count)));
            }
        }

        // ----------------------------------------------------------------- //

        private Script trialScript(final Trial planned) {
            final Script script = new Script();
            final long[] bootAt = new long[1];
            script.then(() -> {
                trial = planned;
                // A power cut loses RAM as well as the rest of the disk write: the step is replayed
                // from a cold boot of the disk image taken right before it. The budget stays off
                // during that boot, or the boot's own writes would eat the cut.
                computer.stop();
                planned.disk = resetTo(stepImages.get(planned.step), Long.MAX_VALUE);
                guest.mark();
                computer.start();
            });
            awaitLogin(script, "the step image");
            script.then(() -> {
                // Budgets are relative to the image, not to the writes this replay boot made.
                planned.disk.setBudgetBytes(planned.disk.bytesWritten()
                    + planned.sector * PowerCutDisk.SECTOR + planned.torn);
                guest.send(steps.get(planned.step).command());
            });
            script.until("the workload step runs to its end or loses power", COMMAND_MS, () -> {
                if (planned.disk.isCut()) {
                    planned.cutHit = true;
                    return true;
                }
                return guest.done();
            });
            script.then(() -> {
                // Hard stop: no guest cooperation, no flush; the disk keeps what reached it.
                computer.stop();
                planned.cutImage = imageBytes(planned.disk);
                planned.pending = new OnyxFsImage(planned.cutImage).journalCount();
                // Reload the stopped machine the way OC2R persists it: NBT round trip, then a cold boot.
                final CompoundTag cold = computer.save();
                computer.load(cold);
                if (!Arrays.equals(imageBytes(), planned.cutImage)) {
                    planned.failure = "the persisted root disk differs from the disk at the cut";
                }
                bootAt[0] = System.nanoTime();
                guest.mark();
                computer.start();
            });
            script.until("the recovery boot reaches login", BOOT_MS, () -> guest.has("login:") || guest.has("KERNEL PANIC"));
            script.then(() -> {
                final String text = guest.text();
                if (text.contains("KERNEL PANIC")) {
                    planned.failure = "kernel panic during the recovery boot";
                    planned.screen = text;
                    planned.aborted = true;
                } else {
                    judge(planned, namespacesFor(planned));
                }
            });
            awaitLogin(script, "the recovery boot");
            for (final String check : RECOVERY_CHECKS) {
                addGuestCommand(script, planned, check, true);
            }
            for (final String work : POST_CRASH_WORK) {
                addGuestCommand(script, planned, work, false);
            }
            script.then(() -> {
                if (!planned.aborted) {
                    final OnyxFsImage image = image();
                    final List<String> problems = new ArrayList<>(newAnomalies(image));
                    final Map<String, Object> seen = observe(image);
                    final Object post = seen.get("/pc/d/f");
                    if (!(post instanceof byte[] bytes) || !new String(bytes, StandardCharsets.US_ASCII).equals("post\n")) {
                        problems.add("the post-recovery file /pc/d/f is missing or wrong");
                    }
                    if (seen.get("/pc") != OBSERVED_DIR || seen.get("/pc/d") != OBSERVED_DIR || seen.containsKey("/pc/f")) {
                        problems.add("post-recovery mkdir/mv left a wrong namespace");
                    }
                    if (!problems.isEmpty() && planned.failure == null) {
                        planned.failure = "after post-crash work: " + problems;
                        planned.screen = guest.text();
                    }
                }
                concludeTrial();
            });
            return script;
        }

        private void addGuestCommand(final Script script, final Trial planned, final String command, final boolean readOnly) {
            script.then(() -> {
                if (!planned.aborted) {
                    guest.send(command);
                }
            });
            script.until("guest command " + command, COMMAND_MS, () -> planned.aborted || guest.done());
            script.then(() -> {
                if (planned.aborted) {
                    return;
                }
                final String output = guest.output();
                if (output.contains("osh:") || output.toLowerCase(java.util.Locale.ROOT).contains("error")
                    || output.contains("denied") || output.contains("failed")) {
                    if (planned.failure == null) {
                        planned.failure = "guest command '" + command + "' complained: " + output.strip();
                        planned.screen = guest.text();
                    }
                }
                if (command.equals("cat /pc/f") && !output.contains("post")) {
                    planned.failure = planned.failure == null ? "cat /pc/f lost the data: " + output.strip() : planned.failure;
                    planned.screen = guest.text();
                }
            });
        }

        private List<Map<String, Allowed>> namespacesFor(final Trial planned) {
            final List<Map<String, Allowed>> allowed = new ArrayList<>();
            final Map<String, Allowed> before = namespaces.get(planned.step);
            final Step step = steps.get(planned.step);
            allowed.add(before);
            allowed.add(step.interim().apply(before));
            // A step that was already complete when power went (cut after its last write) is the next state.
            allowed.add(namespaces.get(planned.step + 1));
            return allowed;
        }

        private void judge(final Trial planned, final List<Map<String, Allowed>> allowed) {
            final OnyxFsImage image = image();
            final List<String> problems = new ArrayList<>(newAnomalies(image));
            final int pendingAfter = image.journalCount();
            if (pendingAfter > 0) {
                problems.add("the journal still holds " + pendingAfter + " entries after the recovery boot");
            }
            List<String> best = null;
            for (final Map<String, Allowed> candidate : allowed) {
                final List<String> mismatches = compare(image, candidate);
                if (mismatches.isEmpty()) {
                    best = null;
                    break;
                }
                if (best == null || mismatches.size() < best.size()) {
                    best = mismatches;
                }
            }
            if (best != null) {
                problems.add("namespace matches neither the old nor the new state: " + best);
            }
            if (!problems.isEmpty()) {
                planned.failure = "after recovery boot (journal entries pending at the cut: " + planned.pending + "): " + problems;
                planned.screen = guest.text();
            }
        }

        /** Host-side view of the disk the guest is looking at, for failure reports. */
        private String diskState() {
            try {
                final OnyxFsImage image = image();
                return ", journal=" + image.journalCount() + " " + image.journalProblems()
                    + " anomalies=" + newAnomalies(image);
            } catch (final RuntimeException failure) { // NOPMD: the unreadable-disk message is returned into the failure report, not swallowed
                return ", disk unreadable: " + failure.getMessage();
            }
        }

        private void concludeTrial() {
            final Trial done = trial;
            trial = null;
            final String where = done.label + (done.cutHit ? "" : " (step completed before the budget ran out)")
                + ", journal entries pending at the cut " + done.pending + ", " + ms(done.started) + " ms";
            if (done.failure == null) {
                passed++;
                report("PASS " + where);
            } else {
                final String line = "FAIL " + where + ": " + done.failure;
                failures.add(line);
                report(line + "\n--- screen ---\n" + done.screen + "\n--- end screen ---");
            }
        }

        private void finish() {
            finished = true;
            report("SUMMARY: " + passed + " passed, " + failures.size() + " failed of " + (passed + failures.size())
                + " trials; flushes seen by the device during the baseline: " + baselineDisk.flushes());
            if (!failures.isEmpty()) {
                throw fail(failures.size() + " power-loss trials failed, first: " + failures.get(0));
            }
        }

        // ----------------------------------------------------------------- //

        private static List<String> compare(final OnyxFsImage image, final Map<String, Allowed> expected) {
            final Map<String, Object> seen = observe(image);
            final List<String> mismatches = new ArrayList<>();
            for (final Map.Entry<String, Allowed> entry : expected.entrySet()) {
                final Object actual = seen.get(entry.getKey());
                if (!entry.getValue().accepts(actual)) {
                    mismatches.add(entry.getKey() + " expected " + entry.getValue().describe() + " but is "
                        + (actual == null ? "absent" : actual == OBSERVED_DIR ? "dir" : ((byte[]) actual).length + " bytes"));
                }
            }
            for (final String path : seen.keySet()) {
                if (!expected.containsKey(path) && (path.startsWith("/w") || path.startsWith("/pc"))) {
                    mismatches.add(path + " exists but is not expected");
                }
            }
            return mismatches;
        }

        /** Paths under the workload directories with their kind and content. */
        private static Map<String, Object> observe(final OnyxFsImage image) {
            final Map<String, Object> seen = new TreeMap<>();
            for (final OnyxFsImage.Node node : image.tree().values()) {
                if (!(node.path().equals("/w") || node.path().startsWith("/w/") || node.path().equals("/pc")
                    || node.path().startsWith("/pc/"))) {
                    continue;
                }
                seen.put(node.path(), node.directory() ? OBSERVED_DIR : image.content(node));
            }
            return seen;
        }

        private static String describe(final OnyxFsImage image) {
            final StringBuilder out = new StringBuilder();
            for (final Map.Entry<String, Object> entry : observe(image).entrySet()) {
                out.append(entry.getKey()).append(entry.getValue() == OBSERVED_DIR ? "/ " : "(" + ((byte[]) entry.getValue()).length + ") ");
            }
            return out.toString();
        }
    }

    // --------------------------------------------------------------------- //

    private static long ms(final long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /** CPU liveness: a halted core with a frozen retired count means the guest never resumes. */
    private static String cpuState(final ComputerFixture computer) {        final var cpu = computer.blockEntity().virtualMachine.state.board.getCpu();
        return "cpu(halted=" + cpu.isHalted() + ", retired=" + cpu.getInstructionsRetired() + ")";
    }

    /** The whole terminal buffer including scrollback, trailing blanks removed. */
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    static String fullScreen(final ComputerFixture computer) {
        final Terminal terminal = computer.blockEntity().terminalManager.getTerminal();
        final StringBuilder text = new StringBuilder();
        synchronized (terminal) {
            final int width = terminal.width;
            final int rows = Math.min(terminal.lastRowToDisplayMax + 1, terminal.buffer.length / Math.max(1, width));
            for (int row = 0; row < rows; row++) {
                final StringBuilder line = new StringBuilder();
                for (int col = 0; col < width; col++) {
                    final int value = terminal.buffer[row * width + col];
                    line.append(value == 0 ? ' ' : (value < 0x20 || value > 0x7E) ? '?' : (char) value);
                }
                text.append(line.toString().stripTrailing()).append('\n');
            }
        }
        return text.toString();
    }

    static void report(final String line) {
        try (OutputStream out = Files.newOutputStream(REPORT, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (final IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
