package li.cil.oc2.common.vm.device.frame;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import org.junit.jupiter.api.Test;

class FramePresentDeviceTest {
    @Test
    void partialFrameAndForcedLateWatcherBaselineStayOnCompletedImage() throws Exception {
        final SimpleFramebufferDevice fb = framebuffer();
        final FramePresentDevice control = new FramePresentDevice(fb);
        fb.store(0, 0x1111, 1);
        assertEquals(0x1111, copy(fb).getShort(0));
        command(control, FramePresentDevice.BEGIN);
        fb.store(0, 0x2222, 1);
        assertFalse(fb.hasChanges());
        assertEquals(0x1111, copy(fb).getShort(0));
        assertEquals(0, copy(fb).getShort(2));
        fb.store(2, 0x3333, 1);
        command(control, FramePresentDevice.COMMIT);
        assertTrue(fb.hasChanges());
        final ByteBuffer complete = copy(fb);
        assertEquals(0x2222, complete.getShort(0));
        assertEquals(0x3333, complete.getShort(2));
    }

    @Test
    void legacyNormalCopyAndCloseKeepExistingBehavior() throws Exception {
        final SimpleFramebufferDevice fb = framebuffer();
        final ByteBuffer output = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        assertTrue(fb.copyFrame(output));
        assertFalse(fb.copyFrame(output));
        fb.store(0, 1234, 1);
        assertTrue(fb.copyFrame(output));
        assertEquals(1234, output.getShort(0));
        fb.close();
        fb.close();
        assertFalse(fb.copyFrame(output, true));
        fb.store(0, 5678, 1);
        assertEquals(0, fb.load(0, 1));
    }

    @Test
    void firstBeginProvidesBaselineEvenBeforeAnyInitialCopyOrCommit() throws Exception {
        final SimpleFramebufferDevice fb = framebuffer();
        fb.store(0, 1234, 1);
        fb.present(FramePresentDevice.BEGIN);
        fb.store(0, 5678, 1);
        assertEquals(1234, copy(fb).getShort(0));
    }

    @Test
    void abortRestoresDrawAndCannotLeakIntoNextFrameOrLegacyMode() throws Exception {
        final SimpleFramebufferDevice fb = framebuffer();
        fb.store(0, 1234, 1);
        fb.present(FramePresentDevice.BEGIN);
        fb.store(0, 5678, 1);
        fb.present(FramePresentDevice.ABORT);
        assertEquals(2, fb.presentationStatus());
        assertEquals(1234, fb.load(0, 1));
        fb.present(FramePresentDevice.BEGIN);
        fb.store(2, 7890, 1);
        fb.present(FramePresentDevice.COMMIT);
        assertEquals(1234, copy(fb).getShort(0));
        fb.present(FramePresentDevice.DISABLE);
        fb.store(0, 2345, 1);
        assertEquals(2345, copy(fb).getShort(0));
    }

    @Test
    void resetExitsTransactionRestoresBaselineAndAllowsLegacyWrites() throws Exception {
        final SimpleFramebufferDevice fb = framebuffer();
        final FramePresentDevice control = new FramePresentDevice(fb);
        fb.store(0, 1000, 1);
        command(control, FramePresentDevice.BEGIN);
        fb.store(0, 2000, 1);
        control.reset();
        assertEquals(0, fb.presentationStatus());
        assertEquals(1000, copy(fb).getShort(0));
        fb.store(0, 3000, 1);
        assertEquals(3000, copy(fb).getShort(0));
    }

    @Test
    void committedSnapshotSurvivesPersistenceWithAnUnfinishedDraw() throws Exception {
        final SimpleFramebufferDevice fb = framebuffer();
        fb.store(0, 1234, 1);
        fb.present(FramePresentDevice.BEGIN);
        fb.store(0, 5678, 1);
        final byte[] saved = fb.savePresentation();
        final SimpleFramebufferDevice restored = framebuffer();
        restored.store(0, 9999, 1);
        restored.restorePresentation(saved, fb.presentationStatus());
        saved[0] = 0;
        assertEquals(1234, copy(restored).getShort(0));
        restored.present(FramePresentDevice.ABORT);
        assertEquals(1234, restored.load(0, 1));
    }

    @Test
    void warmResumePreservesPartialDrawAndFinishesTheSameGuestFrame() throws Exception {
        final ByteBuffer drawBlob = ByteBuffer.allocate(4);
        final SimpleFramebufferDevice before = new SimpleFramebufferDevice(2, 1, drawBlob);
        before.store(0, 1111, 1);
        before.store(2, 1111, 1);
        before.present(FramePresentDevice.BEGIN);
        before.store(0, 2222, 1);
        final FramePublication.Snapshot saved = before.snapshotPresentation();
        before.close();
        final SimpleFramebufferDevice after = new SimpleFramebufferDevice(2, 1, drawBlob);
        after.restorePresentation(saved.image(), saved.status());
        assertEquals(1111, copy(after).getShort(0));
        after.store(2, 2222, 1);
        after.present(FramePresentDevice.COMMIT);
        final ByteBuffer complete = copy(after);
        assertEquals(2222, complete.getShort(0));
        assertEquals(2222, complete.getShort(2));
    }

    @Test
    void protocolRejectsNon32BitCommandsAndReportsVersion() {
        final SimpleFramebufferDevice fb = framebuffer();
        final FramePresentDevice control = new FramePresentDevice(fb);
        assertEquals(32, control.getLength());
        assertEquals(FramePresentDevice.MAGIC, control.load(0, 2));
        assertEquals(1, control.load(4, 2));
        assertEquals(1, control.load(16, 2));
        control.store(8, 1, 0);
        control.store(9, 1, 2);
        assertEquals(0, control.load(12, 2));
        command(control, FramePresentDevice.BEGIN);
        assertEquals(3, control.load(12, 2));
        command(control, FramePresentDevice.COMMIT);
        assertEquals(2, control.load(12, 2));
    }

    @Test
    void concurrentStoresAndCopiesNeverExposePartialTransaction() throws Exception {
        final SimpleFramebufferDevice fb = framebuffer();
        final AtomicBoolean running = new AtomicBoolean(true);
        final java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        final Thread writer = new Thread(() -> {
            try {
                for (int frame = 1; frame <= 2000; frame++) {
                    fb.present(FramePresentDevice.BEGIN);
                    fb.store(0, frame, 1);
                    Thread.yield();
                    fb.store(2, frame, 1);
                    fb.present(FramePresentDevice.COMMIT);
                }
            } catch (Throwable error) { failure.set(error); }
            finally { running.set(false); }
        });
        writer.start();
        try {
            do {
                final ByteBuffer visible = copy(fb);
                assertEquals(visible.getShort(0), visible.getShort(2));
            } while (running.get());
        } finally { writer.join(10000); }
        assertFalse(writer.isAlive());
        assertNull(failure.get());
    }

    @Test
    void invalidDimensionsAndOversizedSnapshotsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SimpleFramebufferDevice.checkedLength(0, 10));
        assertThrows(IllegalArgumentException.class, () -> SimpleFramebufferDevice.checkedLength(8192, 8192));
        final SimpleFramebufferDevice fb = framebuffer();
        fb.restorePresentation(new byte[3], 3);
        assertEquals(0, fb.presentationStatus());
    }

    private static SimpleFramebufferDevice framebuffer() {
        return new SimpleFramebufferDevice(2, 1, ByteBuffer.allocate(4));
    }

    private static void command(final FramePresentDevice device, final int command) {
        device.store(8, command, 2);
    }

    private static ByteBuffer copy(final SimpleFramebufferDevice fb) {
        final ByteBuffer out = ByteBuffer.allocate(fb.getLength()).order(ByteOrder.LITTLE_ENDIAN);
        assertTrue(fb.copyFrame(out, true));
        return out;
    }
}
