package li.cil.oc2.common.vm.video;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import li.cil.oc2.common.blockentity.monitor.video.MonitorVideoController;
import li.cil.oc2.common.blockentity.projector.misc.ProjectorFrameSender;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import li.cil.oc2.common.bus.device.vm.block.misc.ProjectorDevice;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

class LateFramebufferWatcherTest {
    @Test
    @SuppressFBWarnings(
            value = "NP_NONNULL_PARAM_VIOLATION",
            justification =
                    "the monitor device is a stand-in without a block entity; the test replaces"
                        + " its framebuffer through reflection and never mounts it")
    void lateMonitorWatcherReceivesAnIndependentStaticBaseline() throws Exception {
        verifyLateWatcher(new MonitorVideoController(null), new MonitorDevice(null, ignored -> {}));
    }

    @Test
    void lateProjectorWatcherReceivesAnIndependentStaticBaseline() throws Exception {
        verifyLateWatcher(new ProjectorFrameSender(null), new ProjectorDevice(null, ignored -> {}));
    }

    private static void verifyLateWatcher(final Object sender, final Object device) throws Exception {
        final int oldCodec = Config.videoCodec;
        try {
            Config.videoCodec = VideoCodec.DELTA.id;
            final byte[] pixels = new byte[640 * 480 * 2];
            Arrays.fill(pixels, (byte) 0x33);
            final SimpleFramebufferDevice framebuffer =
                    new SimpleFramebufferDevice(640, 480, ByteBuffer.wrap(pixels));
            field(device, "device").set(device, framebuffer);
            // Watchers are only map keys here; no network flush or player method runs.
            // A null stand-in avoids bootstrapping NeoForge's ServerPlayer hierarchy.
            final ServerPlayer originalWatcher = null;
            watch(sender, originalWatcher);
            send(sender, device);
            final AsyncVideoEncoder encoder = (AsyncVideoEncoder) field(sender, "encoder").get(sender);
            final FrameCodec originalDecoder = new FrameCodec();
            final var first = awaitFrame(encoder);
            assertArrayEquals(pixels, decode(originalDecoder, first));
            encoder.recycle(first.buffer());

            // A normal dirty frame now uses the existing encoder's delta history.
            framebuffer.store(0, 0x3333, 1);
            field(sender, "lastSentAt").setLong(sender, 0);
            send(sender, device);
            final var delta = awaitFrame(encoder);
            assertArrayEquals(pixels, decode(originalDecoder, delta));
            final FrameCodec lateDecoder = new FrameCodec();
            assertTrue(lateDecoder.decode(delta.frame().codec(), delta.frame().data(), 640, 480).isEmpty(),
                    "a new client cannot decode the old stream's delta without its baseline");
            encoder.recycle(delta.buffer());
            assertFalse(framebuffer.hasChanges(), "the static frame's dirty state was consumed");

            ((Map<?, ?>) field(sender, "watchers").get(sender)).clear();
            final ServerPlayer lateWatcher = null;
            watch(sender, lateWatcher);
            assertTrue(pending(sender), "a new subscriber must bypass the clean-frame tick gate");
            field(sender, "lastSentAt").setLong(sender, 0);
            send(sender, device);
            final var baseline = awaitFrame(encoder);
            assertArrayEquals(pixels, decode(lateDecoder, baseline));
            assertArrayEquals(pixels, decode(originalDecoder, baseline),
                    "the fresh baseline is broadcast and must also reset existing decoders");
            encoder.recycle(baseline.buffer());
            assertFalse(pending(sender));
            assertFalse(framebuffer.hasChanges());

            watch(sender, lateWatcher);
            assertFalse(pending(sender), "ordinary keep-alives must not restart the stream");
        } finally {
            Config.videoCodec = oldCodec;
        }
    }

    private static byte[] decode(
            final FrameCodec decoder, final AsyncVideoEncoder.CompletedFrame frame) {
        return decoder.decode(frame.frame().codec(), frame.frame().data(), frame.width(), frame.height())
                .orElseThrow();
    }

    private static AsyncVideoEncoder.CompletedFrame awaitFrame(final AsyncVideoEncoder encoder)
            throws InterruptedException {
        final long deadline = System.nanoTime() + 5_000_000_000L;
        AsyncVideoEncoder.CompletedFrame result = encoder.poll();
        while (result == null && System.nanoTime() < deadline) {
            Thread.sleep(5);
            result = encoder.poll();
        }
        assertNotNull(result, "the encoder must publish the requested static frame");
        return result;
    }

    private static void watch(final Object sender, final ServerPlayer player) {
        if (sender instanceof MonitorVideoController monitor) monitor.handleWatchedBy(player);
        else ((ProjectorFrameSender) sender).handleWatchedBy(player);
    }

    private static void send(final Object sender, final Object device) {
        if (sender instanceof MonitorVideoController monitor) monitor.sendFrame((MonitorDevice) device);
        else ((ProjectorFrameSender) sender).sendFrame((ProjectorDevice) device);
    }

    private static boolean pending(final Object sender) {
        return sender instanceof MonitorVideoController monitor
                ? monitor.hasPendingFullFrame() : ((ProjectorFrameSender) sender).hasPendingFullFrame();
    }

    private static Field field(final Object owner, final String name) throws NoSuchFieldException {
        final Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true); // NOPMD - Observe private state without exposing a production-only testing API.
        return field;
    }
}
