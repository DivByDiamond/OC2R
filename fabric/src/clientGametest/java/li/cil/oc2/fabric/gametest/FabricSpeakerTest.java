package li.cil.oc2.fabric.gametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import li.cil.oc2.client.audio.PcmSoundBuffer;
import li.cil.oc2.client.audio.SoundClientManager;
import li.cil.oc2.client.audio.StreamingPcmSoundInstance;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.misc.SpeakerBlockEntity;
import li.cil.oc2.common.util.sound.DfpwmDecoder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;

/**
 * Speaker chain in a real client: server {@code writeDfpwm}, decoder, {@code SoundCardPcmMessage}, and the client's
 * {@link PcmSoundBuffer}. Nothing is audible (null OpenAL driver); the checks are on the data that reached the
 * client's stream and on how much of it the sound engine read.
 */
public final class FabricSpeakerTest implements FabricClientGameTest {
    private static final BlockPos SPEAKER = new BlockPos(0, -59, 0);
    private static final int RATE = 48000;
    private static final long PCM_BYTES_PER_SECOND = 44100L * 2;

    /** Counts what is added to and polled from one client buffer, and keeps what was added. */
    private static final class CountingQueue extends ConcurrentLinkedQueue<byte[]> {
        final AtomicInteger addedChunks = new AtomicInteger();
        final AtomicLong addedBytes = new AtomicLong();
        final AtomicLong polledBytes = new AtomicLong();
        final List<byte[]> captured = new ArrayList<>();

        @Override
        public boolean add(final byte[] chunk) {
            addedChunks.incrementAndGet();
            addedBytes.addAndGet(chunk.length);
            synchronized (captured) {
                captured.add(chunk);
            }
            return super.add(chunk);
        }

        @Override
        public byte[] poll() {
            final byte[] chunk = super.poll();
            if (chunk != null) {
                polledBytes.addAndGet(chunk.length);
            }
            return chunk;
        }

        void addPlain(final byte[] chunk) {
            super.add(chunk);
            synchronized (captured) {
                captured.add(chunk);
            }
        }

        byte[] capturedBytes() {
            synchronized (captured) {
                int total = 0;
                for (final byte[] c : captured) total += c.length;
                final byte[] out = new byte[total];
                int off = 0;
                for (final byte[] c : captured) {
                    System.arraycopy(c, 0, out, off, c.length);
                    off += c.length;
                }
                return out;
            }
        }
    }

    private static final class Hook {
        PcmSoundBuffer buffer;
        StreamingPcmSoundInstance instance;
        CountingQueue queue;
        long baseBytes; // bytes that had reached the buffer before it was hooked
        int baseChunks; // chunks that were still queued when it was hooked
    }

    private final List<Hook> hooks = new CopyOnWriteArrayList<>();

    @Override
    public void runTest(final ClientGameTestContext context) {
        context.runOnClient(client -> {
            // The test client uses the OpenAL null device, so a non-zero volume is silent but lets the sound
            // engine open the source and read the stream (a zero volume would never start it).
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.5);
            client.options.pauseOnLostFocus = false;
            client.options.renderDistance().set(3);
            client.options.framerateLimit().set(30);
            client.options.enableVsync().set(false);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> hookCurrent());
        try (var world = context.worldBuilder().create()) {
            final Server srv = action -> world.getServer().runOnServer(action::accept);
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("tp @a 2.5 -58 0.5");
            world.getServer().runOnServer(server ->
                    server.overworld().setBlockAndUpdate(SPEAKER, Blocks.SPEAKER.get().defaultBlockState()));
            context.waitFor(client -> client.level != null
                    && client.level.getBlockState(SPEAKER).is(Blocks.SPEAKER.get()), 200);
            context.waitTicks(20);

            final byte[] square = square(8192);

            // Phase 1: one chunk, exact PCM compared against a reference decode.
            final int expectedPcm = DfpwmDecoder.pcmLength(square.length, RATE);
            final byte[] reference = new DfpwmDecoder().decode(square, RATE);
            final long t0 = System.nanoTime();
            if (!write(srv, square)) throw new AssertionError("first write refused");
            log("phase1 write accepted, pcm=" + expectedPcm + " bytes, bufferedMs(server)=" + buffered(srv));
            // The instance is created by the production path; hook its queue once it exists.
            context.waitFor(client -> currentInstance() != null && !hooks.isEmpty(), 100);
            final int bufferedAfterWrite = buffered(srv);
            if (bufferedAfterWrite < 1200 || bufferedAfterWrite > 1400)
                throw new AssertionError("bufferedMs after one 8192 byte chunk was " + bufferedAfterWrite);
            context.waitTicks(10);
            final long afterHalfSecond = sample("phase1 +0.5s");
            // wait for the reader to drain it
            context.waitTicks(30);
            sample("phase1 +2s");
            context.waitTicks(20);
            final long[] done = sampleTotals("phase1 +3s");
            final long wallMs = (System.nanoTime() - t0) / 1_000_000;
            final Hook first = hooks.getFirst();
            final byte[] got = first.queue.capturedBytes();
            final long received = first.buffer.size();
            log("phase1 client received " + received + " of " + expectedPcm + " expected bytes in "
                    + totalChunks() + " messages (expected " + (expectedPcm + 4095) / 4096 + ") after "
                    + wallMs + " ms, engine read " + done[1] + ", serverBufferedMs=" + buffered(srv)
                    + ", hooked after " + first.baseBytes + " bytes");
            if (received != expectedPcm)
                throw new AssertionError("client got " + received + " bytes, expected " + expectedPcm);
            final int compareFrom = (int) (received - got.length);
            final byte[] tail = Arrays.copyOfRange(reference, compareFrom, reference.length);
            if (!Arrays.equals(tail, got)) throw new AssertionError("client PCM differs from reference decode");
            if (got.length < 4096 * 10) throw new AssertionError("too little PCM captured to compare: " + got.length);
            log("phase1 the last " + got.length + " bytes the client got (from offset " + compareFrom
                    + ") equal the reference decoder output");
            if (done[1] < expectedPcm - 2 * 4096)
                throw new AssertionError("sound engine read only " + done[1] + " of " + expectedPcm);
            if (afterHalfSecond <= 0) throw new AssertionError("engine had not read anything after 0.5 s");
            if (buffered(srv) != 0) throw new AssertionError("server buffer did not drain: " + buffered(srv));

            // Phase 2: fill the queue until refused, then wait for acceptance again.
            context.waitTicks(40);
            final byte[] small = square(3000);
            int accepted = 0;
            final long beforeFill = totalAdded();
            final long fillStart = System.nanoTime();
            boolean refused = false;
            for (int i = 0; i < 10; i++) {
                if (write(srv, small)) {
                    accepted++;
                } else {
                    refused = true;
                    break;
                }
            }
            final int bufferedAtRefusal = buffered(srv);
            log("phase2 accepted " + accepted + " chunks of 3000 bytes, then refused=" + refused
                    + ", serverBufferedMs=" + bufferedAtRefusal);
            if (!refused || accepted < 2) throw new AssertionError("queue never refused");
            int ticksToAccept = 0;
            boolean again = false;
            while (ticksToAccept < 100) {
                context.waitTicks(1);
                ticksToAccept++;
                if (write(srv, small)) {
                    again = true;
                    break;
                }
            }
            log("phase2 accepted again after " + ticksToAccept + " ticks ("
                    + (System.nanoTime() - fillStart) / 1_000_000 + " ms since the fill began), serverBufferedMs="
                    + buffered(srv));
            if (!again) throw new AssertionError("write was never accepted again");
            context.waitTicks(10);
            log("phase2 client received " + (totalAdded() - beforeFill) + " bytes for "
                    + (accepted + 1) + " accepted writes (" + (accepted + 1) * DfpwmDecoder.pcmLength(3000, RATE)
                    + " expected)");
            if (totalAdded() - beforeFill != (long) (accepted + 1) * DfpwmDecoder.pcmLength(3000, RATE))
                throw new AssertionError("client byte count does not match the accepted writes");

            // Phase 3: sustained playback for 10 s; the client must not receive much more than real time.
            context.waitTicks(60);
            final long sustainedStartBytes = totalAdded();
            final int sustainedStartChunks = totalChunks();
            final long sustainedStartRead = totalRead();
            final long sustainStart = System.nanoTime();
            int sustainAccepted = 0;
            int sustainRefused = 0;
            int maxBuffered = 0;
            int minBufferedAfterWrite = Integer.MAX_VALUE;
            long expectedSustained = 0;
            for (int tick = 0; tick < 200; tick++) {
                context.waitTicks(1);
                if (buffered(srv) < 1000) {
                    if (write(srv, small)) {
                        sustainAccepted++;
                        expectedSustained += DfpwmDecoder.pcmLength(3000, RATE);
                    } else {
                        sustainRefused++;
                    }
                }
                final int b = buffered(srv);
                maxBuffered = Math.max(maxBuffered, b);
                minBufferedAfterWrite = Math.min(minBufferedAfterWrite, b);
                }
            final double seconds = (System.nanoTime() - sustainStart) / 1e9;
            final long receivedBytes = totalAdded() - sustainedStartBytes;
            final int receivedChunks = totalChunks() - sustainedStartChunks;
            final long readBytes = totalRead() - sustainedStartRead;
            log(String.format("phase3 %.2f s: %d writes accepted (%d bytes), %d refused, client received %d bytes in %d"
                            + " chunks (%.1f chunks/s, %.0f bytes/s vs %d real-time), read %d bytes, serverBufferedMs"
                            + " max=%d",
                    seconds, sustainAccepted, expectedSustained, sustainRefused, receivedBytes, receivedChunks,
                    receivedChunks / seconds, receivedBytes / seconds, PCM_BYTES_PER_SECOND, readBytes, maxBuffered));
            if (receivedBytes != expectedSustained)
                throw new AssertionError("client bytes " + receivedBytes + " != sent " + expectedSustained);
            if (receivedBytes > (seconds + 2.0) * PCM_BYTES_PER_SECOND)
                throw new AssertionError("client received more than real time plus the 2 s queue");
            if (readBytes < receivedBytes - 3 * 44100)
                throw new AssertionError("engine reads lag the data by more than the expected queue");

            // Phase 4: silence, the stream goes stale, then a fresh write opens a new stream.
            context.waitTicks(100);
            final long readAfterIdle = totalRead();
            final long addedAfterIdle = totalAdded();
            final int streamsBefore = hooks.size();
            final boolean stoppedOld = lastHook().instance.isStopped();
            log("phase4 after 5 s idle: written=" + addedAfterIdle + " read=" + readAfterIdle
                    + ", old instance stopped=" + stoppedOld + ", serverBufferedMs=" + buffered(srv));
            if (!stoppedOld) throw new AssertionError("idle stream was not stopped by the client");
            final long beforeSecond = totalAdded();
            if (!write(srv, small)) throw new AssertionError("write after idle refused");
            context.waitTicks(5);
            context.waitTicks(30);
            final Hook second = lastHook();
            log("phase4 new write after idle: streams " + streamsBefore + " -> " + hooks.size()
                    + ", new buffer received " + second.buffer.size()
                    + " bytes, total read=" + totalRead());
            if (hooks.size() != streamsBefore + 1) throw new AssertionError("no new stream after idle");

            // Phase 5: the Charger-style model and texture, seen from all sides. The
            // screenshots land in the run directory for review; the model check is
            // the automated part (full cube, no missing textures, front/side/top).
            context.runOnClient(FabricSpeakerTest::assertSpeakerModel);
            for (final String[] view : VIEWS) {
                world.getServer().runCommand("tp @a " + view[1]);
                context.waitTicks(15);
                context.takeScreenshot("oc2r-speaker-" + view[0]);
            }
            context.runOnClient(client -> SoundClientManager.stopAll());
        }
    }

    /** Camera poses around the speaker at (0, -59, 0): name and absolute tp arguments. */
    private static final String[][] VIEWS = {
            {"north", "0.5 -58.3 -2.5 0 5"},
            {"east", "2.5 -58.3 0.5 90 5"},
            {"south", "0.5 -58.3 2.5 180 5"},
            {"west", "-2.5 -58.3 0.5 -90 5"},
            {"top", "0.5 -57.2 0.6 0 80"},
    };

    // ---- model ----

    private static void assertSpeakerModel(final net.minecraft.client.Minecraft client) {
        final net.minecraft.world.level.block.state.BlockState state =
                Blocks.SPEAKER.get().defaultBlockState();
        final var model = client.getModelManager().getBlockStateModelSet().get(state);
        final var parts = new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
        model.collectParts(net.minecraft.util.RandomSource.create(0), parts);
        int quads = 0;
        boolean front = false, side = false, top = false;
        final double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        final double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        final java.util.List<net.minecraft.core.Direction> sides =
                new java.util.ArrayList<>(java.util.List.of(net.minecraft.core.Direction.values()));
        sides.add(null);
        for (final var part : parts) {
            for (final net.minecraft.core.Direction facing : sides) {
                for (final var quad : part.getQuads(facing)) {
                    quads++;
                    for (int vertex = 0; vertex < 4; vertex++) {
                        final var position = quad.position(vertex);
                        final double[] xyz = {position.x(), position.y(), position.z()};
                        for (int axis = 0; axis < 3; axis++) {
                            min[axis] = Math.min(min[axis], xyz[axis]);
                            max[axis] = Math.max(max[axis], xyz[axis]);
                        }
                    }
                    final String name = quad.materialInfo().sprite().contents().name().toString();
                    if (name.contains("missingno")) throw new AssertionError("Missing texture for speaker");
                    front |= name.equals("oc2r:block/speaker/front");
                    side |= name.equals("oc2r:block/speaker/side");
                    top |= name.equals("oc2r:block/speaker/top");
                }
            }
        }
        // A plain full cube: the model must agree with the block's collision/selection shape.
        final var shape = state.getShape(client.level, SPEAKER).bounds();
        final double[] expectedMin = {shape.minX, shape.minY, shape.minZ};
        final double[] expectedMax = {shape.maxX, shape.maxY, shape.maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(min[axis] - expectedMin[axis]) > 0.00001
                    || Math.abs(max[axis] - expectedMax[axis]) > 0.00001)
                throw new AssertionError("Speaker model and selection box disagree on axis " + axis);
        }
        if (quads < 6 || !front || !side || !top)
            throw new AssertionError("Incomplete speaker model: " + quads + " quads, front="
                    + front + ", side=" + side + ", top=" + top);
        log("phase5 speaker model OK: " + quads + " quads, full cube, front/side/top textures present");
    }

    // ---- server side ----

    /** Runs code on the server thread; adapts the game test server context. */
    private interface Server {
        void runOnServer(java.util.function.Consumer<net.minecraft.server.MinecraftServer> action);
    }

    private static boolean write(final Server world,
                                 final byte[] data) {
        final boolean[] result = new boolean[1];
        world.runOnServer(server -> result[0] =
                ((SpeakerBlockEntity) server.overworld().getBlockEntity(SPEAKER)).writeDfpwm(data, RATE));
        return result[0];
    }

    private static int buffered(final Server world) {
        final int[] result = new int[1];
        world.runOnServer(server ->
                result[0] = ((SpeakerBlockEntity) server.overworld().getBlockEntity(SPEAKER)).getBufferedMs());
        return result[0];
    }

    private static byte[] square(final int length) {
        final byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (((i / 64) & 1) == 0 ? 0xFF : 0x00);
        }
        return data;
    }

    // ---- client side ----

    @SuppressWarnings("unchecked")
    private static StreamingPcmSoundInstance currentInstance() {
        try {
            final Field field = SoundClientManager.class.getDeclaredField("PCM_STREAMS");
            field.setAccessible(true);
            return ((Map<BlockPos, StreamingPcmSoundInstance>) field.get(null)).get(SPEAKER);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void hookCurrent() {
        final StreamingPcmSoundInstance instance = currentInstance();
        if (instance == null) return;
        final PcmSoundBuffer buffer = instance.getBuffer();
        if (!hooks.isEmpty() && lastHook().buffer == buffer) return;
        try {
            final Field chunks = PcmSoundBuffer.class.getDeclaredField("chunks");
            chunks.setAccessible(true);
            final Hook hook = new Hook();
            hook.buffer = buffer;
            hook.instance = instance;
            hook.queue = new CountingQueue();
            hook.baseBytes = buffer.size();
            @SuppressWarnings("unchecked")
            final ConcurrentLinkedQueue<byte[]> old = (ConcurrentLinkedQueue<byte[]>) chunks.get(buffer);
            byte[] chunk;
            while ((chunk = old.poll()) != null) {
                hook.queue.addPlain(chunk);
                hook.baseChunks++;
            }
            chunks.set(buffer, hook.queue);
            hooks.add(hook);
            log("hooked stream #" + hooks.size() + ", " + hook.baseBytes + " bytes had already arrived, "
                    + hook.baseChunks + " chunks still queued");
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Hook lastHook() {
        return hooks.getLast();
    }

    /** Bytes that reached the client's buffers, as counted by the buffers themselves. */
    private long totalAdded() {
        long sum = 0;
        for (final Hook h : hooks) sum += h.buffer.size();
        return sum;
    }

    /** Messages that reached the client (one chunk each); the first ones of a new stream may predate the hook. */
    private int totalChunks() {
        int sum = 0;
        for (final Hook h : hooks) {
            sum += h.queue.addedChunks.get() + (int) ((h.baseBytes + 4095) / 4096);
        }
        return sum;
    }

    /** Bytes the engine pulled out of the buffers so far: everything polled minus the unread rest of a chunk. */
    private long totalRead() {
        long sum = 0;
        for (final Hook h : hooks) sum += read(h);
        return sum;
    }

    private static long read(final Hook h) {
        try {
            final Field current = PcmSoundBuffer.class.getDeclaredField("currentChunk");
            final Field offset = PcmSoundBuffer.class.getDeclaredField("currentChunkOffset");
            current.setAccessible(true);
            offset.setAccessible(true);
            final byte[] chunk = (byte[]) current.get(h.buffer);
            long pending = chunk == null ? 0 : Math.max(0, chunk.length - offset.getInt(h.buffer));
            for (final byte[] queued : h.queue) pending += queued.length;
            return Math.max(0, h.buffer.size() - pending);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private long sample(final String label) {
        final long read = totalRead();
        log(label + ": written=" + totalAdded() + " read=" + read + " chunks=" + totalChunks());
        return read;
    }

    private long[] sampleTotals(final String label) {
        return new long[]{sample(label), totalRead()};
    }

    private static void log(final String message) {
        System.out.println("SPEAKERTEST " + message);
    }
}
